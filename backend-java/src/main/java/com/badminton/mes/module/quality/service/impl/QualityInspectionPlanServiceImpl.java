package com.badminton.mes.module.quality.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.quality.constants.QualityErrorCodeConstants;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionPlanItemSaveReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionPlanPageReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionPlanRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionPlanSaveReqVO;
import com.badminton.mes.module.quality.convert.QualityInspectionPlanConvert;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionItemEntity;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionPlanEntity;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionPlanItemEntity;
import com.badminton.mes.module.quality.dal.redis.QualityCache;
import com.badminton.mes.module.quality.dal.redis.QualityRedisKeyConstants;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionItemRepository;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionPlanItemRepository;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionPlanRepository;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionPlanSpecifications;
import com.badminton.mes.module.quality.service.QualityInspectionPlanService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 检验标准方案应用服务实现。
 *
 * <p>方案状态机为 DRAFT - EFFECTIVE - DISABLED：只有草稿可编辑或删除，审核使草稿生效，生效版本只能
 * 停用或派生新版本。新版本始终回到草稿，不继承生效日期、默认资格和审核信息。</p>
 *
 * <p>方案项把项目规则按“请求覆盖值优先、项目主数据默认值兜底”固化为版本快照。审核默认方案和分配
 * 新版本号都使用悲观锁串行化竞争，防止同一适用范围出现多个默认方案或并发生成重复版本。</p>
 */
@Service
public class QualityInspectionPlanServiceImpl implements QualityInspectionPlanService {

    /** 状态机、规则类型和版本初始化约定。 */
    private static final String PLAN_STATUS_DRAFT = "DRAFT";
    private static final String PLAN_STATUS_EFFECTIVE = "EFFECTIVE";
    private static final String PLAN_STATUS_DISABLED = "DISABLED";
    private static final String VALUE_TYPE_NUMERIC = "NUMERIC";
    private static final String JUDGMENT_METHOD_RANGE = "RANGE";
    private static final String JUDGMENT_METHOD_STANDARD_VALUE = "STANDARD_VALUE";
    private static final int ENABLED = 1;
    private static final int INITIAL_VERSION = 1;
    private static final Long DEFAULT_OPERATOR_ID = 1L;
    private static final String DELETED_CODE_PREFIX = "__DELETED_";

    private final QualityInspectionPlanRepository planRepository;
    private final QualityInspectionPlanItemRepository planItemRepository;
    private final QualityInspectionItemRepository inspectionItemRepository;
    private final QualityCache qualityCache;

    /** 注入方案及方案项存储、项目主数据和质量详情缓存。 */
    public QualityInspectionPlanServiceImpl(QualityInspectionPlanRepository planRepository,
                                            QualityInspectionPlanItemRepository planItemRepository,
                                            QualityInspectionItemRepository inspectionItemRepository,
                                            QualityCache qualityCache) {
        this.planRepository = planRepository;
        this.planItemRepository = planItemRepository;
        this.inspectionItemRepository = inspectionItemRepository;
        this.qualityCache = qualityCache;
    }

