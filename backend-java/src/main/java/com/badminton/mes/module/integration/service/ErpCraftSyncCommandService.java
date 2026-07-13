package com.badminton.mes.module.integration.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.craft.controller.vo.CraftRouteSaveReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteStepSaveReqVO;
import com.badminton.mes.module.craft.dal.entity.CraftProcessEntity;
import com.badminton.mes.module.craft.dal.repository.CraftProcessRepository;
import com.badminton.mes.module.craft.service.CraftRouteService;
import com.badminton.mes.module.integration.constants.IntegrationErrorCodeConstants;
import com.badminton.mes.module.integration.dal.entity.ErpCraftPendingEntity;
import com.badminton.mes.module.integration.dal.repository.ErpCraftPendingRepository;
import com.badminton.mes.module.integration.enums.ErpCraftPendingStatusEnum;
import com.badminton.mes.module.integration.enums.IntegrationInterfaceTypeEnum;
import com.badminton.mes.module.integration.enums.IntegrationWriteStatusEnum;
import com.badminton.mes.module.integration.service.dto.ErpCraftDTO;
import com.badminton.mes.module.integration.service.dto.ErpCraftStepDTO;
import com.badminton.mes.module.integration.service.dto.ErpCraftSyncResult;
import com.badminton.mes.module.production.dal.entity.ProductEntity;
import com.badminton.mes.module.production.dal.repository.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * ERP 工艺数据同步命令服务，负责单条工艺路线的校验、暂存和确认转草稿。
 *
 * <p>同步时校验产品和工序，有效数据进入待确认区，无效数据标记异常但仍存储。
 * 确认时反序列化工序步骤、查找产品和工序主键，调用 CraftRouteService.createRoute 生成草稿。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Service
public class ErpCraftSyncCommandService {

    private static final String CONFIRM_REASON = "ERP 工艺数据确认";

    private static final String ERP_CRAFT_UNIQUE_CONSTRAINT = "uk_source_code_version";

    private final ErpCraftPendingRepository pendingRepository;

    private final ProductRepository productRepository;

    private final CraftProcessRepository processRepository;

    private final CraftRouteService craftRouteService;

    private final IntegrationAuditService auditService;

    private final ObjectMapper objectMapper;

