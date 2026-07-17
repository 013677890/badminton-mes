package com.badminton.mes.module.quality.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.production.controller.vo.WorkOrderRespVO;
import com.badminton.mes.module.production.service.WorkOrderService;
import com.badminton.mes.module.quality.constants.QualityErrorCodeConstants;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionRecordCreateReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionRecordPageReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionRecordRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionRecordSubmitReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionResultSaveReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionResultsSaveReqVO;
import com.badminton.mes.module.quality.convert.QualityInspectionRecordConvert;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionItemEntity;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionPlanEntity;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionPlanItemEntity;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionRecordEntity;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionResultEntity;
import com.badminton.mes.module.quality.dal.redis.QualityCache;
import com.badminton.mes.module.quality.dal.redis.QualityRedisKeyConstants;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionItemRepository;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionPlanItemRepository;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionPlanRepository;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionRecordRepository;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionRecordSpecifications;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionResultRepository;
import com.badminton.mes.module.quality.service.QualityInspectionRecordService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 统一质量检验单应用服务实现。
 *
 * <p>检验单创建时校验生效方案、来源单据和产品/客户适用范围，并固化方案编号、版本及全部方案项规则。
 * 生产过程类检验限定从工单取得产品和客户；其他检验要求显式来源单据，防止检验记录脱离业务来源。</p>
 *
 * <p>草稿结果允许分批保存，提交时执行最终完整性与结论一致性检查。提交后 PASS、CONCESSION 放行为
 * RELEASED，其他结论转为 BLOCKED。更新和提交均悲观锁定检验单，且只在事务提交后失效详情缓存。</p>
 */
@Service
public class QualityInspectionRecordServiceImpl implements QualityInspectionRecordService {

    /** 方案、检验单、判定结论和放行状态约定。 */
    private static final String PLAN_STATUS_EFFECTIVE = "EFFECTIVE";
    private static final String RECORD_STATUS_DRAFT = "DRAFT";
    private static final String RECORD_STATUS_SUBMITTED = "SUBMITTED";
    private static final String CONCLUSION_PASS = "PASS";
    private static final String CONCLUSION_CONCESSION = "CONCESSION";
    private static final String JUDGMENT_FAIL = "FAIL";
    private static final String RELEASE_PENDING = "PENDING";
    private static final String RELEASED = "RELEASED";
    private static final String BLOCKED = "BLOCKED";
    private static final Set<String> PRODUCTION_INSPECTION_TYPES =
            Set.of("FIRST_ARTICLE", "LAST_ARTICLE", "PATROL");
    private static final Long DEFAULT_OPERATOR_ID = 1L;
    private static final DateTimeFormatter INSPECTION_NO_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final QualityInspectionRecordRepository recordRepository;
    private final QualityInspectionResultRepository resultRepository;
    private final QualityInspectionPlanRepository planRepository;
    private final QualityInspectionPlanItemRepository planItemRepository;
    private final QualityInspectionItemRepository inspectionItemRepository;
    private final WorkOrderService workOrderService;
    private final QualityCache qualityCache;

    /** 注入检验单、结果快照、方案模板、项目主数据、工单来源及详情缓存依赖。 */
    public QualityInspectionRecordServiceImpl(QualityInspectionRecordRepository recordRepository,
                                              QualityInspectionResultRepository resultRepository,
                                              QualityInspectionPlanRepository planRepository,
                                              QualityInspectionPlanItemRepository planItemRepository,
                                              QualityInspectionItemRepository inspectionItemRepository,
                                              WorkOrderService workOrderService,
                                              QualityCache qualityCache) {
        this.recordRepository = recordRepository;
        this.resultRepository = resultRepository;
        this.planRepository = planRepository;
        this.planItemRepository = planItemRepository;
        this.inspectionItemRepository = inspectionItemRepository;
        this.workOrderService = workOrderService;
        this.qualityCache = qualityCache;
    }