    /**
     * 初次创建固定为版本 1 和 DRAFT；先验证全部方案项，再在同一事务中保存主表与规则快照，
     * 避免产生没有有效方案项的半成品方案。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPlan(QualityInspectionPlanSaveReqVO request) {
        if (planRepository.existsByPlanCodeAndVersionNoAndDeletedFalse(
                request.getPlanCode(), INITIAL_VERSION)) {
            throw new ServiceException(QualityErrorCodeConstants.PLAN_CODE_DUPLICATE);
        }
        Map<Long, QualityInspectionItemEntity> inspectionItemsById = validatePlanItems(request.getItems());

        QualityInspectionPlanEntity plan = QualityInspectionPlanConvert.toEntity(request);
        plan.setVersionNo(INITIAL_VERSION);
        plan.setPlanStatus(PLAN_STATUS_DRAFT);
        plan.setCreateBy(getCurrentOperatorId());
        plan.setDeleted(false);
        savePlan(plan);
        savePlanItems(plan.getId(), request.getItems(), inspectionItemsById);
        return plan.getId();
    }

    /**
     * 仅草稿可整体更新。方案编码作为跨版本业务标识不可修改；方案项采用全量替换，
     * 删除后立即刷盘以消除旧唯一约束占用，再写入新快照集合。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePlan(Long id, QualityInspectionPlanSaveReqVO request) {
        QualityInspectionPlanEntity plan = getPlanForUpdate(id);
        validateDraftPlan(plan);
        if (!plan.getPlanCode().equals(request.getPlanCode())) {
            throw new ServiceException(QualityErrorCodeConstants.PLAN_CODE_DUPLICATE);
        }
        Map<Long, QualityInspectionItemEntity> inspectionItemsById = validatePlanItems(request.getItems());

        QualityInspectionPlanConvert.copyEditableFields(request, plan);
        savePlan(plan);
        planItemRepository.deleteByPlanId(id);
        planItemRepository.flush();
        savePlanItems(id, request.getItems(), inspectionItemsById);
        evictPlanCacheAfterCommit(id);
    }

    /** 仅草稿可删除；清除方案项后释放业务编码，并取消默认资格以免参与有效方案选择。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlan(Long id) {
        QualityInspectionPlanEntity plan = getPlanForUpdate(id);
        validateDraftPlan(plan);
        planItemRepository.deleteByPlanId(id);
        String deletedCode = DELETED_CODE_PREFIX + Long.toString(id, 36).toUpperCase();
        plan.setPlanCode(deletedCode);
        plan.setDefaultFlag(false);
        plan.setDeleted(true);
        savePlan(plan);
        evictPlanCacheAfterCommit(id);
    }

    /**
     * 将包含方案项的草稿审核为生效版本。锁定同检验类型方案后再检查产品、客户和检验类型组成的
     * 默认适用范围，使“检查冲突并生效”在并发审核下保持串行。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditPlan(Long id) {
        QualityInspectionPlanEntity plan = getPlanForUpdate(id);
        if (!PLAN_STATUS_DRAFT.equals(plan.getPlanStatus()) || planItemRepository.countByPlanId(id) == 0) {
            throw new ServiceException(QualityErrorCodeConstants.PLAN_AUDIT_NOT_ALLOWED);
        }

        // 锁粒度覆盖同检验类型候选集，确保并发事务不能同时通过默认方案唯一性检查。
        planRepository.lockPlansByInspectionType(plan.getInspectionType());
        if (Boolean.TRUE.equals(plan.getDefaultFlag())
                && planRepository.existsEffectiveDefaultForScope(
                        plan.getProductId(), plan.getCustomerId(), plan.getInspectionType(), plan.getId())) {
            throw new ServiceException(QualityErrorCodeConstants.PLAN_DEFAULT_CONFLICT);
        }
        plan.setPlanStatus(PLAN_STATUS_EFFECTIVE);
        if (plan.getEffectiveDate() == null) {
            plan.setEffectiveDate(LocalDate.now());
        }
        plan.setAuditBy(getCurrentOperatorId());
        plan.setAuditTime(LocalDateTime.now());
        savePlan(plan);
        evictPlanCacheAfterCommit(id);
    }

    /** EFFECTIVE 只能迁移到 DISABLED；停用同时撤销默认标记，不提供逆向恢复。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disablePlan(Long id) {
        QualityInspectionPlanEntity plan = getPlanForUpdate(id);
        if (!PLAN_STATUS_EFFECTIVE.equals(plan.getPlanStatus())) {
            throw new ServiceException(QualityErrorCodeConstants.PLAN_DISABLE_NOT_ALLOWED);
        }
        plan.setPlanStatus(PLAN_STATUS_DISABLED);
        plan.setDefaultFlag(false);
        savePlan(plan);
        evictPlanCacheAfterCommit(id);
    }

    /**
     * 从已审核或已停用版本派生新草稿。先按方案编码锁定所有版本，再计算最大版本号加一，
     * 从而避免两个并发请求获得相同版本号；方案项按原快照复制，不重新读取项目当前规则。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createNewVersion(Long id) {
        QualityInspectionPlanEntity sourcePlanSnapshot = getPlanEntity(id);
        List<QualityInspectionPlanEntity> lockedPlanVersions =
                planRepository.lockPlansByPlanCode(sourcePlanSnapshot.getPlanCode());
        QualityInspectionPlanEntity sourcePlan = lockedPlanVersions.stream()
                .filter(planVersion -> id.equals(planVersion.getId()))
                .findFirst()
                .orElseThrow(() -> new ServiceException(QualityErrorCodeConstants.PLAN_NOT_EXISTS));
        if (PLAN_STATUS_DRAFT.equals(sourcePlan.getPlanStatus())) {
            throw new ServiceException(QualityErrorCodeConstants.PLAN_VERSION_NOT_ALLOWED);
        }
        List<QualityInspectionPlanItemEntity> sourceItems =
                planItemRepository.findByPlanIdOrderBySortOrderAscIdAsc(id);
        if (sourceItems.isEmpty()) {
            throw new ServiceException(QualityErrorCodeConstants.PLAN_AUDIT_NOT_ALLOWED);
        }

        int maximumVersion = lockedPlanVersions.stream()
                .map(QualityInspectionPlanEntity::getVersionNo)
                .max(Integer::compareTo)
                .orElse(INITIAL_VERSION - 1);
        int nextVersion = maximumVersion + 1;
        QualityInspectionPlanEntity newVersion = copyPlanAsDraft(sourcePlan, nextVersion);
        savePlan(newVersion);
        List<QualityInspectionPlanItemEntity> copiedItems = sourceItems.stream()
                .map(sourceItem -> copyPlanItem(sourceItem, newVersion.getId()))
                .toList();
        savePlanItemEntities(copiedItems);
        return newVersion.getId();
    }

    /** 详情缓存包含方案主表、规则快照及当前项目冗余字段，缓存未命中时一次性完成装配。 */
    @Override
    @Transactional(readOnly = true)
    public QualityInspectionPlanRespVO getPlan(Long id) {
        return qualityCache.getOrLoadDetail(QualityRedisKeyConstants.INSPECTION_PLAN_RESOURCE,
                id, QualityInspectionPlanRespVO.class, () -> {
            QualityInspectionPlanEntity plan = getPlanEntity(id);
            List<QualityInspectionPlanItemEntity> planItems =
                    planItemRepository.findByPlanIdOrderBySortOrderAscIdAsc(id);
            QualityInspectionPlanRespVO response =
                    QualityInspectionPlanConvert.toRespVO(plan, planItems, loadInspectionItems(planItems));
            return response;
        });
    }

