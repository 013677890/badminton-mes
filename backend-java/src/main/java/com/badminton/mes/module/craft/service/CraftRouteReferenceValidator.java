package com.badminton.mes.module.craft.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.badminton.mes.common.core.GlobalErrorCodeConstants;
import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.craft.constants.CraftErrorCodeConstants;
import com.badminton.mes.module.craft.controller.vo.CraftRouteStepSaveReqVO;
import com.badminton.mes.module.craft.dal.entity.CraftProcessEntity;
import com.badminton.mes.module.craft.dal.entity.CraftProcessSopEntity;
import com.badminton.mes.module.craft.dal.entity.CraftQualityPlanReferenceEntity;
import com.badminton.mes.module.craft.dal.entity.CraftRouteDetailEntity;
import com.badminton.mes.module.craft.dal.entity.CraftRouteProductEntity;
import com.badminton.mes.module.craft.dal.entity.CraftWorkstationReferenceEntity;
import com.badminton.mes.module.craft.dal.repository.CraftProcessRepository;
import com.badminton.mes.module.craft.dal.repository.CraftProcessSopRepository;
import com.badminton.mes.module.craft.dal.repository.CraftQualityPlanReferenceRepository;
import com.badminton.mes.module.craft.dal.repository.CraftWorkstationReferenceRepository;
import com.badminton.mes.module.craft.service.dto.CraftRouteReferenceContext;
import com.badminton.mes.module.equipment.dal.entity.EquipmentCategoryEntity;
import com.badminton.mes.module.equipment.dal.repository.EquipmentCategoryRepository;
import com.badminton.mes.module.production.dal.entity.ProductEntity;
import com.badminton.mes.module.production.dal.repository.ProductRepository;

import org.springframework.stereotype.Service;

