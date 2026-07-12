package com.badminton.mes.module.integration.service;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.craft.dal.entity.CraftRouteEntity;
import com.badminton.mes.module.craft.dal.repository.CraftRouteProductRepository;
import com.badminton.mes.module.craft.dal.repository.CraftRouteRepository;
import com.badminton.mes.module.craft.enums.CraftRouteStatusEnum;
import com.badminton.mes.module.integration.constants.IntegrationErrorCodeConstants;
import com.badminton.mes.module.integration.controller.vo.ExternalWorkOrderWriteReqVO;
import com.badminton.mes.module.integration.controller.vo.UnitWriteReqVO;
import com.badminton.mes.module.integration.dal.entity.UnitEntity;
import com.badminton.mes.module.integration.dal.repository.UnitRepository;
import com.badminton.mes.module.integration.enums.IntegrationInterfaceTypeEnum;
import com.badminton.mes.module.integration.enums.IntegrationWriteStatusEnum;
import com.badminton.mes.module.integration.service.dto.IntegrationCommandResult;
import com.badminton.mes.module.production.dal.entity.BomEntity;
import com.badminton.mes.module.production.dal.entity.ProductEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;
import com.badminton.mes.module.production.dal.entity.WorkshopEntity;
import com.badminton.mes.module.production.dal.redis.WorkOrderNoSequence;
import com.badminton.mes.module.production.dal.repository.BomRepository;
import com.badminton.mes.module.production.dal.repository.ProductRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.production.dal.repository.WorkshopRepository;
import com.badminton.mes.module.production.enums.BomStatusEnum;
import com.badminton.mes.module.production.enums.WorkOrderSourceTypeEnum;
import com.badminton.mes.module.production.enums.WorkOrderStatusEnum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 外部写入命令事务服务，保证主数据与成功/重复日志原子提交。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
@Service
public class IntegrationWriteCommandService {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationWriteCommandService.class);

    private static final String UNIT_CODE_CONSTRAINT = "uk_active_unit_code";

    private static final String EXTERNAL_WORK_ORDER_CONSTRAINT = "uk_external_source_order";

    private final UnitRepository unitRepository;

    private final ProductRepository productRepository;

    private final WorkshopRepository workshopRepository;

    private final BomRepository bomRepository;

    private final CraftRouteRepository routeRepository;

    private final CraftRouteProductRepository routeProductRepository;

    private final WorkOrderRepository workOrderRepository;

    private final WorkOrderNoSequence workOrderNoSequence;

    private final IntegrationAuditService auditService;

    /**
     * 构造外部写入命令服务。
     *
     * @param unitRepository         计量单位 Repository
     * @param productRepository      产品 Repository
     * @param workshopRepository     车间 Repository
     * @param bomRepository          BOM Repository
     * @param routeRepository        工艺路线 Repository
     * @param routeProductRepository 路线产品关系 Repository
     * @param workOrderRepository    生产工单 Repository
     * @param workOrderNoSequence    工单号流水
     * @param auditService           接口审计服务
     */
    public IntegrationWriteCommandService(UnitRepository unitRepository,
                                          ProductRepository productRepository,
                                          WorkshopRepository workshopRepository,
                                          BomRepository bomRepository,
                                          CraftRouteRepository routeRepository,
                                          CraftRouteProductRepository routeProductRepository,
                                          WorkOrderRepository workOrderRepository,
                                          WorkOrderNoSequence workOrderNoSequence,
                                          IntegrationAuditService auditService) {
        this.unitRepository = unitRepository;
        this.productRepository = productRepository;
        this.workshopRepository = workshopRepository;
        this.bomRepository = bomRepository;
        this.routeRepository = routeRepository;
        this.routeProductRepository = routeProductRepository;
        this.workOrderRepository = workOrderRepository;
        this.workOrderNoSequence = workOrderNoSequence;
        this.auditService = auditService;
    }

    /**
     * 新增或更新计量单位。
     *
     * @param reqVO    单位写入请求
     * @param snapshot 原始请求快照
     * @return 命令结果
     */
    @Transactional(rollbackFor = Exception.class)
    public IntegrationCommandResult writeUnit(UnitWriteReqVO reqVO, String snapshot) {
        String sourceSystem = normalizeCode(reqVO.getSourceSystem());
        String unitCode = normalizeCode(reqVO.getUnitCode());
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        UnitEntity unit = unitRepository.findByUnitCodeForUpdate(unitCode).orElse(null);
        if (unit == null) {
            unit = new UnitEntity();
            unit.setUnitCode(unitCode);
            unit.setCreateBy(operatorId);
        } else if (!Objects.equals(unit.getDecimalPrecision(), reqVO.getDecimalPrecision())
                && productRepository.existsByUnitIdAndDeletedFalse(unit.getId())) {
            throw new ServiceException(IntegrationErrorCodeConstants.UNIT_PRECISION_IN_USE);
        }

        unit.setUnitName(reqVO.getUnitName().trim());
        unit.setDecimalPrecision(reqVO.getDecimalPrecision());
        unit.setStatus(reqVO.getStatus());
        unit.setUpdateBy(operatorId);
        saveUnit(unit);
        Long logId = auditService.recordResult(
                IntegrationInterfaceTypeEnum.UNIT_WRITE,
                sourceSystem,
                unitCode,
                snapshot,
                IntegrationWriteStatusEnum.SUCCESS,
                unit.getId(),
                unitCode);
        return new IntegrationCommandResult(unit.getId(), unitCode, false, logId);
    }

    /**
     * 幂等写入外部生产工单。
     *
     * @param reqVO    外部工单请求
     * @param snapshot 原始请求快照
     * @return 命令结果
     */
    @Transactional(rollbackFor = Exception.class)
    public IntegrationCommandResult writeWorkOrder(ExternalWorkOrderWriteReqVO reqVO,
                                                    String snapshot) {
        String sourceSystem = normalizeCode(reqVO.getSourceSystem());
        String externalNo = reqVO.getExternalWorkOrderNo().trim();
        Optional<WorkOrderEntity> existing = findExternalWorkOrder(sourceSystem, externalNo);
        if (existing.isPresent()) {
            WorkOrderEntity workOrder = existing.get();
            Long logId = auditService.recordResult(
                    IntegrationInterfaceTypeEnum.WORK_ORDER_WRITE,
                    sourceSystem,
                    externalNo,
                    snapshot,
                    IntegrationWriteStatusEnum.DUPLICATE,
                    workOrder.getId(),
                    workOrder.getWorkOrderNo());
            return new IntegrationCommandResult(
                    workOrder.getId(), workOrder.getWorkOrderNo(), true, logId);
        }

        validatePlanTime(reqVO);
        ProductEntity product = requireProduct(reqVO.getProductCode());
        validateProductUnit(product.getUnitId());
        WorkshopEntity workshop = requireWorkshop(reqVO.getWorkshopCode());
        validateBom(reqVO.getBomId(), product.getId());
        validateRoute(reqVO.getRoutingId(), product.getId());

        WorkOrderEntity workOrder = buildWorkOrder(reqVO, sourceSystem, externalNo,
                product, workshop);
        saveWorkOrder(workOrder);
        Long logId = auditService.recordResult(
                IntegrationInterfaceTypeEnum.WORK_ORDER_WRITE,
                sourceSystem,
                externalNo,
                snapshot,
                IntegrationWriteStatusEnum.SUCCESS,
                workOrder.getId(),
                workOrder.getWorkOrderNo());
        return new IntegrationCommandResult(
                workOrder.getId(), workOrder.getWorkOrderNo(), false, logId);
    }

    /**
     * 查询已处理的外部工单，包含逻辑删除历史，保证幂等键永久有效。
     *
     * @param sourceSystem 来源系统
     * @param externalNo   外部工单号
     * @return 已生成的工单
     */
    @Transactional(readOnly = true)
    public Optional<WorkOrderEntity> findExternalWorkOrder(String sourceSystem, String externalNo) {
        return workOrderRepository.findBySourceTypeAndSourceSystemAndSourceOrderNo(
                WorkOrderSourceTypeEnum.API_WRITE.getType(), sourceSystem, externalNo);
    }

    /**
     * 查询未删除计量单位。
     *
     * @param unitCode 单位编码
     * @return 计量单位
     */
    @Transactional(readOnly = true)
    public Optional<UnitEntity> findUnit(String unitCode) {
        return unitRepository.findByUnitCodeAndDeletedFalse(normalizeCode(unitCode));
    }

    /**
     * 保存单位并翻译可识别的唯一约束冲突。
     *
     * @param unit 计量单位
     */
    private void saveUnit(UnitEntity unit) {
        try {
            unitRepository.saveAndFlush(unit);
        } catch (DataIntegrityViolationException exception) {
            if (containsConstraint(exception, UNIT_CODE_CONSTRAINT)) {
                throw new ServiceException(IntegrationErrorCodeConstants.UNIT_CODE_DUPLICATE);
            }
            logger.error("[计量单位接口写入冲突] unitCode: {}, errorMessage: {}",
                    unit.getUnitCode(), exception.getMessage(), exception);
            throw new ServiceException(IntegrationErrorCodeConstants.WRITE_CONFLICT);
        }
    }

    /**
     * 保存外部工单并翻译来源幂等唯一约束冲突。
     *
     * @param workOrder 生产工单
     */
    private void saveWorkOrder(WorkOrderEntity workOrder) {
        try {
            workOrderRepository.saveAndFlush(workOrder);
        } catch (DataIntegrityViolationException exception) {
            if (containsConstraint(exception, EXTERNAL_WORK_ORDER_CONSTRAINT)) {
                throw new ServiceException(
                        IntegrationErrorCodeConstants.EXTERNAL_WORK_ORDER_DUPLICATE);
            }
            logger.error("[外部工单接口写入冲突] sourceSystem: {}, sourceOrderNo: {}, errorMessage: {}",
                    workOrder.getSourceSystem(), workOrder.getSourceOrderNo(),
                    exception.getMessage(), exception);
            throw new ServiceException(IntegrationErrorCodeConstants.WRITE_CONFLICT);
        }
    }

    /**
     * 校验计划时间顺序。
     *
     * @param reqVO 外部工单请求
     */
    private void validatePlanTime(ExternalWorkOrderWriteReqVO reqVO) {
        if (reqVO.getPlanEndTime().isBefore(reqVO.getPlanStartTime())) {
            throw new ServiceException(
                    IntegrationErrorCodeConstants.EXTERNAL_WORK_ORDER_PLAN_TIME_INVALID);
        }
    }

    /**
     * 查询启用产品。
     *
     * @param productCode 产品编码
     * @return 产品实体
     */
    private ProductEntity requireProduct(String productCode) {
        ProductEntity product = productRepository
                .findByProductCodeAndDeletedFalse(normalizeCode(productCode))
                .orElseThrow(() -> new ServiceException(
                        IntegrationErrorCodeConstants.EXTERNAL_PRODUCT_NOT_AVAILABLE));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(product.getStatus())) {
            throw new ServiceException(IntegrationErrorCodeConstants.EXTERNAL_PRODUCT_NOT_AVAILABLE);
        }
        return product;
    }

    /**
     * 校验产品引用的计量单位存在且启用。
     *
     * @param unitId 产品计量单位主键
     */
    private void validateProductUnit(Long unitId) {
        UnitEntity unit = unitRepository.findByIdAndDeletedFalse(unitId)
                .orElseThrow(() -> new ServiceException(
                        IntegrationErrorCodeConstants.UNIT_NOT_EXISTS));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(unit.getStatus())) {
            throw new ServiceException(IntegrationErrorCodeConstants.UNIT_NOT_EXISTS);
        }
    }

    /**
     * 查询启用车间。
     *
     * @param workshopCode 车间编码
     * @return 车间实体
     */
    private WorkshopEntity requireWorkshop(String workshopCode) {
        WorkshopEntity workshop = workshopRepository
                .findByWorkshopCodeAndDeletedFalseForUpdate(normalizeCode(workshopCode))
                .orElseThrow(() -> new ServiceException(
                        IntegrationErrorCodeConstants.EXTERNAL_WORKSHOP_NOT_AVAILABLE));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(workshop.getStatus())) {
            throw new ServiceException(IntegrationErrorCodeConstants.EXTERNAL_WORKSHOP_NOT_AVAILABLE);
        }
        return workshop;
    }

    /**
     * 校验 BOM 已生效且属于当前产品。
     *
     * @param bomId     BOM 主键
     * @param productId 产品主键
     */
    private void validateBom(Long bomId, Long productId) {
        BomEntity bom = bomRepository.findByIdAndDeletedFalse(bomId)
                .orElseThrow(() -> new ServiceException(
                        IntegrationErrorCodeConstants.EXTERNAL_BOM_NOT_AVAILABLE));
        boolean available = BomStatusEnum.EFFECTIVE.getStatus().equals(bom.getBomStatus())
                && productId.equals(bom.getProductId());
        if (!available) {
            throw new ServiceException(IntegrationErrorCodeConstants.EXTERNAL_BOM_NOT_AVAILABLE);
        }
    }

    /**
     * 校验工艺路线已生效且绑定当前产品。
     *
     * @param routeId   路线主键
     * @param productId 产品主键
     */
    private void validateRoute(Long routeId, Long productId) {
        CraftRouteEntity route = routeRepository.findByIdAndDeletedFalse(routeId)
                .orElseThrow(() -> new ServiceException(
                        IntegrationErrorCodeConstants.EXTERNAL_ROUTE_NOT_AVAILABLE));
        boolean available = CraftRouteStatusEnum.EFFECTIVE.getStatus()
                .equals(route.getRoutingStatus())
                && routeProductRepository.existsByRouteIdAndProductIdAndDeletedFalse(
                        routeId, productId);
        if (!available) {
            throw new ServiceException(IntegrationErrorCodeConstants.EXTERNAL_ROUTE_NOT_AVAILABLE);
        }
    }

    /**
     * 构造外部工单实体，冗余产品字段只取主档数据。
     *
     * @param reqVO        外部工单请求
     * @param sourceSystem 来源系统
     * @param externalNo   外部工单号
     * @param product      产品实体
     * @param workshop     车间实体
     * @return 待保存工单
     */
    private WorkOrderEntity buildWorkOrder(ExternalWorkOrderWriteReqVO reqVO,
                                           String sourceSystem,
                                           String externalNo,
                                           ProductEntity product,
                                           WorkshopEntity workshop) {
        WorkOrderEntity workOrder = new WorkOrderEntity();
        workOrder.setWorkOrderNo(workOrderNoSequence.nextNo());
        workOrder.setSourceType(WorkOrderSourceTypeEnum.API_WRITE.getType());
        workOrder.setSourceSystem(sourceSystem);
        workOrder.setSourceOrderNo(externalNo);
        workOrder.setProductId(product.getId());
        workOrder.setProductName(product.getProductName());
        workOrder.setSpec(product.getSpec());
        workOrder.setUnitId(product.getUnitId());
        workOrder.setBatchNo(trimToNull(reqVO.getBatchNo()));
        workOrder.setBomId(reqVO.getBomId());
        workOrder.setRoutingId(reqVO.getRoutingId());
        workOrder.setCustomerId(reqVO.getCustomerId());
        workOrder.setWorkshopId(workshop.getId());
        workOrder.setPlanQuantity(reqVO.getPlanQuantity());
        workOrder.setOverRatio(reqVO.getOverRatio());
        workOrder.setPriority(reqVO.getPriority());
        workOrder.setPlanStartTime(reqVO.getPlanStartTime());
        workOrder.setPlanEndTime(reqVO.getPlanEndTime());
        workOrder.setOrderStatus(WorkOrderStatusEnum.CREATED.getStatus());
        workOrder.setCreateBy(SecurityContextHolder.getRequiredLoginUserId());
        return workOrder;
    }

    /**
     * 判断异常因果链是否包含指定数据库约束名。
     *
     * @param exception      数据完整性异常
     * @param constraintName 约束名
     * @return true 表示命中该约束
     */
    private boolean containsConstraint(DataIntegrityViolationException exception,
                                       String constraintName) {
        Throwable cause = exception;
        String expected = constraintName.toLowerCase(Locale.ROOT);
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains(expected)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
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

    /**
     * 字符串去空格，空白转 null。
     *
     * @param value 原始字符串
     * @return 规范化字符串
     */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
