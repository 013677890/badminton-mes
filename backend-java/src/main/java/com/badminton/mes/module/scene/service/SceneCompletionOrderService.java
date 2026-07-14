package com.badminton.mes.module.scene.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.integration.dal.entity.CompletionOrderEntity;
import com.badminton.mes.module.integration.dal.repository.CompletionOrderRepository;
import com.badminton.mes.module.integration.enums.CompletionAuditStatusEnum;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.dal.entity.ProductEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;
import com.badminton.mes.module.production.dal.repository.ProductRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.scene.constants.SceneErrorCodeConstants;
import com.badminton.mes.module.scene.controller.vo.CompletionOrderSaveReqVO;
import com.badminton.mes.module.scene.dal.entity.SceneProcessTaskEntity;
import com.badminton.mes.module.scene.dal.entity.SceneExecutionTaskEntity;
import com.badminton.mes.module.scene.dal.repository.SceneProcessTaskRepository;
import com.badminton.mes.module.scene.dal.repository.SceneExecutionTaskRepository;
import com.badminton.mes.module.scene.dal.repository.SceneWorkReportRepository;
import com.badminton.mes.module.scene.enums.SceneExecutionTaskStatusEnum;
import com.badminton.mes.module.scene.enums.WorkReportAuditStatusEnum;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 生产完工单创建、审核与作废服务。
 *
 * @author Codex
 * @date 2026/07/13
 */
@Service
public class SceneCompletionOrderService {

    private final CompletionOrderRepository completionOrderRepository;
    private final WorkOrderRepository workOrderRepository;
    private final ProductRepository productRepository;
    private final SceneExecutionTaskRepository productionTaskRepository;
    private final SceneProcessTaskRepository processTaskRepository;
    private final SceneWorkReportRepository workReportRepository;

    public SceneCompletionOrderService(CompletionOrderRepository completionOrderRepository,
                                       WorkOrderRepository workOrderRepository,
                                       ProductRepository productRepository,
                                       SceneExecutionTaskRepository productionTaskRepository,
                                       SceneProcessTaskRepository processTaskRepository,
                                       SceneWorkReportRepository workReportRepository) {
        this.completionOrderRepository = completionOrderRepository;
        this.workOrderRepository = workOrderRepository;
        this.productRepository = productRepository;
        this.productionTaskRepository = productionTaskRepository;
        this.processTaskRepository = processTaskRepository;
        this.workReportRepository = workReportRepository;
    }

    /** 创建待审核生产完工单。 */
    @Transactional(rollbackFor = Exception.class)
    public Long createCompletionOrder(CompletionOrderSaveReqVO reqVO) {
        int detailQuantity = reqVO.getGoodQuantity() + reqVO.getDefectQuantity();
        if (detailQuantity != reqVO.getCompletionQuantity()) {
            throw new ServiceException(SceneErrorCodeConstants.COMPLETION_QUANTITY_INVALID);
        }
        SceneExecutionTaskEntity productionTask = requireCompletableTask(
                reqVO.getProductionTaskId());
        if (reqVO.getWorkOrderId() != null
                && !reqVO.getWorkOrderId().equals(productionTask.getWorkOrderId())) {
            throw new ServiceException(
                    SceneErrorCodeConstants.COMPLETION_TASK_WORK_ORDER_MISMATCH);
        }
        WorkOrderEntity workOrder = workOrderRepository
                .findByIdAndDeletedFalse(productionTask.getWorkOrderId())
                .orElseThrow(() -> new ServiceException(
                        ProductionErrorCodeConstants.WORK_ORDER_NOT_EXISTS));
        ProductEntity product = productRepository.findByIdAndDeletedFalse(workOrder.getProductId())
                .orElseThrow(() -> new ServiceException(
                        ProductionErrorCodeConstants.PRODUCT_NOT_EXISTS));
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        CompletionOrderEntity completion = new CompletionOrderEntity();
        completion.setCompletionNo(createCompletionNo());
        completion.setProductionTaskId(productionTask.getId());
        completion.setWorkOrderId(workOrder.getId());
        completion.setWorkOrderNo(workOrder.getWorkOrderNo());
        completion.setProductId(product.getId());
        completion.setProductCode(product.getProductCode());
        completion.setProductName(product.getProductName());
        completion.setBatchNo(reqVO.getBatchNo().trim());
        completion.setCompletionQuantity(reqVO.getCompletionQuantity());
        completion.setGoodQuantity(reqVO.getGoodQuantity());
        completion.setDefectQuantity(reqVO.getDefectQuantity());
        completion.setAuditStatus(CompletionAuditStatusEnum.PENDING.getStatus());
        completion.setCreateBy(operatorId);
        completion.setUpdateBy(operatorId);
        completionOrderRepository.save(completion);
        return completion.getId();
    }