    /**
     * 创建草稿检验单时先验证方案可用性和非空方案项，再解析来源与适用范围；主表保存成功后，
     * 按方案项生成结果快照，任一步失败都由事务整体回滚。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRecord(String inspectionType, QualityInspectionRecordCreateReqVO request) {
        QualityInspectionPlanEntity plan = planRepository.findByIdAndDeletedFalse(request.getPlanId())
                .orElseThrow(() -> new ServiceException(QualityErrorCodeConstants.PLAN_NOT_EXISTS));
        validatePlanAvailable(plan, inspectionType);
        List<QualityInspectionPlanItemEntity> planItems =
                planItemRepository.findByPlanIdOrderBySortOrderAscIdAsc(plan.getId());
        if (planItems.isEmpty()) {
            throw new ServiceException(QualityErrorCodeConstants.RECORD_PLAN_UNAVAILABLE);
        }

        QualityInspectionRecordEntity record = buildRecord(inspectionType, request, plan);
        enrichAndValidateSource(record, request);
        validatePlanScope(plan, record);
        saveRecord(record);
        saveResultSnapshots(record.getId(), planItems);
        return record.getId();
    }

    /**
     * 草稿阶段允许分批保存，但请求内结果主键必须唯一且属于当前检验单。悲观锁防止保存结果与提交操作
     * 并发交错；这里只校验已填写字段的局部一致性，必检项完整性留到提交阶段检查。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveResults(Long id, QualityInspectionResultsSaveReqVO request) {
        QualityInspectionRecordEntity record = getRecordForUpdate(id);
        validateDraftRecord(record);
        Set<Long> resultIds = request.getResults().stream()
                .map(QualityInspectionResultSaveReqVO::getResultId)
                .collect(Collectors.toSet());
        if (resultIds.size() != request.getResults().size()) {
            throw new ServiceException(QualityErrorCodeConstants.RECORD_RESULTS_INCOMPLETE);
        }

        List<QualityInspectionResultEntity> storedResults =
                resultRepository.findByInspectionRecordIdOrderBySortOrderAscIdAsc(id);
        Map<Long, QualityInspectionResultEntity> storedResultsById = storedResults.stream()
                .collect(Collectors.toMap(QualityInspectionResultEntity::getId, Function.identity()));
        for (QualityInspectionResultSaveReqVO resultRequest : request.getResults()) {
            QualityInspectionResultEntity result = storedResultsById.get(resultRequest.getResultId());
            if (result == null) {
                throw new ServiceException(QualityErrorCodeConstants.RECORD_RESULTS_INCOMPLETE);
            }
            validateResultInput(resultRequest);
            result.setMeasuredValue(resultRequest.getMeasuredValue());
            result.setJudgmentResult(resultRequest.getJudgmentResult());
            result.setDefectDescription(resultRequest.getDefectDescription());
        }
        resultRepository.saveAll(storedResultsById.values());
        evictRecordCacheAfterCommit(id);
    }

    /**
     * 提交是 DRAFT 到 SUBMITTED 的不可逆迁移。先验证全部结果，再约束逐项判定与单据结论一致：
     * 有 FAIL 不得 PASS，无 FAIL 只能 PASS；不合格还必须有原因和处置意见。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitRecord(Long id, QualityInspectionRecordSubmitReqVO request) {
        QualityInspectionRecordEntity record = getRecordForUpdate(id);
        validateDraftRecord(record);
        List<QualityInspectionResultEntity> results =
                resultRepository.findByInspectionRecordIdOrderBySortOrderAscIdAsc(id);
        validateResultsComplete(results);

        boolean hasFailedResult = results.stream()
                .anyMatch(result -> JUDGMENT_FAIL.equals(result.getJudgmentResult()));
        if (hasFailedResult && CONCLUSION_PASS.equals(request.getConclusion())) {
            throw new ServiceException(QualityErrorCodeConstants.RECORD_CONCLUSION_INVALID);
        }
        if (!hasFailedResult && !CONCLUSION_PASS.equals(request.getConclusion())) {
            throw new ServiceException(QualityErrorCodeConstants.RECORD_CONCLUSION_INVALID);
        }
        if (hasFailedResult && !StringUtils.hasText(request.getNonconformanceDescription())) {
            throw new ServiceException(QualityErrorCodeConstants.RECORD_RESULTS_INCOMPLETE);
        }
        if (!CONCLUSION_PASS.equals(request.getConclusion())
                && !StringUtils.hasText(request.getDisposition())) {
            throw new ServiceException(QualityErrorCodeConstants.RECORD_RESULTS_INCOMPLETE);
        }

        record.setRecordStatus(RECORD_STATUS_SUBMITTED);
        record.setConclusion(request.getConclusion());
        // 合格和让步接收均可放行，其余结论必须阻断后续流转。
        record.setReleaseStatus(resolveReleaseStatus(request.getConclusion()));
        applyDefectResult(record, request, hasFailedResult);
        record.setNonconformanceDescription(request.getNonconformanceDescription());
        record.setDisposition(request.getDisposition());
        record.setInspectorId(getCurrentOperatorId());
        record.setInspectedAt(LocalDateTime.now());
        saveRecord(record);
        evictRecordCacheAfterCommit(id);
    }

    /** 详情始终读取检验发生时落库的结果快照，不回查当前方案或项目规则。 */
    @Override
    @Transactional(readOnly = true)
    public QualityInspectionRecordRespVO getRecord(Long id) {
        return qualityCache.getOrLoadDetail(QualityRedisKeyConstants.INSPECTION_RECORD_RESOURCE,
                id, QualityInspectionRecordRespVO.class, () -> {
            QualityInspectionRecordEntity record = getRecordEntity(id);
            List<QualityInspectionResultEntity> results =
                    resultRepository.findByInspectionRecordIdOrderBySortOrderAscIdAsc(id);
            QualityInspectionRecordRespVO response = QualityInspectionRecordConvert.toRespVO(record, results);
            return response;
        });
    }