    /**
     * 构造 ERP 工艺同步命令服务。
     *
     * @param pendingRepository  待确认数据 Repository
     * @param productRepository  产品 Repository
     * @param processRepository  工序 Repository
     * @param craftRouteService  工艺路线 Service
     * @param auditService       接口审计服务
     * @param objectMapper       JSON 序列化器
     */
    public ErpCraftSyncCommandService(ErpCraftPendingRepository pendingRepository,
                                      ProductRepository productRepository,
                                      CraftProcessRepository processRepository,
                                      CraftRouteService craftRouteService,
                                      IntegrationAuditService auditService,
                                      ObjectMapper objectMapper) {
        this.pendingRepository = pendingRepository;
        this.productRepository = productRepository;
        this.processRepository = processRepository;
        this.craftRouteService = craftRouteService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    /**
     * 同步单条 ERP 工艺路线，校验后存入待确认区。
     *
     * <p>已存在待确认或已确认数据视为重复；已存在异常数据则覆盖重新校验。
     *
     * @param craft        ERP 工艺数据
     * @param snapshot     请求快照
     * @param sourceSystem 来源系统
     * @return 同步结果
     */
    @Transactional(rollbackFor = Exception.class)
    public ErpCraftSyncResult syncCraft(ErpCraftDTO craft, String snapshot, String sourceSystem) {
        validateCraftStructure(craft);
        String routingCode = craft.erpRoutingCode().trim();
        String routingVersion = craft.erpRoutingVersion().trim();
        String businessKey = routingCode + ":" + routingVersion;

        Optional<ErpCraftPendingEntity> existing = pendingRepository
                .findBySourceSystemAndErpRoutingCodeAndErpRoutingVersion(
                        sourceSystem, routingCode, routingVersion);
        if (existing.isPresent()
                && !ErpCraftPendingStatusEnum.FAILED.getStatus().equals(existing.get().getStatus())) {
            Long logId = auditService.recordResult(
                    IntegrationInterfaceTypeEnum.ERP_CRAFT_SYNC,
                    sourceSystem, businessKey, snapshot,
                    IntegrationWriteStatusEnum.DUPLICATE,
                    existing.get().getId(), businessKey);
            return new ErpCraftSyncResult(null, true, logId, null, null);
        }

        ErpCraftPendingEntity pending = existing.orElseGet(ErpCraftPendingEntity::new);
        fillPendingFields(pending, craft, sourceSystem);
        if (pending.getId() == null) {
            pending.setCreateBy(SecurityContextHolder.getRequiredLoginUserId());
        }
        clearErrorFields(pending);

        ServiceException validationFailure = null;
        try {
            List<ErpCraftStepDTO> sortedSteps = sortAndValidateSteps(craft.steps());
            pending.setProcessSteps(serializeSteps(sortedSteps));
            validateCraft(craft, sortedSteps);
        } catch (ServiceException exception) {
            validationFailure = exception;
        }
        if (validationFailure != null) {
            return saveFailedPending(
                    pending, craft.steps(), sourceSystem, businessKey, snapshot, validationFailure);
        }

        pending.setStatus(ErpCraftPendingStatusEnum.PENDING.getStatus());
        savePending(pending);
        Long logId = auditService.recordResult(
                IntegrationInterfaceTypeEnum.ERP_CRAFT_SYNC,
                sourceSystem, businessKey, snapshot,
                IntegrationWriteStatusEnum.SUCCESS,
                pending.getId(), businessKey);
        return new ErpCraftSyncResult(pending, false, logId, null, null);
    }

    /**
     * 查询已提交的 ERP 工艺待确认数据，供唯一键竞争后的门面查询获胜结果。
     *
     * @param sourceSystem   来源系统
     * @param routingCode    ERP 路线编码
     * @param routingVersion ERP 路线版本
     * @return 已提交的待确认数据
     */
    @Transactional(readOnly = true)
    public Optional<ErpCraftPendingEntity> findSyncedCraft(
            String sourceSystem,
            String routingCode,
            String routingVersion) {
        return pendingRepository.findBySourceSystemAndErpRoutingCodeAndErpRoutingVersion(
                sourceSystem, routingCode, routingVersion);
    }

    /**
     * 确认待确认工艺数据，生成 MES 工艺路线草稿。
     *
     * @param id 待确认数据主键
     * @return 新生成的工艺路线主键
     */
    @Transactional(rollbackFor = Exception.class)
    public Long confirmCraft(Long id) {
        ErpCraftPendingEntity pending = pendingRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ServiceException(
                        IntegrationErrorCodeConstants.ERP_CRAFT_PENDING_NOT_EXISTS));
        if (!ErpCraftPendingStatusEnum.PENDING.getStatus().equals(pending.getStatus())) {
            throw new ServiceException(
                    IntegrationErrorCodeConstants.ERP_CRAFT_PENDING_STATUS_INVALID);
        }

        List<ErpCraftStepDTO> steps = deserializeSteps(pending.getProcessSteps());
        CraftRouteSaveReqVO routeReqVO = buildRouteSaveReqVO(pending, steps);
        Long routeId = craftRouteService.createRoute(routeReqVO);

        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        pending.setStatus(ErpCraftPendingStatusEnum.CONFIRMED.getStatus());
        pending.setConfirmedRouteId(routeId);
        pending.setConfirmedBy(operatorId);
        pending.setConfirmTime(LocalDateTime.now());
        pendingRepository.saveAndFlush(pending);
        return routeId;
    }

    /**
     * 查询待确认数据，供门面层构造响应。
     *
     * @param id 主键
     * @return 待确认数据
     */
    @Transactional(readOnly = true)
    public Optional<ErpCraftPendingEntity> findPending(Long id) {
        return pendingRepository.findById(id);
    }

    /**
     * 校验 ERP 工艺数据：产品存在、工序编码存在、工序顺序完整不重复。
     *
     * @param craft ERP 工艺数据
     */
    private void validateCraft(ErpCraftDTO craft, List<ErpCraftStepDTO> sortedSteps) {
        validateProductExists(craft.productCode());
        validateProcessCodes(sortedSteps);
    }

    /**
     * 校验 ERP 工艺头和步骤结构，避免空字段或空步骤中断整个同步批次。
     *
     * @param craft ERP 工艺数据
     */
    private void validateCraftStructure(ErpCraftDTO craft) {
        boolean requiredTextMissing = craft == null
                || !StringUtils.hasText(craft.erpRoutingCode())
                || !StringUtils.hasText(craft.erpRoutingName())
                || !StringUtils.hasText(craft.erpRoutingVersion())
                || !StringUtils.hasText(craft.productCode());
        if (requiredTextMissing) {
            throw new ServiceException(IntegrationErrorCodeConstants.ERP_SOURCE_DATA_INVALID);
        }
    }

    /**
     * 按顺序号排序并校验步骤连续性，使同步校验和正式路线创建使用同一顺序契约。
     *
     * @param steps ERP 工序步骤
     * @return 已按顺序号升序排列的不可变副本
     */
    private List<ErpCraftStepDTO> sortAndValidateSteps(List<ErpCraftStepDTO> steps) {
        if (steps == null || steps.isEmpty()) {
            throw new ServiceException(IntegrationErrorCodeConstants.ERP_SOURCE_DATA_INVALID);
        }
        boolean invalidStep = steps.stream().anyMatch(step -> step == null
                || step.sequenceNo() == null
                || !StringUtils.hasText(step.processCode()));
        if (invalidStep) {
            throw new ServiceException(IntegrationErrorCodeConstants.ERP_CRAFT_SEQUENCE_INVALID);
        }
        List<ErpCraftStepDTO> sortedSteps = steps.stream()
                .sorted(Comparator.comparing(ErpCraftStepDTO::sequenceNo))
                .toList();
        validateSequence(sortedSteps);
        return sortedSteps;
    }