    /** 审核完工单并原子回写工单数量。 */
    @Transactional(rollbackFor = Exception.class)
    public void approveCompletionOrder(Long id, String remark) {
        CompletionOrderEntity completion = requirePending(id);
        SceneExecutionTaskEntity productionTask = requireCompletableTask(
                completion.getProductionTaskId());
        validateReportBalance(productionTask, completion.getCompletionQuantity());
        int updated = workOrderRepository.increaseCompletionQuantity(
                completion.getWorkOrderId(), completion.getCompletionQuantity(),
                completion.getDefectQuantity());
        if (updated == 0) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_FINISH_EXCEED_LIMIT);
        }
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        completion.setAuditStatus(CompletionAuditStatusEnum.APPROVED.getStatus());
        completion.setAuditBy(operatorId);
        completion.setAuditTime(LocalDateTime.now());
        completion.setAuditRemark(remark);
        completion.setUpdateBy(operatorId);
        completionOrderRepository.save(completion);
    }

    /** 作废尚未审核的完工单。 */
    @Transactional(rollbackFor = Exception.class)
    public void voidCompletionOrder(Long id, String remark) {
        CompletionOrderEntity completion = requirePending(id);
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        completion.setAuditStatus(CompletionAuditStatusEnum.VOIDED.getStatus());
        completion.setAuditBy(operatorId);
        completion.setAuditTime(LocalDateTime.now());
        completion.setAuditRemark(remark);
        completion.setUpdateBy(operatorId);
        completionOrderRepository.save(completion);
    }

    private CompletionOrderEntity requirePending(Long id) {
        CompletionOrderEntity completion = completionOrderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ServiceException(
                        SceneErrorCodeConstants.COMPLETION_ORDER_NOT_EXISTS));
        if (!CompletionAuditStatusEnum.PENDING.getStatus().equals(completion.getAuditStatus())) {
            throw new ServiceException(SceneErrorCodeConstants.COMPLETION_STATUS_INVALID);
        }
        return completion;
    }

    private SceneExecutionTaskEntity requireCompletableTask(Long productionTaskId) {
        SceneExecutionTaskEntity task = productionTaskRepository
                .findByIdForUpdate(productionTaskId)
                .orElseThrow(() -> new ServiceException(
                        SceneErrorCodeConstants.PRODUCTION_TASK_NOT_EXISTS));
        if (SceneExecutionTaskStatusEnum.CANCELLED.getStatus().equals(task.getTaskStatus())) {
            throw new ServiceException(SceneErrorCodeConstants.PRODUCTION_TASK_STATUS_INVALID);
        }
        List<SceneProcessTaskEntity> processTasks = processTaskRepository
                .findByProductionTaskIdForUpdate(task.getId());
        boolean allCompleted = !processTasks.isEmpty() && processTasks.stream().allMatch(item ->
                SceneExecutionTaskStatusEnum.COMPLETED.getStatus().equals(item.getTaskStatus()));
        if (!allCompleted) {
            throw new ServiceException(SceneErrorCodeConstants.COMPLETION_PROCESS_NOT_FINISHED);
        }
        return task;
    }

    private void validateReportBalance(
            SceneExecutionTaskEntity productionTask, Integer completionQuantity) {
        List<SceneProcessTaskEntity> processTasks = processTaskRepository
                .findByProductionTaskIdAndDeletedFalseOrderBySequenceNoAsc(productionTask.getId());
        SceneProcessTaskEntity lastProcess = processTasks.get(processTasks.size() - 1);
        BigDecimal approvedReportQuantity = workReportRepository.sumApprovedQuantity(
                productionTask.getId(), lastProcess.getProcessId(),
                WorkReportAuditStatusEnum.APPROVED.getStatus());
        Long approvedCompletionQuantity = completionOrderRepository
                .sumCompletionQuantityByTaskAndStatus(
                        productionTask.getId(), CompletionAuditStatusEnum.APPROVED.getStatus());
        BigDecimal requestedTotal = BigDecimal.valueOf(approvedCompletionQuantity)
                .add(BigDecimal.valueOf(completionQuantity));
        if (requestedTotal.compareTo(approvedReportQuantity) > 0) {
            throw new ServiceException(
                    SceneErrorCodeConstants.COMPLETION_REPORT_QUANTITY_NOT_ENOUGH);
        }
    }

    private String createCompletionNo() {
        String random = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return "CO" + random.substring(0, 30);
    }
}