    /** 分页仅返回检验单摘要，不加载结果快照；越界页码收敛到末页。 */
    @Override
    @Transactional(readOnly = true)
    public PageResult<QualityInspectionRecordRespVO> getRecordPage(QualityInspectionRecordPageReqVO request) {
        var specification = QualityInspectionRecordSpecifications.page(request);
        long total = recordRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(request.getPageNo(), request.getPageSize());
        }
        int pageSize = request.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        int pageNo = Math.min(request.getPageNo(), totalPages);
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize,
                Sort.by(Sort.Direction.DESC, "createTime").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<QualityInspectionRecordEntity> page = recordRepository.findAll(specification, pageRequest);
        List<QualityInspectionRecordRespVO> list = page.getContent().stream()
                .map(QualityInspectionRecordConvert::toSummaryRespVO)
                .toList();
        return PageResult.of(list, total, pageNo, pageSize);
    }

    /**
     * 初始化检验单主表快照。方案编码和版本在创建时固化，状态固定为 DRAFT/PENDING，
     * 来源字段先按请求暂存，随后由来源校验按权威单据覆盖或确认。
     */
    private QualityInspectionRecordEntity buildRecord(
            String inspectionType,
            QualityInspectionRecordCreateReqVO request,
            QualityInspectionPlanEntity plan) {
        QualityInspectionRecordEntity record = new QualityInspectionRecordEntity();
        record.setInspectionNo(generateInspectionNo());
        record.setInspectionType(inspectionType);
        record.setPlanId(plan.getId());
        record.setPlanCodeSnapshot(plan.getPlanCode());
        record.setPlanVersionSnapshot(plan.getVersionNo());
        record.setWorkOrderId(request.getWorkOrderId());
        record.setProductionTaskId(request.getProductionTaskId());
        record.setSourceDocumentId(request.getSourceDocumentId());
        record.setSourceDocumentNo(request.getSourceDocumentNo());
        record.setProductId(request.getProductId());
        record.setCustomerId(request.getCustomerId());
        record.setProductionLineId(request.getProductionLineId());
        record.setProcessId(request.getProcessId());
        record.setBatchNo(request.getBatchNo());
        record.setSampleQuantity(request.getSampleQuantity());
        record.setRecordStatus(RECORD_STATUS_DRAFT);
        record.setReleaseStatus(RELEASE_PENDING);
        record.setDefectQuantity(0);
        record.setCreateBy(getCurrentOperatorId());
        record.setDeleted(false);
        return record;
    }

    /**
     * 校验检验单来源范围。
     *
     * <p>FIRST_ARTICLE、LAST_ARTICLE、PATROL 属于生产过程检验，必须关联工单，并以工单产品、客户为
     * 权威值；请求若同时提供这些字段只能与工单一致。其他检验必须提供来源单据主键、单号和产品。</p>
     */
    private void enrichAndValidateSource(QualityInspectionRecordEntity record,
                                         QualityInspectionRecordCreateReqVO request) {
        if (PRODUCTION_INSPECTION_TYPES.contains(record.getInspectionType())) {
            if (request.getWorkOrderId() == null) {
                throw new ServiceException(QualityErrorCodeConstants.RECORD_SOURCE_INVALID);
            }
            WorkOrderRespVO workOrder = workOrderService.getWorkOrder(request.getWorkOrderId());
            validateOptionalSourceField(request.getProductId(), workOrder.getProductId());
            validateOptionalSourceField(request.getCustomerId(), workOrder.getCustomerId());
            if (StringUtils.hasText(workOrder.getBatchNo())
                    && !workOrder.getBatchNo().equals(request.getBatchNo())) {
                throw new ServiceException(QualityErrorCodeConstants.RECORD_SOURCE_INVALID);
            }
            record.setProductId(workOrder.getProductId());
            record.setCustomerId(workOrder.getCustomerId());
            return;
        }
        if (request.getSourceDocumentId() == null || !StringUtils.hasText(request.getSourceDocumentNo())
                || request.getProductId() == null) {
            throw new ServiceException(QualityErrorCodeConstants.RECORD_SOURCE_INVALID);
        }
    }

    /** 请求可省略由来源单据决定的字段，但一旦提供就必须与权威来源一致。 */
    private void validateOptionalSourceField(Long requestedValue, Long sourceValue) {
        if (requestedValue != null && !requestedValue.equals(sourceValue)) {
            throw new ServiceException(QualityErrorCodeConstants.RECORD_SOURCE_INVALID);
        }
    }

    /** 方案必须已审核生效、检验类型一致，并且生效日期不得晚于当前日期。 */
    private void validatePlanAvailable(QualityInspectionPlanEntity plan, String inspectionType) {
        if (!PLAN_STATUS_EFFECTIVE.equals(plan.getPlanStatus())
                || !inspectionType.equals(plan.getInspectionType())
                || plan.getEffectiveDate() == null
                || plan.getEffectiveDate().isAfter(java.time.LocalDate.now())) {
            throw new ServiceException(QualityErrorCodeConstants.RECORD_PLAN_UNAVAILABLE);
        }
    }

    /**
     * 校验方案适用范围：方案未限定的产品或客户视为通配；一旦限定则必须与检验来源一致。
     */
    private void validatePlanScope(QualityInspectionPlanEntity plan,
                                   QualityInspectionRecordEntity record) {
        boolean productMismatch = plan.getProductId() != null
                && !plan.getProductId().equals(record.getProductId());
        boolean customerMismatch = plan.getCustomerId() != null
                && !plan.getCustomerId().equals(record.getCustomerId());
        if (productMismatch || customerMismatch) {
            throw new ServiceException(QualityErrorCodeConstants.RECORD_PLAN_UNAVAILABLE);
        }
    }

    /**
     * 为每个方案项创建一条检验结果快照。先批量加载全部项目并校验引用完整性，避免缺少项目主数据时
     * 生成不可审计的半快照；所有结果与检验单在同一事务内一次刷盘。
     */
    private void saveResultSnapshots(Long recordId, List<QualityInspectionPlanItemEntity> planItems) {
        Set<Long> inspectionItemIds = planItems.stream()
                .map(QualityInspectionPlanItemEntity::getInspectionItemId)
                .collect(Collectors.toCollection(HashSet::new));
        Map<Long, QualityInspectionItemEntity> inspectionItemsById = inspectionItemRepository
                .findAllById(inspectionItemIds).stream()
                .collect(Collectors.toMap(QualityInspectionItemEntity::getId, Function.identity()));
        if (inspectionItemsById.size() != inspectionItemIds.size()) {
            throw new ServiceException(QualityErrorCodeConstants.RECORD_PLAN_UNAVAILABLE);
        }
        List<QualityInspectionResultEntity> results = planItems.stream()
                .map(planItem -> toResultSnapshot(recordId, planItem,
                        inspectionItemsById.get(planItem.getInspectionItemId())))
                .toList();
        resultRepository.saveAllAndFlush(results);
    }

    /**
     * 合并两层来源形成历史快照：项目编码、名称、值类型、单位取项目主数据；必检、标准值、上下限、
     * 判定方式和顺序取方案项版本快照，保证后续主数据变化不改变本次检验依据。
     */
    private QualityInspectionResultEntity toResultSnapshot(
            Long recordId,
            QualityInspectionPlanItemEntity planItem,
            QualityInspectionItemEntity inspectionItem) {
        QualityInspectionResultEntity result = new QualityInspectionResultEntity();
        result.setInspectionRecordId(recordId);
        result.setInspectionItemId(inspectionItem.getId());
        result.setItemCodeSnapshot(inspectionItem.getItemCode());
        result.setItemNameSnapshot(inspectionItem.getItemName());
        result.setValueTypeSnapshot(inspectionItem.getValueType());
        result.setUnitSnapshot(inspectionItem.getUnit());
        result.setRequiredFlag(planItem.getRequiredFlag());
        result.setStandardValueSnapshot(planItem.getStandardValue());
        result.setLowerLimitSnapshot(planItem.getLowerLimit());
        result.setUpperLimitSnapshot(planItem.getUpperLimit());
        result.setJudgmentMethodSnapshot(planItem.getJudgmentMethod());
        result.setSortOrder(planItem.getSortOrder());
        return result;
    }

    /** 局部保存时只约束成对字段：有判定必须有实测值，FAIL 必须说明缺陷。 */
    private void validateResultInput(QualityInspectionResultSaveReqVO request) {
        if (StringUtils.hasText(request.getJudgmentResult())
                && !StringUtils.hasText(request.getMeasuredValue())) {
            throw new ServiceException(QualityErrorCodeConstants.RECORD_RESULTS_INCOMPLETE);
        }
        if (JUDGMENT_FAIL.equals(request.getJudgmentResult())
                && !StringUtils.hasText(request.getDefectDescription())) {
            throw new ServiceException(QualityErrorCodeConstants.RECORD_RESULTS_INCOMPLETE);
        }
    }

    /**
     * 提交前校验最终完整性：结果集合不能为空；任何已判定项必须有实测值；必检项必须同时填写实测值
     * 和判定；FAIL 项必须填写缺陷描述。非必检且完全未填写的项目允许保留为空。
     */
    private void validateResultsComplete(List<QualityInspectionResultEntity> results) {
        if (results.isEmpty()) {
            throw new ServiceException(QualityErrorCodeConstants.RECORD_RESULTS_INCOMPLETE);
        }
        for (QualityInspectionResultEntity result : results) {
            if (StringUtils.hasText(result.getJudgmentResult())
                    && !StringUtils.hasText(result.getMeasuredValue())) {
                throw new ServiceException(QualityErrorCodeConstants.RECORD_RESULTS_INCOMPLETE);
            }
            if (Boolean.TRUE.equals(result.getRequiredFlag())
                    && (!StringUtils.hasText(result.getMeasuredValue())
                    || !StringUtils.hasText(result.getJudgmentResult()))) {
                throw new ServiceException(QualityErrorCodeConstants.RECORD_RESULTS_INCOMPLETE);
            }
            if (JUDGMENT_FAIL.equals(result.getJudgmentResult())
                    && !StringUtils.hasText(result.getDefectDescription())) {
                throw new ServiceException(QualityErrorCodeConstants.RECORD_RESULTS_INCOMPLETE);
            }
        }
    }

    /** PASS 与 CONCESSION 映射为 RELEASED，其他结论统一映射为 BLOCKED。 */
    private String resolveReleaseStatus(String conclusion) {
        return CONCLUSION_PASS.equals(conclusion) || CONCLUSION_CONCESSION.equals(conclusion)
                ? RELEASED : BLOCKED;
    }

    /**
     * 根据项目判定结果校验并固化不良数量和归并号。
     *
     * <p>无失败项目时不允许携带不良数量；存在失败项目时，不良数量必须介于 1 和抽样数量之间。
     * 归并号在落库前去除首尾空白，空白字符串按未提供处理。</p>
     */
    private void applyDefectResult(QualityInspectionRecordEntity record,
                                   QualityInspectionRecordSubmitReqVO request,
                                   boolean hasFailedResult) {
        int defectQuantity = request.getDefectQuantity() == null ? 0 : request.getDefectQuantity();
        if (!hasFailedResult && defectQuantity != 0) {
            throw new ServiceException(QualityErrorCodeConstants.RECORD_CONCLUSION_INVALID);
        }
        if (hasFailedResult && (defectQuantity <= 0 || defectQuantity > record.getSampleQuantity())) {
            throw new ServiceException(QualityErrorCodeConstants.RECORD_RESULTS_INCOMPLETE);
        }
        record.setDefectQuantity(defectQuantity);
        record.setDefectGroupNo(StringUtils.hasText(request.getDefectGroupNo())
                ? request.getDefectGroupNo().trim() : null);
    }

    /** 以毫秒时间和随机后缀生成检验单号，数据库唯一约束负责最终并发兜底。 */
    private String generateInspectionNo() {
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "QI" + LocalDateTime.now().format(INSPECTION_NO_TIME_FORMAT) + randomSuffix;
    }

    /** 统一守卫草稿专属写操作，提交后的检验单不可再次录入或重复提交。 */
    private void validateDraftRecord(QualityInspectionRecordEntity record) {
        if (!RECORD_STATUS_DRAFT.equals(record.getRecordStatus())) {
            throw new ServiceException(QualityErrorCodeConstants.RECORD_EDIT_NOT_ALLOWED);
        }
    }

    /** 读取未删除检验单，不获取写锁。 */
    private QualityInspectionRecordEntity getRecordEntity(Long id) {
        return recordRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(QualityErrorCodeConstants.RECORD_NOT_EXISTS));
    }

    /** 悲观锁定检验单，使草稿状态检查、结果保存和提交迁移互斥。 */
    private QualityInspectionRecordEntity getRecordForUpdate(Long id) {
        return recordRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(QualityErrorCodeConstants.RECORD_NOT_EXISTS));
    }

    /** 立即刷盘并将极低概率的检验单号唯一键冲突转换为业务异常。 */
    private void saveRecord(QualityInspectionRecordEntity record) {
        try {
            recordRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException exception) {
            throw new ServiceException(QualityErrorCodeConstants.RECORD_NO_DUPLICATE);
        }
    }

    /** 仅在事务成功提交后失效检验单详情，避免回滚数据被重新缓存。 */
    private void evictRecordCacheAfterCommit(Long id) {
        qualityCache.evictDetailAfterCommit(QualityRedisKeyConstants.INSPECTION_RECORD_RESOURCE, id);
    }

    /** 无登录上下文的系统调用回退到默认操作人。 */
    private Long getCurrentOperatorId() {
        if (SecurityContextHolder.getLoginUser() == null) {
            return DEFAULT_OPERATOR_ID;
        }
        return SecurityContextHolder.getRequiredLoginUserId();
    }
}