    /**
     * 保存校验失败的待确认数据，并在同一事务中记录失败日志。
     *
     * @param pending          待确认实体
     * @param originalSteps    ERP 原始步骤
     * @param sourceSystem     来源系统
     * @param businessKey      来源业务键
     * @param snapshot         请求快照
     * @param validationFailure 校验异常
     * @return 失败同步结果
     */
    private ErpCraftSyncResult saveFailedPending(
            ErpCraftPendingEntity pending,
            List<ErpCraftStepDTO> originalSteps,
            String sourceSystem,
            String businessKey,
            String snapshot,
            ServiceException validationFailure) {
        if (pending.getProcessSteps() == null) {
            List<ErpCraftStepDTO> storableSteps = originalSteps == null ? List.of() : originalSteps;
            pending.setProcessSteps(serializeSteps(storableSteps));
        }
        pending.setStatus(ErpCraftPendingStatusEnum.FAILED.getStatus());
        pending.setErrorCode(validationFailure.getErrorCode().code());
        pending.setErrorMessage(validationFailure.getMessage());
        savePending(pending);
        Long logId = auditService.recordFailureInCurrentTransaction(
                IntegrationInterfaceTypeEnum.ERP_CRAFT_SYNC,
                sourceSystem, businessKey, snapshot,
                pending.getId(), businessKey,
                validationFailure.getErrorCode(), validationFailure.getMessage());
        return new ErpCraftSyncResult(pending, false, logId,
                validationFailure.getErrorCode().code(), validationFailure.getMessage());
    }

    /**
     * 保存待确认数据并翻译来源工艺唯一键竞争。
     *
     * @param pending 待确认实体
     */
    private void savePending(ErpCraftPendingEntity pending) {
        try {
            pendingRepository.saveAndFlush(pending);
        } catch (org.springframework.dao.DataIntegrityViolationException exception) {
            if (containsConstraint(exception, ERP_CRAFT_UNIQUE_CONSTRAINT)) {
                throw new ServiceException(IntegrationErrorCodeConstants.ERP_CRAFT_DUPLICATE);
            }
            throw exception;
        }
    }