/**
 * 工艺路线聚合引用和生效完整性校验器。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Service
public class CraftRouteReferenceValidator {

    private final ProductRepository productRepository;

    private final CraftProcessRepository processRepository;

    private final CraftWorkstationReferenceRepository workstationRepository;

    private final EquipmentCategoryRepository equipmentCategoryRepository;

    private final CraftProcessSopRepository sopRepository;

    private final CraftQualityPlanReferenceRepository qualityPlanRepository;

    /**
     * 构造器注入。
     *
     * @param productRepository           产品 Repository
     * @param processRepository           工序 Repository
     * @param workstationRepository       工位只读 Repository
     * @param equipmentCategoryRepository 设备类别 Repository
     * @param sopRepository               工序 SOP Repository
     * @param qualityPlanRepository       检验方案只读 Repository
     */
    public CraftRouteReferenceValidator(
            ProductRepository productRepository,
            CraftProcessRepository processRepository,
            CraftWorkstationReferenceRepository workstationRepository,
            EquipmentCategoryRepository equipmentCategoryRepository,
            CraftProcessSopRepository sopRepository,
            CraftQualityPlanReferenceRepository qualityPlanRepository) {
        this.productRepository = productRepository;
        this.processRepository = processRepository;
        this.workstationRepository = workstationRepository;
        this.equipmentCategoryRepository = equipmentCategoryRepository;
        this.sopRepository = sopRepository;
        this.qualityPlanRepository = qualityPlanRepository;
    }

    /**
     * 校验草稿保存请求，并把工序默认控制规则继承到待落库步骤。
     *
     * @param productIds 产品主键列表
     * @param steps      路线步骤请求
     * @return 可用产品与工序映射
     */
    public CraftRouteReferenceContext validateForSave(
            List<Long> productIds, List<CraftRouteStepSaveReqVO> steps) {
        return validate(productIds, steps, true, false, false);
    }

    /**
     * 校验数据库中已保存路线达到生效标准。
     *
     * <p>审核路径严格校验持久化值，不再向临时对象继承工序规则；同时写锁全部引用，
     * 防止引用在校验通过到路线生效之间被并发停用或删除。
     *
     * @param relations 产品关系
     * @param details   路线明细
     * @return 可用产品与工序映射
     */
    public CraftRouteReferenceContext validateForApproval(
            List<CraftRouteProductEntity> relations,
            List<CraftRouteDetailEntity> details) {
        // 将已持久化子表转换为统一校验模型，保证保存与审核复用同一套业务规则。
        List<Long> productIds = relations.stream()
                .map(CraftRouteProductEntity::getProductId)
                .toList();
        List<CraftRouteStepSaveReqVO> steps = details.stream()
                .map(this::toStepRequest)
                .toList();
        return validate(productIds, steps, false, true, true);
    }

    /**
     * 统一校验路线引用和控制配置。
     *
     * @param productIds       产品主键列表
     * @param steps            路线步骤
     * @param inheritRules     是否继承工序默认规则
     * @param approvalRequired 是否按生效标准检查
     * @param lockReferences   是否写锁被引用主档
     * @return 可用产品与工序映射
     */
    private CraftRouteReferenceContext validate(
            List<Long> productIds,
            List<CraftRouteStepSaveReqVO> steps,
            boolean inheritRules,
            boolean approvalRequired,
            boolean lockReferences) {
        // 先校验纯内存结构，尽早拒绝重复产品和断裂步骤，减少无效数据库查询。
        validateNoDuplicateProducts(productIds);
        validateContinuousSequence(steps);

        Map<Long, ProductEntity> productMap = loadAvailableProducts(productIds);
        Map<Long, CraftProcessEntity> processMap = loadAvailableProcesses(steps, lockReferences);
        if (inheritRules) {
            // 草稿保存允许省略工序默认控制项，由工序主档补齐后再写入路线明细。
            inheritProcessRules(steps, processMap);
        }
        validateWorkstations(steps, lockReferences);
        validateEquipmentCategories(steps, lockReferences);
        Map<Long, CraftProcessSopEntity> sopMap = validateSops(steps, lockReferences);
        validateQualityPlans(steps, lockReferences);
        if (approvalRequired) {
            // 审核阶段不再自动修正数据，只验证持久化配置是否已满足生效条件。
            validateApprovalConfiguration(steps, processMap, sopMap);
        }
        return new CraftRouteReferenceContext(productMap, processMap);
    }

    /**
     * 校验产品列表不重复。
     *
     * @param productIds 产品主键列表
     */
    private void validateNoDuplicateProducts(List<Long> productIds) {
        if (new HashSet<>(productIds).size() != productIds.size()) {
            throw new ServiceException(GlobalErrorCodeConstants.PARAM_ERROR, "适用产品不能重复");
        }
    }

    /**
     * 校验步骤顺序从 1 开始且连续。
     *
     * @param steps 路线步骤
     */
    private void validateContinuousSequence(List<CraftRouteStepSaveReqVO> steps) {
        // 请求列表位置就是预期执行顺序，因此第 index 项必须严格对应 index + 1。
        for (int index = 0; index < steps.size(); index++) {
            if (steps.get(index).getSequenceNo() != index + 1) {
                throw new ServiceException(CraftErrorCodeConstants.ROUTE_SEQUENCE_INVALID);
            }
        }
    }

    /**
     * 批量加载可用产品并校验数量一致。
     *
     * @param productIds 产品主键列表
     * @return 产品映射
     */
    private Map<Long, ProductEntity> loadAvailableProducts(List<Long> productIds) {
        // 一次 IN 查询只加载启用且未删除产品，数量不一致即说明至少一个引用不可用。
        List<ProductEntity> products = productRepository.findByIdInAndStatusAndDeletedFalse(
                productIds, CommonStatusEnum.ENABLED.getStatus());
        if (products.size() != productIds.size()) {
            throw new ServiceException(CraftErrorCodeConstants.ROUTE_PRODUCT_NOT_AVAILABLE);
        }
        return products.stream().collect(Collectors.toMap(ProductEntity::getId, Function.identity()));
    }

    /**
     * 批量加载可用工序并校验全部步骤均可引用。
     *
     * @param steps          路线步骤
     * @param lockReferences 是否写锁引用
     * @return 工序映射
     */
    private Map<Long, CraftProcessEntity> loadAvailableProcesses(
            List<CraftRouteStepSaveReqVO> steps, boolean lockReferences) {
        // 同一工序可出现在多个步骤，先去重以减少 IN 查询与加锁范围。
        Set<Long> processIds = steps.stream()
                .map(CraftRouteStepSaveReqVO::getProcessId)
                .collect(Collectors.toSet());
        // 审核时按主键升序加写锁，防止校验完成到路线生效之间工序被停用或删除。
        List<CraftProcessEntity> processes = lockReferences
                ? processRepository.findAvailableByIdInForUpdateOrderByIdAsc(
                        processIds, CommonStatusEnum.ENABLED.getStatus())
                : processRepository.findByIdInAndStatusAndDeletedFalse(
                        processIds, CommonStatusEnum.ENABLED.getStatus());
        if (processes.size() != processIds.size()) {
            throw new ServiceException(CraftErrorCodeConstants.ROUTE_PROCESS_NOT_AVAILABLE);
        }
        return processes.stream().collect(Collectors.toMap(CraftProcessEntity::getId, Function.identity()));
    }

    /**
     * 将工序档案中的设备和质量规则继承到未显式配置的路线步骤。
     *
     * @param steps      路线步骤
     * @param processMap 工序映射
     */
    private void inheritProcessRules(
            List<CraftRouteStepSaveReqVO> steps,
            Map<Long, CraftProcessEntity> processMap) {
        for (CraftRouteStepSaveReqVO step : steps) {
            CraftProcessEntity process = processMap.get(step.getProcessId());
            if (step.getEquipmentCategoryId() == null) {
                // 路线未覆盖设备类别时继承工序默认值，显式配置则保持路线自身选择。
                step.setEquipmentCategoryId(process.getEquipmentCategoryId());
            }
            if (Boolean.TRUE.equals(process.getQualityRequired())) {
                // 工序要求质检时，路线步骤必须成为检验节点并继承缺省检验方案。
                step.setInspectNode(true);
                if (step.getQualityPlanId() == null) {
                    step.setQualityPlanId(process.getQualityPlanId());
                }
            }
        }
    }

    /**
     * 校验步骤工位引用。
     *
     * @param steps          路线步骤
     * @param lockReferences 是否写锁引用
     */
    private void validateWorkstations(
            List<CraftRouteStepSaveReqVO> steps, boolean lockReferences) {
        Set<Long> stationIds = nonNullIds(steps.stream()
                .map(CraftRouteStepSaveReqVO::getStationId)
                .toList());
        if (stationIds.isEmpty()) {
            return;
        }
        // 保存只校验可用性，审核则锁定引用；两条路径都使用批量查询避免 N+1。
        List<CraftWorkstationReferenceEntity> stations = lockReferences
                ? workstationRepository.findAvailableByIdInForUpdateOrderByIdAsc(
                        stationIds, CommonStatusEnum.ENABLED.getStatus())
                : workstationRepository.findByIdInAndStatusAndDeletedFalse(
                        stationIds, CommonStatusEnum.ENABLED.getStatus());
        if (stations.size() != stationIds.size()) {
            throw new ServiceException(CraftErrorCodeConstants.ROUTE_STATION_NOT_AVAILABLE);
        }
    }

    /**
     * 校验步骤设备类别引用。
     *
     * @param steps          路线步骤
     * @param lockReferences 是否写锁引用
     */
    private void validateEquipmentCategories(
            List<CraftRouteStepSaveReqVO> steps, boolean lockReferences) {
        Set<Long> categoryIds = nonNullIds(steps.stream()
                .map(CraftRouteStepSaveReqVO::getEquipmentCategoryId)
                .toList());
        if (categoryIds.isEmpty()) {
            return;
        }
        List<EquipmentCategoryEntity> categories = lockReferences
                ? equipmentCategoryRepository.findAvailableByIdInForUpdateOrderByIdAsc(
                        categoryIds, CommonStatusEnum.ENABLED.getStatus())
                : equipmentCategoryRepository.findByIdInAndStatusAndDeletedFalse(
                        categoryIds, CommonStatusEnum.ENABLED.getStatus());
        if (categories.size() != categoryIds.size()) {
            throw new ServiceException(
                    CraftErrorCodeConstants.ROUTE_EQUIPMENT_CATEGORY_NOT_AVAILABLE);
        }
    }

    /**
     * 校验 SOP 可用并返回映射。
     *
     * @param steps          路线步骤
     * @param lockReferences 是否写锁引用
     * @return SOP 映射
     */
    private Map<Long, CraftProcessSopEntity> validateSops(
            List<CraftRouteStepSaveReqVO> steps, boolean lockReferences) {
        Set<Long> sopIds = nonNullIds(steps.stream()
                .map(CraftRouteStepSaveReqVO::getSopId)
                .toList());
        if (sopIds.isEmpty()) {
            return Map.of();
        }
        // SOP 除了必须启用外，还必须属于当前步骤引用的同一工序。
        List<CraftProcessSopEntity> sops = lockReferences
                ? sopRepository.findAvailableByIdInForUpdateOrderByIdAsc(
                        sopIds, CommonStatusEnum.ENABLED.getStatus())
                : sopRepository.findByIdInAndStatusAndDeletedFalse(
                        sopIds, CommonStatusEnum.ENABLED.getStatus());
        if (sops.size() != sopIds.size()) {
            throw new ServiceException(CraftErrorCodeConstants.ROUTE_SOP_NOT_AVAILABLE);
        }
        Map<Long, CraftProcessSopEntity> sopMap = sops.stream()
                .collect(Collectors.toMap(CraftProcessSopEntity::getId, Function.identity()));
        boolean mismatched = steps.stream()
                .filter(step -> step.getSopId() != null)
                .anyMatch(step -> !step.getProcessId().equals(sopMap.get(step.getSopId()).getProcessId()));
        if (mismatched) {
            throw new ServiceException(CraftErrorCodeConstants.ROUTE_SOP_NOT_AVAILABLE);
        }
        return sopMap;
    }

    /**
     * 校验检验方案引用。
     *
     * @param steps          路线步骤
     * @param lockReferences 是否写锁引用
     */
    private void validateQualityPlans(
            List<CraftRouteStepSaveReqVO> steps, boolean lockReferences) {
        Set<Long> planIds = nonNullIds(steps.stream()
                .map(CraftRouteStepSaveReqVO::getQualityPlanId)
                .toList());
        if (planIds.isEmpty()) {
            return;
        }
        List<CraftQualityPlanReferenceEntity> plans = lockReferences
                ? qualityPlanRepository.findAvailableByIdInForUpdateOrderByIdAsc(
                        planIds, CraftQualityPlanReferenceEntity.PLAN_STATUS_EFFECTIVE)
                : qualityPlanRepository.findByIdInAndPlanStatusAndDeletedFalse(
                        planIds, CraftQualityPlanReferenceEntity.PLAN_STATUS_EFFECTIVE);
        if (plans.size() != planIds.size()) {
            throw new ServiceException(CraftErrorCodeConstants.ROUTE_QUALITY_PLAN_NOT_AVAILABLE);
        }
    }

    /**
     * 校验关键工序和质检节点达到生效配置标准。
     *
     * @param steps      路线步骤
     * @param processMap 工序映射
     * @param sopMap     SOP 映射
     */
    private void validateApprovalConfiguration(
            List<CraftRouteStepSaveReqVO> steps,
            Map<Long, CraftProcessEntity> processMap,
            Map<Long, CraftProcessSopEntity> sopMap) {
        for (CraftRouteStepSaveReqVO step : steps) {
            CraftProcessEntity process = processMap.get(step.getProcessId());
            boolean qualityRequired = Boolean.TRUE.equals(process.getQualityRequired());
            // 关键工序、强制质检工序或显式检验节点均视为控制节点，生效前必须绑定 SOP。
            boolean controlNode = Boolean.TRUE.equals(process.getKeyProcess())
                    || qualityRequired
                    || Boolean.TRUE.equals(step.getInspectNode());
            boolean missingEquipment = process.getEquipmentCategoryId() != null
                    && step.getEquipmentCategoryId() == null;
            boolean inspectRuleMismatch = qualityRequired
                    && !Boolean.TRUE.equals(step.getInspectNode());
            boolean missingSop = controlNode && (step.getSopId() == null
                    || !sopMap.containsKey(step.getSopId()));
            boolean missingQualityPlan = (qualityRequired || Boolean.TRUE.equals(step.getInspectNode()))
                    && step.getQualityPlanId() == null;
            // 任一控制配置缺失都拒绝整条路线生效，避免现场执行到中途才发现资料不全。
            if (missingEquipment || inspectRuleMismatch || missingSop || missingQualityPlan) {
                throw new ServiceException(CraftErrorCodeConstants.ROUTE_CONFIGURATION_INCOMPLETE);
            }
        }
    }

    /**
     * 过滤集合中的 null 并转为去重集合。
     *
     * @param ids 可能包含 null 的主键集合
     * @return 非空主键集合
     */
    private Set<Long> nonNullIds(Collection<Long> ids) {
        return ids.stream().filter(id -> id != null).collect(Collectors.toSet());
    }

    /**
     * 数据库明细转校验请求对象。
     *
     * @param detail 路线明细实体
     * @return 步骤校验请求
     */
    private CraftRouteStepSaveReqVO toStepRequest(CraftRouteDetailEntity detail) {
        CraftRouteStepSaveReqVO step = new CraftRouteStepSaveReqVO();
        step.setSequenceNo(detail.getSequenceNo());
        step.setProcessId(detail.getProcessId());
        step.setStationId(detail.getStationId());
        step.setEquipmentCategoryId(detail.getEquipmentCategoryId());
        step.setInspectNode(detail.getInspect());
        step.setSopId(detail.getSopId());
        step.setQualityPlanId(detail.getQualityPlanId());
        return step;
    }
}
