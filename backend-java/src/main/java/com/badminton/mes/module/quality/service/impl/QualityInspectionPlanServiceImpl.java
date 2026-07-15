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

/** 检验标准方案 Service 实现。 */
@Service
public class QualityInspectionPlanServiceImpl implements QualityInspectionPlanService {

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

    public QualityInspectionPlanServiceImpl(QualityInspectionPlanRepository planRepository,
                                            QualityInspectionPlanItemRepository planItemRepository,
                                            QualityInspectionItemRepository inspectionItemRepository,
                                            QualityCache qualityCache) {
        this.planRepository = planRepository;
        this.planItemRepository = planItemRepository;
        this.inspectionItemRepository = inspectionItemRepository;
        this.qualityCache = qualityCache;
    }

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditPlan(Long id) {
        QualityInspectionPlanEntity plan = getPlanForUpdate(id);
        if (!PLAN_STATUS_DRAFT.equals(plan.getPlanStatus()) || planItemRepository.countByPlanId(id) == 0) {
            throw new ServiceException(QualityErrorCodeConstants.PLAN_AUDIT_NOT_ALLOWED);
        }

        // 锁定同检验类型的方案，串行化同适用范围默认方案的审核操作。
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

    private void savePlanItems(Long planId,
                               List<QualityInspectionPlanItemSaveReqVO> requests,
                               Map<Long, QualityInspectionItemEntity> inspectionItemsById) {
        List<QualityInspectionPlanItemEntity> entities = requests.stream()
                .map(request -> toPlanItemEntity(planId, request,
                        inspectionItemsById.get(request.getInspectionItemId())))
                .toList();
        savePlanItemEntities(entities);
    }

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

    private Map<Long, QualityInspectionItemEntity> loadInspectionItems(
            List<QualityInspectionPlanItemEntity> planItems) {
        Set<Long> inspectionItemIds = planItems.stream()
                .map(QualityInspectionPlanItemEntity::getInspectionItemId)
                .collect(Collectors.toCollection(HashSet::new));
        return inspectionItemRepository.findAllById(inspectionItemIds).stream()
                .collect(Collectors.toMap(QualityInspectionItemEntity::getId, Function.identity()));
    }

    private void validateDraftPlan(QualityInspectionPlanEntity plan) {
        if (!PLAN_STATUS_DRAFT.equals(plan.getPlanStatus())) {
            throw new ServiceException(QualityErrorCodeConstants.PLAN_EDIT_NOT_ALLOWED);
        }
    }

    private QualityInspectionPlanEntity getPlanEntity(Long id) {
        return planRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(QualityErrorCodeConstants.PLAN_NOT_EXISTS));
    }

    private QualityInspectionPlanEntity getPlanForUpdate(Long id) {
        return planRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(QualityErrorCodeConstants.PLAN_NOT_EXISTS));
    }

    private void savePlan(QualityInspectionPlanEntity plan) {
        try {
            planRepository.saveAndFlush(plan);
        } catch (DataIntegrityViolationException exception) {
            throw new ServiceException(QualityErrorCodeConstants.PLAN_CODE_DUPLICATE);
        }
    }

    private void savePlanItemEntities(List<QualityInspectionPlanItemEntity> planItems) {
        try {
            planItemRepository.saveAllAndFlush(planItems);
        } catch (DataIntegrityViolationException exception) {
            throw new ServiceException(QualityErrorCodeConstants.PLAN_ITEMS_DUPLICATE);
        }
    }

    private void evictPlanCacheAfterCommit(Long id) {
        qualityCache.evictDetailAfterCommit(QualityRedisKeyConstants.INSPECTION_PLAN_RESOURCE, id);
    }

    private Long getCurrentOperatorId() {
        if (SecurityContextHolder.getLoginUser() == null) {
            return DEFAULT_OPERATOR_ID;
        }
        return SecurityContextHolder.getRequiredLoginUserId();
    }
}
