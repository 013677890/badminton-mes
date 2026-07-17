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

    /** ERP 待确认数据转为 MES 草稿时写入版本变更记录的固定原因。 */
    private static final String CONFIRM_REASON = "ERP 工艺数据确认";

    /** 来源系统、ERP 路线编码和版本的组合唯一索引，用于兜底并发同步。 */
    private static final String ERP_CRAFT_UNIQUE_CONSTRAINT = "uk_source_code_version";

    /** ERP 工艺暂存仓储，负责幂等查询、确认锁定以及异常数据保留。 */
    private final ErpCraftPendingRepository pendingRepository;

    /** 产品仓储，用于同步校验并在确认时将产品编码解析为 MES 主键。 */
    private final ProductRepository productRepository;

    /** 工序仓储，用于验证每个 ERP 工序编码并构造路线步骤主键。 */
    private final CraftProcessRepository processRepository;

    /** 工艺路线领域服务，确认时通过其创建草稿以复用版本和子项校验规则。 */
    private final CraftRouteService craftRouteService;

    /** 接口审计服务，记录同步成功、重复及保留异常数据的处理结果。 */
    private final IntegrationAuditService auditService;

    /** 工序步骤 JSON 序列化器，用于暂存原始步骤并在确认时恢复。 */
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
        // 先检查头部必填结构，避免后续 trim 或业务键拼接因空值中断整个批次。
        validateCraftStructure(craft);
        String routingCode = craft.erpRoutingCode().trim();
        String routingVersion = craft.erpRoutingVersion().trim();
        String businessKey = routingCode + ":" + routingVersion;

        // 来源系统、路线编码和版本共同构成 ERP 工艺在 MES 侧的永久幂等身份。
        Optional<ErpCraftPendingEntity> existing = pendingRepository
                .findBySourceSystemAndErpRoutingCodeAndErpRoutingVersion(
                        sourceSystem, routingCode, routingVersion);
        if (existing.isPresent()
                && !ErpCraftPendingStatusEnum.FAILED.getStatus().equals(existing.get().getStatus())
                && !ErpCraftPendingStatusEnum.REJECTED.getStatus().equals(existing.get().getStatus())) {
            // 待确认或已确认数据不可被同步覆盖，只记录重复调用并保留首次暂存内容。
            Long logId = auditService.recordResult(
                    IntegrationInterfaceTypeEnum.ERP_CRAFT_SYNC,
                    sourceSystem, businessKey, snapshot,
                    IntegrationWriteStatusEnum.DUPLICATE,
                    existing.get().getId(), businessKey);
            return new ErpCraftSyncResult(null, true, logId, null, null);
        }

        // 失败或已驳回数据允许上游修正后原位覆盖，避免唯一键阻止再次同步。
        ErpCraftPendingEntity pending = existing.orElseGet(ErpCraftPendingEntity::new);
        fillPendingFields(pending, craft, sourceSystem);
        if (pending.getId() == null) {
            pending.setCreateBy(SecurityContextHolder.getRequiredLoginUserId());
        }
        // 重试前清除上一次失败或确认痕迹，后续按本次校验结果重新写入状态。
        clearErrorFields(pending);

        ServiceException validationFailure = null;
        try {
            // 排序后的步骤同时用于暂存和校验，保证确认创建时遵循同一顺序契约。
            List<ErpCraftStepDTO> sortedSteps = sortAndValidateSteps(craft.steps());
            pending.setProcessSteps(serializeSteps(sortedSteps));
            validateCraft(craft, sortedSteps);
        } catch (ServiceException exception) {
            validationFailure = exception;
        }
        if (validationFailure != null) {
            // 业务校验失败不回滚暂存数据：保留异常快照供人工查看和上游修正。
            return saveFailedPending(
                    pending, craft.steps(), sourceSystem, businessKey, snapshot, validationFailure);
        }

        // 全部主档与顺序校验通过后进入待确认区，但不会直接发布为生效路线。
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
        // 悲观锁定待确认记录，阻止两名审核人同时确认并各自生成一条路线草稿。
        ErpCraftPendingEntity pending = pendingRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ServiceException(
                        IntegrationErrorCodeConstants.ERP_CRAFT_PENDING_NOT_EXISTS));
        if (!ErpCraftPendingStatusEnum.PENDING.getStatus().equals(pending.getStatus())) {
            throw new ServiceException(
                    IntegrationErrorCodeConstants.ERP_CRAFT_PENDING_STATUS_INVALID);
        }

        // 从暂存快照恢复步骤，再重新解析当前 MES 主档主键，避免持久化跨模块实体引用。
        List<ErpCraftStepDTO> steps = deserializeSteps(pending.getProcessSteps());
        CraftRouteSaveReqVO routeReqVO = buildRouteSaveReqVO(pending, steps);
        // 必须调用工艺模块服务创建草稿，不能直接写路线表绕过其版本与子项规则。
        Long routeId = craftRouteService.createRoute(routeReqVO);

        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        // 路线草稿与确认状态在同一事务提交，任一保存失败都不会留下单边结果。
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
     * 驳回一条待确认 ERP 工艺。
     *
     * @param id 待确认数据主键
     * @param reason 驳回原因
     */
    @Transactional(rollbackFor = Exception.class)
    public void rejectCraft(Long id, String reason) {
        // 与确认使用相同写锁，保证驳回和确认不能基于同一个 PENDING 状态并发成功。
        ErpCraftPendingEntity pending = pendingRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ServiceException(
                        IntegrationErrorCodeConstants.ERP_CRAFT_PENDING_NOT_EXISTS));
        if (!ErpCraftPendingStatusEnum.PENDING.getStatus().equals(pending.getStatus())) {
            throw new ServiceException(
                    IntegrationErrorCodeConstants.ERP_CRAFT_PENDING_STATUS_INVALID);
        }
        pending.setStatus(ErpCraftPendingStatusEnum.REJECTED.getStatus());
        pending.setErrorCode("A0440");
        pending.setErrorMessage(reason.trim());
        pendingRepository.save(pending);
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
        // 排序后再校验从 1 连续递增，可同时识别缺号、重复号和非 1 起始。
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
            // 结构校验在排序前失败时仍保存可序列化的原始步骤，便于定位 ERP 数据问题。
            List<ErpCraftStepDTO> storableSteps = originalSteps == null ? List.of() : originalSteps;
            pending.setProcessSteps(serializeSteps(storableSteps));
        }
        pending.setStatus(ErpCraftPendingStatusEnum.FAILED.getStatus());
        pending.setErrorCode(validationFailure.getErrorCode().code());
        pending.setErrorMessage(validationFailure.getMessage());
        // 异常暂存记录和 FAILED 审计共同加入当前事务，确保排障数据完整。
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
            // 每个步骤均以规范化编码解析当前有效工序，停用工序也不能进入新路线草稿。
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
        // 原位重试必须同时清理错误和历史确认字段，防止新状态携带旧处理结果。
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
        // ERP 编码和版本原样成为 MES 草稿身份，产品编码则转换为内部主键关系。
        reqVO.setRoutingCode(pending.getErpRoutingCode());
        reqVO.setRoutingName(pending.getErpRoutingName());
        reqVO.setRoutingVersion(pending.getErpRoutingVersion());
        // 来源类型 2 明确标识该草稿由 ERP 读取确认产生，便于后续追溯来源。
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
            // 确认时重新查询工序，防止同步后至确认前工序被删除或失效而继续生成路线。
            CraftProcessEntity process = processRepository
                    .findByProcessCodeAndDeletedFalse(normalizeCode(step.processCode()))
                    .orElseThrow(() -> new ServiceException(
                            IntegrationErrorCodeConstants.ERP_CRAFT_PROCESS_NOT_AVAILABLE));
            CraftRouteStepSaveReqVO routeStep = new CraftRouteStepSaveReqVO();
            routeStep.setSequenceNo(step.sequenceNo());
            routeStep.setProcessId(process.getId());
            // 工序的质量必检配置映射为路线检验节点，不接受 ERP 数据自行指定。
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
