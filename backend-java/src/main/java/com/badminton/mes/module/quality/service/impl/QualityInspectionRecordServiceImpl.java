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
 * 统一质量检验单 Service 实现。
 *
 * <p>负责“创建草稿 → 保存结果 → 提交判定”的完整流程。创建时把方案项目复制成
 * 检验结果快照，避免方案后续变更影响历史检验事实；写方法使用事务保证检验单和结果一致。
 *
 * @author MES 开发组
 * @date 2026/07/16
 */
@Service
public class QualityInspectionRecordServiceImpl implements QualityInspectionRecordService {

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

    /** 创建检验单并固化当时的方案项目快照。 */
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

    /** 保存草稿结果；只更新属于当前检验单的结果行。 */
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

    /** 校验必检项和结论完整性后提交，并计算放行/阻断状态。 */
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
        record.setReleaseStatus(resolveReleaseStatus(request.getConclusion()));
        applyDefectResult(record, request, hasFailedResult);
        record.setNonconformanceDescription(request.getNonconformanceDescription());
        record.setDisposition(request.getDisposition());
        record.setInspectorId(getCurrentOperatorId());
        record.setInspectedAt(LocalDateTime.now());
        saveRecord(record);
        evictRecordCacheAfterCommit(id);
    }

    /** 从缓存读取检验单详情，未命中时聚合主表和结果明细。 */
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

    /** 分页查询检验单，供质量列表页和报表筛选使用。 */
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
     * 根据工单等可信来源补齐检验单范围字段，并拒绝前端提交值与来源档案冲突的请求。
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

    private void validateOptionalSourceField(Long requestedValue, Long sourceValue) {
        if (requestedValue != null && !requestedValue.equals(sourceValue)) {
            throw new ServiceException(QualityErrorCodeConstants.RECORD_SOURCE_INVALID);
        }
    }

    private void validatePlanAvailable(QualityInspectionPlanEntity plan, String inspectionType) {
        if (!PLAN_STATUS_EFFECTIVE.equals(plan.getPlanStatus())
                || !inspectionType.equals(plan.getInspectionType())
                || plan.getEffectiveDate() == null
                || plan.getEffectiveDate().isAfter(java.time.LocalDate.now())) {
            throw new ServiceException(QualityErrorCodeConstants.RECORD_PLAN_UNAVAILABLE);
        }
    }

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

    /** 提交前确认所有必检项目均已填写并完成判定，防止生成不完整质量结论。 */
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

    private String resolveReleaseStatus(String conclusion) {
        return CONCLUSION_PASS.equals(conclusion) || CONCLUSION_CONCESSION.equals(conclusion)
                ? RELEASED : BLOCKED;
    }

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

    private String generateInspectionNo() {
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "QI" + LocalDateTime.now().format(INSPECTION_NO_TIME_FORMAT) + randomSuffix;
    }

    private void validateDraftRecord(QualityInspectionRecordEntity record) {
        if (!RECORD_STATUS_DRAFT.equals(record.getRecordStatus())) {
            throw new ServiceException(QualityErrorCodeConstants.RECORD_EDIT_NOT_ALLOWED);
        }
    }

    private QualityInspectionRecordEntity getRecordEntity(Long id) {
        return recordRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(QualityErrorCodeConstants.RECORD_NOT_EXISTS));
    }

    private QualityInspectionRecordEntity getRecordForUpdate(Long id) {
        return recordRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(QualityErrorCodeConstants.RECORD_NOT_EXISTS));
    }

    private void saveRecord(QualityInspectionRecordEntity record) {
        try {
            recordRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException exception) {
            throw new ServiceException(QualityErrorCodeConstants.RECORD_NO_DUPLICATE);
        }
    }

    private void evictRecordCacheAfterCommit(Long id) {
        qualityCache.evictDetailAfterCommit(QualityRedisKeyConstants.INSPECTION_RECORD_RESOURCE, id);
    }

    private Long getCurrentOperatorId() {
        if (SecurityContextHolder.getLoginUser() == null) {
            return DEFAULT_OPERATOR_ID;
        }
        return SecurityContextHolder.getRequiredLoginUserId();
    }
}