    /** 分页仅转换主表摘要，不加载方案项，页码越界时收敛到最后一页。 */
    @Override
    @Transactional(readOnly = true)
    public PageResult<QualityInspectionPlanRespVO> getPlanPage(QualityInspectionPlanPageReqVO request) {
        var specification = QualityInspectionPlanSpecifications.page(request);
        long total = planRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(request.getPageNo(), request.getPageSize());
        }
        int pageSize = request.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        int pageNo = Math.min(request.getPageNo(), totalPages);
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize,
                Sort.by(Sort.Direction.DESC, "createTime").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<QualityInspectionPlanEntity> page = planRepository.findAll(specification, pageRequest);
        List<QualityInspectionPlanRespVO> list = page.getContent().stream()
                .map(QualityInspectionPlanConvert::toSummaryRespVO)
                .toList();
        return PageResult.of(list, total, pageNo, pageSize);
    }

    /**
     * 批量校验方案项引用完整性。
     *
     * <p>同一方案不得重复引用检验项目；全部项目必须存在且未删除，再逐项校验启用状态以及覆盖后规则。</p>
     *
     * @return 按项目主键索引的主数据，供后续生成方案项快照复用
     */
    private Map<Long, QualityInspectionItemEntity> validatePlanItems(
            List<QualityInspectionPlanItemSaveReqVO> requests) {
        Set<Long> inspectionItemIds = requests.stream()
                .map(QualityInspectionPlanItemSaveReqVO::getInspectionItemId)
                .collect(Collectors.toSet());
        if (inspectionItemIds.size() != requests.size()) {
            throw new ServiceException(QualityErrorCodeConstants.PLAN_ITEMS_DUPLICATE);
        }
        Map<Long, QualityInspectionItemEntity> inspectionItemsById = inspectionItemRepository
                .findAllById(inspectionItemIds)
                .stream()
                .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
                .collect(Collectors.toMap(QualityInspectionItemEntity::getId, Function.identity()));
        if (inspectionItemsById.size() != inspectionItemIds.size()) {
            throw new ServiceException(QualityErrorCodeConstants.PLAN_ITEM_INVALID);
        }
        for (QualityInspectionPlanItemSaveReqVO request : requests) {
            QualityInspectionItemEntity inspectionItem = inspectionItemsById.get(request.getInspectionItemId());
            validatePlanItem(request, inspectionItem);
        }
        return inspectionItemsById;
    }

    /**
     * 校验单个方案项最终生效规则。请求字段非空时覆盖项目默认规则，否则继承项目当前值；
     * RANGE 必须对应数值项目及有序上下限，STANDARD_VALUE 必须有标准值。
     */
    private void validatePlanItem(QualityInspectionPlanItemSaveReqVO request,
                                  QualityInspectionItemEntity inspectionItem) {
        if (!Integer.valueOf(ENABLED).equals(inspectionItem.getEnabledStatus())) {
            throw new ServiceException(QualityErrorCodeConstants.PLAN_ITEM_INVALID);
        }
        String judgmentMethod = request.getJudgmentMethod() == null
                ? inspectionItem.getJudgmentMethod() : request.getJudgmentMethod();
        String standardValue = request.getStandardValue() == null
                ? inspectionItem.getStandardValue() : request.getStandardValue();
        var lowerLimit = request.getLowerLimit() == null
                ? inspectionItem.getLowerLimit() : request.getLowerLimit();
        var upperLimit = request.getUpperLimit() == null
                ? inspectionItem.getUpperLimit() : request.getUpperLimit();
        if (JUDGMENT_METHOD_RANGE.equals(judgmentMethod)
                && (!VALUE_TYPE_NUMERIC.equals(inspectionItem.getValueType())
                || lowerLimit == null || upperLimit == null || lowerLimit.compareTo(upperLimit) > 0)) {
            throw new ServiceException(QualityErrorCodeConstants.PLAN_ITEM_INVALID);
        }
        if (JUDGMENT_METHOD_STANDARD_VALUE.equals(judgmentMethod) && !StringUtils.hasText(standardValue)) {
            throw new ServiceException(QualityErrorCodeConstants.PLAN_ITEM_INVALID);
        }
    }

    /** 将已经校验的请求批量转换为方案项规则快照并持久化。 */
    private void savePlanItems(Long planId,
                               List<QualityInspectionPlanItemSaveReqVO> requests,
                               Map<Long, QualityInspectionItemEntity> inspectionItemsById) {
        List<QualityInspectionPlanItemEntity> entities = requests.stream()
                .map(request -> toPlanItemEntity(planId, request,
                        inspectionItemsById.get(request.getInspectionItemId())))
                .toList();
        savePlanItemEntities(entities);
    }

    /**
     * 生成方案项快照：排序默认为零；必检、标准值、上下限和判定方式均按请求优先、项目默认兜底。
     */
    private QualityInspectionPlanItemEntity toPlanItemEntity(
            Long planId,
            QualityInspectionPlanItemSaveReqVO request,
            QualityInspectionItemEntity inspectionItem) {
        QualityInspectionPlanItemEntity entity = new QualityInspectionPlanItemEntity();
        entity.setPlanId(planId);
        entity.setInspectionItemId(inspectionItem.getId());
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setSampleQuantity(request.getSampleQuantity());
        entity.setRequiredFlag(request.getRequiredFlag() == null
                ? inspectionItem.getRequiredFlag() : request.getRequiredFlag());
        entity.setStandardValue(request.getStandardValue() == null
                ? inspectionItem.getStandardValue() : request.getStandardValue());
        entity.setLowerLimit(request.getLowerLimit() == null
                ? inspectionItem.getLowerLimit() : request.getLowerLimit());
        entity.setUpperLimit(request.getUpperLimit() == null
                ? inspectionItem.getUpperLimit() : request.getUpperLimit());
        entity.setJudgmentMethod(request.getJudgmentMethod() == null
                ? inspectionItem.getJudgmentMethod() : request.getJudgmentMethod());
        return entity;
    }

    /**
     * 复制方案业务范围形成新草稿；明确清空生效日期并取消默认标记，审核字段由新实体保持为空。
     */
    private QualityInspectionPlanEntity copyPlanAsDraft(QualityInspectionPlanEntity sourcePlan,
                                                         Integer versionNo) {
        QualityInspectionPlanEntity entity = new QualityInspectionPlanEntity();
        entity.setPlanCode(sourcePlan.getPlanCode());
        entity.setPlanName(sourcePlan.getPlanName());
        entity.setProductId(sourcePlan.getProductId());
        entity.setCustomerId(sourcePlan.getCustomerId());
        entity.setInspectionType(sourcePlan.getInspectionType());
        entity.setVersionNo(versionNo);
        entity.setPlanStatus(PLAN_STATUS_DRAFT);
        entity.setEffectiveDate(null);
        entity.setDefaultFlag(false);
        entity.setRemark(sourcePlan.getRemark());
        entity.setCreateBy(getCurrentOperatorId());
        entity.setDeleted(false);
        return entity;
    }

    /** 完整复制源版本的方案项规则快照，仅替换所属方案主键。 */
    private QualityInspectionPlanItemEntity copyPlanItem(QualityInspectionPlanItemEntity sourceItem,
                                                          Long targetPlanId) {
        QualityInspectionPlanItemEntity entity = new QualityInspectionPlanItemEntity();
        entity.setPlanId(targetPlanId);
        entity.setInspectionItemId(sourceItem.getInspectionItemId());
        entity.setSortOrder(sourceItem.getSortOrder());
        entity.setSampleQuantity(sourceItem.getSampleQuantity());
        entity.setRequiredFlag(sourceItem.getRequiredFlag());
        entity.setStandardValue(sourceItem.getStandardValue());
        entity.setLowerLimit(sourceItem.getLowerLimit());
        entity.setUpperLimit(sourceItem.getUpperLimit());
        entity.setJudgmentMethod(sourceItem.getJudgmentMethod());
        return entity;
    }

    /** 批量加载当前项目主数据，仅用于方案详情中的描述字段冗余展示。 */
    private Map<Long, QualityInspectionItemEntity> loadInspectionItems(
            List<QualityInspectionPlanItemEntity> planItems) {
        Set<Long> inspectionItemIds = planItems.stream()
                .map(QualityInspectionPlanItemEntity::getInspectionItemId)
                .collect(Collectors.toCollection(HashSet::new));
        return inspectionItemRepository.findAllById(inspectionItemIds).stream()
                .collect(Collectors.toMap(QualityInspectionItemEntity::getId, Function.identity()));
    }

    /** 统一守卫草稿专属操作，阻止已审核版本被原地修改。 */
    private void validateDraftPlan(QualityInspectionPlanEntity plan) {
        if (!PLAN_STATUS_DRAFT.equals(plan.getPlanStatus())) {
            throw new ServiceException(QualityErrorCodeConstants.PLAN_EDIT_NOT_ALLOWED);
        }
    }

    /** 读取未删除方案，不获取写锁。 */
    private QualityInspectionPlanEntity getPlanEntity(Long id) {
        return planRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(QualityErrorCodeConstants.PLAN_NOT_EXISTS));
    }

    /** 悲观锁定目标方案，保证状态检查与状态迁移不可被并发写入穿透。 */
    private QualityInspectionPlanEntity getPlanForUpdate(Long id) {
        return planRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(QualityErrorCodeConstants.PLAN_NOT_EXISTS));
    }

    /** 立即刷盘，并将方案编码/版本唯一约束冲突转换为稳定业务异常。 */
    private void savePlan(QualityInspectionPlanEntity plan) {
        try {
            planRepository.saveAndFlush(plan);
        } catch (DataIntegrityViolationException exception) {
            throw new ServiceException(QualityErrorCodeConstants.PLAN_CODE_DUPLICATE);
        }
    }

    /** 批量刷盘方案项，将重复项目等数据完整性冲突统一映射为方案项重复。 */
    private void savePlanItemEntities(List<QualityInspectionPlanItemEntity> planItems) {
        try {
            planItemRepository.saveAllAndFlush(planItems);
        } catch (DataIntegrityViolationException exception) {
            throw new ServiceException(QualityErrorCodeConstants.PLAN_ITEMS_DUPLICATE);
        }
    }

    /** 仅在事务成功提交后失效方案详情，避免回滚事务污染缓存可见性。 */
    private void evictPlanCacheAfterCommit(Long id) {
        qualityCache.evictDetailAfterCommit(QualityRedisKeyConstants.INSPECTION_PLAN_RESOURCE, id);
    }

    /** 无登录上下文的系统调用回退到默认操作人。 */
    private Long getCurrentOperatorId() {
        if (SecurityContextHolder.getLoginUser() == null) {
            return DEFAULT_OPERATOR_ID;
        }
        return SecurityContextHolder.getRequiredLoginUserId();
    }
}
