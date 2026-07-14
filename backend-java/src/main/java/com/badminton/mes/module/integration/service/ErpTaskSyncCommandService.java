package com.badminton.mes.module.integration.service;

import java.util.Locale;
import java.util.Optional;

import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.integration.constants.IntegrationErrorCodeConstants;
import com.badminton.mes.module.integration.enums.IntegrationInterfaceTypeEnum;
import com.badminton.mes.module.integration.enums.IntegrationWriteStatusEnum;
import com.badminton.mes.module.integration.service.dto.ErpTaskDTO;
import com.badminton.mes.module.integration.service.dto.IntegrationCommandResult;
import com.badminton.mes.module.production.dal.entity.ProductEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;
import com.badminton.mes.module.production.dal.entity.WorkshopEntity;
import com.badminton.mes.module.production.dal.redis.WorkOrderNoSequence;
import com.badminton.mes.module.production.dal.repository.ProductRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.production.dal.repository.WorkshopRepository;
import com.badminton.mes.module.production.enums.WorkOrderSourceTypeEnum;
import com.badminton.mes.module.production.enums.WorkOrderStatusEnum;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * ERP 生产任务单同步命令服务，负责单条任务的幂等判断、校验和工单生成。
 *
 * <p>每条任务在独立事务中处理：成功/重复日志与工单原子提交，
 * 失败由门面层用独立事务记录。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Service
public class ErpTaskSyncCommandService {

    private static final String EXTERNAL_WORK_ORDER_CONSTRAINT = "uk_external_source_order";

    private final WorkOrderRepository workOrderRepository;

    private final ProductRepository productRepository;

    private final WorkshopRepository workshopRepository;

    private final WorkOrderNoSequence workOrderNoSequence;

    private final IntegrationAuditService auditService;

    /**
     * 构造 ERP 任务同步命令服务。
     *
     * @param workOrderRepository  生产工单 Repository
     * @param productRepository    产品 Repository
     * @param workshopRepository   车间 Repository
     * @param workOrderNoSequence  工单号流水生成器
     * @param auditService         接口审计服务
     */
    public ErpTaskSyncCommandService(WorkOrderRepository workOrderRepository,
                                     ProductRepository productRepository,
                                     WorkshopRepository workshopRepository,
                                     WorkOrderNoSequence workOrderNoSequence,
                                     IntegrationAuditService auditService) {
        this.workOrderRepository = workOrderRepository;
        this.productRepository = productRepository;
        this.workshopRepository = workshopRepository;
        this.workOrderNoSequence = workOrderNoSequence;
        this.auditService = auditService;
    }

    /**
     * 同步单条 ERP 生产任务单，幂等生成 MES 生产工单。
     *
     * @param task         ERP 任务数据
     * @param snapshot     请求快照
     * @param sourceSystem 来源系统
     * @return 命令结果
     */
    @Transactional(rollbackFor = Exception.class)
    public IntegrationCommandResult syncTask(ErpTaskDTO task, String snapshot, String sourceSystem) {
        validateTaskStructure(task);
        String erpOrderNo = task.erpOrderNo().trim();
        Optional<WorkOrderEntity> existing = workOrderRepository
                .findBySourceTypeAndSourceSystemAndSourceOrderNo(
                        WorkOrderSourceTypeEnum.ERP_SYNC.getType(), sourceSystem, erpOrderNo);
        if (existing.isPresent()) {
            WorkOrderEntity workOrder = existing.get();
            Long logId = auditService.recordResult(
                    IntegrationInterfaceTypeEnum.ERP_TASK_SYNC,
                    sourceSystem, erpOrderNo, snapshot,
                    IntegrationWriteStatusEnum.DUPLICATE,
                    workOrder.getId(), workOrder.getWorkOrderNo());
            return new IntegrationCommandResult(
                    workOrder.getId(), workOrder.getWorkOrderNo(), true, logId);
        }

        validateTask(task);
        ProductEntity product = requireProduct(task.productCode());
        WorkshopEntity workshop = requireWorkshop(task.workshopCode());

        WorkOrderEntity workOrder = buildWorkOrder(task, sourceSystem, erpOrderNo, product, workshop);
        saveWorkOrder(workOrder);
        Long logId = auditService.recordResult(
                IntegrationInterfaceTypeEnum.ERP_TASK_SYNC,
                sourceSystem, erpOrderNo, snapshot,
                IntegrationWriteStatusEnum.SUCCESS,
                workOrder.getId(), workOrder.getWorkOrderNo());
        return new IntegrationCommandResult(
                workOrder.getId(), workOrder.getWorkOrderNo(), false, logId);
    }

    /**
     * 查询已提交的 ERP 同步工单，供并发唯一键竞争后的门面查询获胜结果。
     *
     * @param sourceSystem 来源系统
     * @param erpOrderNo   ERP 任务单号
     * @return 已生成的生产工单
     */
    @Transactional(readOnly = true)
    public Optional<WorkOrderEntity> findSyncedTask(String sourceSystem, String erpOrderNo) {
        return workOrderRepository.findBySourceTypeAndSourceSystemAndSourceOrderNo(
                WorkOrderSourceTypeEnum.ERP_SYNC.getType(), sourceSystem, erpOrderNo);
    }