    /**
     * 判断数据库异常因果链是否包含指定约束名。
     *
     * @param exception      数据完整性异常
     * @param constraintName 约束名
     * @return true 表示命中指定约束
     */
    private boolean containsConstraint(Throwable exception, String constraintName) {
        String expectedName = constraintName.toLowerCase(Locale.ROOT);
        Throwable currentCause = exception;
        while (currentCause != null) {
            String message = currentCause.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains(expectedName)) {
                return true;
            }
            currentCause = currentCause.getCause();
        }
        return false;
    }

    /**
     * 校验产品编码在 MES 中存在且启用。
     *
     * @param productCode 产品编码
     */
    private void validateProductExists(String productCode) {
        ProductEntity product = productRepository
                .findByProductCodeAndDeletedFalse(normalizeCode(productCode))
                .orElseThrow(() -> new ServiceException(
                        IntegrationErrorCodeConstants.ERP_PRODUCT_NOT_AVAILABLE));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(product.getStatus())) {
            throw new ServiceException(IntegrationErrorCodeConstants.ERP_PRODUCT_NOT_AVAILABLE);
        }
    }

    /**
     * 校验所有工序编码在 MES 中存在且启用。
     *
     * @param steps 工序步骤
     */
    private void validateProcessCodes(List<ErpCraftStepDTO> steps) {
        for (ErpCraftStepDTO step : steps) {
            CraftProcessEntity process = processRepository
                    .findByProcessCodeAndDeletedFalse(normalizeCode(step.processCode()))
                    .orElseThrow(() -> new ServiceException(
                            IntegrationErrorCodeConstants.ERP_CRAFT_PROCESS_NOT_AVAILABLE));
            if (!CommonStatusEnum.ENABLED.getStatus().equals(process.getStatus())) {
                throw new ServiceException(
                        IntegrationErrorCodeConstants.ERP_CRAFT_PROCESS_NOT_AVAILABLE);
            }
        }
    }

    /**
     * 校验工序顺序号从 1 开始连续且不重复。
     *
     * @param steps 工序步骤
     */
    private void validateSequence(List<ErpCraftStepDTO> steps) {
        Set<Integer> seen = new HashSet<>();
        for (int index = 0; index < steps.size(); index++) {
            Integer sequenceNo = steps.get(index).sequenceNo();
            if (!seen.add(sequenceNo) || sequenceNo != index + 1) {
                throw new ServiceException(
                        IntegrationErrorCodeConstants.ERP_CRAFT_SEQUENCE_INVALID);
            }
        }
    }

    /**
     * 填充待确认实体公共字段。
     *
     * @param pending      待确认实体
     * @param craft        ERP 工艺数据
     * @param sourceSystem 来源系统
     */
    private void fillPendingFields(ErpCraftPendingEntity pending, ErpCraftDTO craft,
                                   String sourceSystem) {
        pending.setSourceSystem(sourceSystem);
        pending.setErpRoutingCode(craft.erpRoutingCode().trim());
        pending.setErpRoutingName(craft.erpRoutingName().trim());
        pending.setErpRoutingVersion(craft.erpRoutingVersion().trim());
        pending.setProductCode(normalizeCode(craft.productCode()));
    }

    /**
     * 清除旧的异常和确认字段。
     *
     * @param pending 待确认实体
     */
    private void clearErrorFields(ErpCraftPendingEntity pending) {
        pending.setErrorCode(null);
        pending.setErrorMessage(null);
        pending.setConfirmedRouteId(null);
        pending.setConfirmedBy(null);
        pending.setConfirmTime(null);
    }

    /**
     * 序列化工序步骤为 JSON。
     *
     * @param steps 工序步骤
     * @return JSON 字符串
     */
    private String serializeSteps(List<ErpCraftStepDTO> steps) {
        return objectMapper.writeValueAsString(steps);
    }

    /**
     * 反序列化工序步骤 JSON。
     *
     * @param json JSON 字符串
     * @return 工序步骤列表
     */
    private List<ErpCraftStepDTO> deserializeSteps(String json) {
        return objectMapper.readValue(json, new TypeReference<List<ErpCraftStepDTO>>() {
        });
    }

    /**
     * 构建工艺路线创建请求，sourceType=2 表示 ERP 读取确认。
     *
     * @param pending 待确认实体
     * @param steps   工序步骤
     * @return 工艺路线创建请求
     */
    private CraftRouteSaveReqVO buildRouteSaveReqVO(ErpCraftPendingEntity pending,
                                                     List<ErpCraftStepDTO> steps) {
        Long productId = resolveProductId(pending.getProductCode());
        CraftRouteSaveReqVO reqVO = new CraftRouteSaveReqVO();
        reqVO.setRoutingCode(pending.getErpRoutingCode());
        reqVO.setRoutingName(pending.getErpRoutingName());
        reqVO.setRoutingVersion(pending.getErpRoutingVersion());
        reqVO.setSourceType(2);
        reqVO.setProductIds(List.of(productId));
        reqVO.setSteps(buildRouteSteps(steps));
        reqVO.setChangeReason(CONFIRM_REASON);
        return reqVO;
    }

    /**
     * 构建工艺路线步骤列表，按工序编码查找工序主键。
     *
     * @param steps ERP 工序步骤
     * @return 工艺路线步骤请求列表
     */
    private List<CraftRouteStepSaveReqVO> buildRouteSteps(List<ErpCraftStepDTO> steps) {
        List<CraftRouteStepSaveReqVO> routeSteps = new ArrayList<>(steps.size());
        for (ErpCraftStepDTO step : steps) {
            CraftProcessEntity process = processRepository
                    .findByProcessCodeAndDeletedFalse(normalizeCode(step.processCode()))
                    .orElseThrow(() -> new ServiceException(
                            IntegrationErrorCodeConstants.ERP_CRAFT_PROCESS_NOT_AVAILABLE));
            CraftRouteStepSaveReqVO routeStep = new CraftRouteStepSaveReqVO();
            routeStep.setSequenceNo(step.sequenceNo());
            routeStep.setProcessId(process.getId());
            routeStep.setInspectNode(Boolean.TRUE.equals(process.getQualityRequired()));
            routeSteps.add(routeStep);
        }
        return routeSteps;
    }

    /**
     * 按产品编码查找启用产品主键。
     *
     * @param productCode 产品编码
     * @return 产品主键
     */
    private Long resolveProductId(String productCode) {
        ProductEntity product = productRepository
                .findByProductCodeAndDeletedFalse(productCode)
                .orElseThrow(() -> new ServiceException(
                        IntegrationErrorCodeConstants.ERP_PRODUCT_NOT_AVAILABLE));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(product.getStatus())) {
            throw new ServiceException(IntegrationErrorCodeConstants.ERP_PRODUCT_NOT_AVAILABLE);
        }
        return product.getId();
    }

    /**
     * 规范化 ASCII 编码。
     *
     * @param value 原始编码
     * @return 去空格大写编码
     */
    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