    /**
     * 校验同步命令执行所需的来源数据结构，避免单条脏数据触发空指针并中断批次。
     *
     * @param task ERP 任务数据
     */
    private void validateTaskStructure(ErpTaskDTO task) {
        boolean requiredTextMissing = task == null
                || !StringUtils.hasText(task.erpOrderNo())
                || !StringUtils.hasText(task.productCode())
                || !StringUtils.hasText(task.workshopCode());
        boolean requiredTimeMissing = task == null
                || task.planStartTime() == null
                || task.planEndTime() == null;
        if (requiredTextMissing || requiredTimeMissing) {
            throw new ServiceException(IntegrationErrorCodeConstants.ERP_SOURCE_DATA_INVALID);
        }
    }

    /**
     * 校验 ERP 任务单业务规则。
     *
     * @param task ERP 任务数据
     */
    private void validateTask(ErpTaskDTO task) {
        if (task.planQuantity() == null || task.planQuantity() <= 0) {
            throw new ServiceException(IntegrationErrorCodeConstants.ERP_TASK_QUANTITY_INVALID);
        }
        if (task.planEndTime().isBefore(task.planStartTime())) {
            throw new ServiceException(IntegrationErrorCodeConstants.ERP_TASK_PLAN_TIME_INVALID);
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
                        IntegrationErrorCodeConstants.ERP_PRODUCT_NOT_AVAILABLE));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(product.getStatus())) {
            throw new ServiceException(IntegrationErrorCodeConstants.ERP_PRODUCT_NOT_AVAILABLE);
        }
        return product;
    }

    /**
     * 查询启用车间。
     *
     * @param workshopCode 车间编码
     * @return 车间实体
     */
    private WorkshopEntity requireWorkshop(String workshopCode) {
        WorkshopEntity workshop = workshopRepository
                .findByWorkshopCodeAndDeletedFalse(normalizeCode(workshopCode))
                .orElseThrow(() -> new ServiceException(
                        IntegrationErrorCodeConstants.ERP_TASK_WORKSHOP_NOT_AVAILABLE));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(workshop.getStatus())) {
            throw new ServiceException(IntegrationErrorCodeConstants.ERP_TASK_WORKSHOP_NOT_AVAILABLE);
        }
        return workshop;
    }

    /**
     * 构建 ERP 同步生产工单实体，BOM 和工艺路线留空由 PMC 后续维护。
     *
     * @param task         ERP 任务数据
     * @param sourceSystem 来源系统
     * @param erpOrderNo   ERP 任务单号
     * @param product      产品实体
     * @param workshop     车间实体
     * @return 工单实体
     */
    private WorkOrderEntity buildWorkOrder(ErpTaskDTO task, String sourceSystem, String erpOrderNo,
                                           ProductEntity product, WorkshopEntity workshop) {
        WorkOrderEntity workOrder = new WorkOrderEntity();
        workOrder.setWorkOrderNo(workOrderNoSequence.nextNo());
        workOrder.setSourceType(WorkOrderSourceTypeEnum.ERP_SYNC.getType());
        workOrder.setSourceSystem(sourceSystem);
        workOrder.setSourceOrderNo(erpOrderNo);
        workOrder.setProductId(product.getId());
        workOrder.setProductName(product.getProductName());
        workOrder.setSpec(product.getSpec());
        workOrder.setUnitId(product.getUnitId());
        workOrder.setBatchNo(task.batchNo());
        workOrder.setWorkshopId(workshop.getId());
        workOrder.setPlanQuantity(task.planQuantity());
        workOrder.setPlanStartTime(task.planStartTime());
        workOrder.setPlanEndTime(task.planEndTime());
        workOrder.setOrderStatus(WorkOrderStatusEnum.CREATED.getStatus());
        workOrder.setCreateBy(SecurityContextHolder.getRequiredLoginUserId());
        return workOrder;
    }

    /**
     * 保存工单并翻译来源幂等唯一约束冲突。
     *
     * @param workOrder 生产工单
     */
    private void saveWorkOrder(WorkOrderEntity workOrder) {
        try {
            workOrderRepository.saveAndFlush(workOrder);
        } catch (DataIntegrityViolationException exception) {
            Throwable cause = exception;
            String expected = EXTERNAL_WORK_ORDER_CONSTRAINT.toLowerCase(Locale.ROOT);
            while (cause != null) {
                String message = cause.getMessage();
                if (message != null && message.toLowerCase(Locale.ROOT).contains(expected)) {
                    throw new ServiceException(
                            IntegrationErrorCodeConstants.EXTERNAL_WORK_ORDER_DUPLICATE);
                }
                cause = cause.getCause();
            }
            throw exception;
        }
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
