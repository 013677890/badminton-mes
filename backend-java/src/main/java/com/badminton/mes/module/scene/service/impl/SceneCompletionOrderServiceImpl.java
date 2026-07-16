package com.badminton.mes.module.scene.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.integration.service.CompletionOrderPublishService;
import com.badminton.mes.module.integration.service.dto.ApprovedCompletionDTO;
import com.badminton.mes.module.production.service.WorkOrderExecutionSummaryService;
import com.badminton.mes.module.scene.constants.SceneErrorCodeConstants;
import com.badminton.mes.module.scene.controller.vo.SceneCompletionAuditReqVO;
import com.badminton.mes.module.scene.controller.vo.SceneCompletionCreateReqVO;
import com.badminton.mes.module.scene.controller.vo.SceneCompletionSaveReqVO;
import com.badminton.mes.module.scene.dal.entity.SceneCompletionOrderEntity;
import com.badminton.mes.module.scene.dal.entity.SceneCompletionSyncRecordEntity;
import com.badminton.mes.module.scene.dal.entity.SceneProductionTaskEntity;
import com.badminton.mes.module.scene.dal.repository.SceneCompletionOrderRepository;
import com.badminton.mes.module.scene.dal.repository.SceneCompletionSyncRecordRepository;
import com.badminton.mes.module.scene.dal.repository.SceneProductionTaskRepository;
import com.badminton.mes.module.scene.service.CompletionSyncClient;
import com.badminton.mes.module.scene.service.CompletionSyncResultService;
import com.badminton.mes.module.scene.service.SceneCompletionOrderService;
import com.badminton.mes.module.scene.service.SceneDataScopeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 完工创建、草稿修改、审核和人工同步实现。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
@Service
public class SceneCompletionOrderServiceImpl implements SceneCompletionOrderService {

    private static final String TARGET_SYSTEM = "ERP";
    private static final int MAX_RETRY_COUNT = 3;
    private static final int FINISH_STATUS_DRAFT = 0;
    private static final int FINISH_STATUS_PENDING_AUDIT = 1;
    private static final int FINISH_STATUS_APPROVED = 2;
    private static final int FINISH_STATUS_REJECTED = 3;
    private static final int SYNC_STATUS_SUCCESS = 1;
    private static final int SYNC_STATUS_FAILED = 2;

    private final SceneCompletionOrderRepository orderRepository;
    private final SceneCompletionSyncRecordRepository syncRecordRepository;
    private final SceneProductionTaskRepository taskRepository;
    private final SceneDataScopeService dataScopeService;
    private final CompletionSyncClient syncClient;
    private final CompletionSyncResultService syncResultService;
    private final WorkOrderExecutionSummaryService workOrderExecutionSummaryService;
    private final CompletionOrderPublishService completionOrderPublishService;

    public SceneCompletionOrderServiceImpl(SceneCompletionOrderRepository orderRepository,
                                           SceneCompletionSyncRecordRepository syncRecordRepository,
                                           SceneProductionTaskRepository taskRepository,
                                           SceneDataScopeService dataScopeService,
                                           CompletionSyncClient syncClient,
                                           CompletionSyncResultService syncResultService,
                                           WorkOrderExecutionSummaryService workOrderExecutionSummaryService,
                                           CompletionOrderPublishService completionOrderPublishService) {
        this.orderRepository = orderRepository;
        this.syncRecordRepository = syncRecordRepository;
        this.taskRepository = taskRepository;
        this.dataScopeService = dataScopeService;
        this.syncClient = syncClient;
        this.syncResultService = syncResultService;
        this.workOrderExecutionSummaryService = workOrderExecutionSummaryService;
        this.completionOrderPublishService = completionOrderPublishService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SceneCompletionCreateReqVO reqVO) {
        SceneProductionTaskEntity task = requireTaskForUpdate(reqVO.getTaskId());
        var existed = orderRepository.findByTaskIdAndDeletedFalse(reqVO.getTaskId());
        if (existed.isPresent()) {
            return existed.get().getId();
        }

        validateFinishQuantity(task, reqVO.getFinishQuantity());
        SceneCompletionOrderEntity order = new SceneCompletionOrderEntity();
        order.setFinishNo(number());
        order.setTaskId(task.getId());
        order.setWorkOrderId(task.getWorkOrderId());
        order.setProductId(task.getProductId());
        order.setBatchNo(task.getBatchNo());
        order.setFinishQuantity(reqVO.getFinishQuantity());
        order.setGoodQuantity(reqVO.getFinishQuantity());
        order.setDefectQuantity(0);
        order.setReworkQuantity(0);
        order.setFinishStatus(FINISH_STATUS_DRAFT);
        order.setSyncStatus(0);
        order.setCreateBy(SecurityContextHolder.getRequiredLoginUserId());
        return orderRepository.saveAndFlush(order).getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SceneCompletionSaveReqVO reqVO) {
        SceneCompletionOrderEntity order = requireOrderForUpdate(id);
        if (!isEditable(order.getFinishStatus())) {
            invalidStatus();
        }

        SceneProductionTaskEntity task = requireTaskForUpdate(order.getTaskId());
        validateFinishQuantity(task, reqVO.getFinishQuantity());
        order.setFinishQuantity(reqVO.getFinishQuantity());
        order.setGoodQuantity(reqVO.getFinishQuantity());
        orderRepository.save(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long id) {
        SceneCompletionOrderEntity order = requireOrderForUpdate(id);
        if (!isEditable(order.getFinishStatus())) {
            invalidStatus();
        }
        requireTask(order.getTaskId());
        order.setFinishStatus(FINISH_STATUS_PENDING_AUDIT);
        orderRepository.save(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, SceneCompletionAuditReqVO reqVO) {
        SceneCompletionOrderEntity order = requireOrderForUpdate(id);
        if (!Integer.valueOf(FINISH_STATUS_PENDING_AUDIT).equals(order.getFinishStatus())) {
            invalidStatus();
        }

        SceneProductionTaskEntity task = requireTaskForUpdate(order.getTaskId());
        order.setAuditBy(SecurityContextHolder.getRequiredLoginUserId());
        order.setAuditTime(LocalDateTime.now());
        order.setAuditRemark(reqVO.getRemark());
        boolean approved = Boolean.TRUE.equals(reqVO.getApproved());
        order.setFinishStatus(approved ? FINISH_STATUS_APPROVED : FINISH_STATUS_REJECTED);
        if (approved) {
            task.setFinishQuantity(task.getFinishQuantity() + order.getFinishQuantity());
            if (task.getFinishQuantity() >= task.getPlanQuantity()) {
                task.setTaskStatus(5);
                task.setActualEndTime(LocalDateTime.now());
            }
            taskRepository.save(task);
            workOrderExecutionSummaryService.addApprovedCompletion(
                    order.getWorkOrderId(), order.getFinishQuantity());
            completionOrderPublishService.publishApproved(toApprovedCompletion(order, task));
        }
        orderRepository.save(order);
    }

    @Override
    public void sync(Long id) {
        SceneCompletionOrderEntity order = requireOrder(id);
        if (!Integer.valueOf(FINISH_STATUS_APPROVED).equals(order.getFinishStatus())) {
            invalidStatus();
        }

        String idempotencyKey = "FINISH:" + order.getFinishNo() + ":" + TARGET_SYSTEM;
        SceneCompletionSyncRecordEntity record = syncRecordRepository
                .findByFinishOrderIdAndTargetSystemAndDeletedFalse(id, TARGET_SYSTEM)
                .orElseGet(() -> newRecord(id, idempotencyKey));
        if (Integer.valueOf(SYNC_STATUS_SUCCESS).equals(record.getSyncStatus())) {
            return;
        }
        if (record.getRetryCount() >= MAX_RETRY_COUNT) {
            throw new ServiceException(SceneErrorCodeConstants.COMPLETION_SYNC_RETRY_EXCEEDED);
        }

        try {
            syncClient.sync(order, TARGET_SYSTEM, idempotencyKey);
        } catch (RuntimeException exception) {
            syncResultService.saveResult(order, record, SYNC_STATUS_FAILED, summary(exception));
            throw new ServiceException(SceneErrorCodeConstants.COMPLETION_SYNC_FAILED);
        }
        syncResultService.saveResult(order, record, SYNC_STATUS_SUCCESS, null);
    }

    private SceneCompletionOrderEntity requireOrder(Long id) {
        SceneCompletionOrderEntity order = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(SceneErrorCodeConstants.COMPLETION_NOT_EXISTS));
        requireTask(order.getTaskId());
        return order;
    }

    private SceneCompletionOrderEntity requireOrderForUpdate(Long id) {
        return orderRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(SceneErrorCodeConstants.COMPLETION_NOT_EXISTS));
    }

    private SceneProductionTaskEntity requireTask(Long id) {
        SceneProductionTaskEntity task = taskRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(SceneErrorCodeConstants.TASK_NOT_EXISTS));
        dataScopeService.check(task.getWorkshopId(), task.getLineId());
        return task;
    }

    private SceneProductionTaskEntity requireTaskForUpdate(Long id) {
        SceneProductionTaskEntity task = taskRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(SceneErrorCodeConstants.TASK_NOT_EXISTS));
        dataScopeService.check(task.getWorkshopId(), task.getLineId());
        return task;
    }

    private void validateFinishQuantity(SceneProductionTaskEntity task, Integer finishQuantity) {
        int availableQuantity = task.getGoodQuantity() - task.getFinishQuantity();
        if (finishQuantity == null || finishQuantity <= 0 || finishQuantity > availableQuantity) {
            throw new ServiceException(SceneErrorCodeConstants.COMPLETION_QUANTITY_INVALID);
        }
    }

    private boolean isEditable(Integer finishStatus) {
        return Integer.valueOf(FINISH_STATUS_DRAFT).equals(finishStatus)
                || Integer.valueOf(FINISH_STATUS_REJECTED).equals(finishStatus);
    }

    private SceneCompletionSyncRecordEntity newRecord(Long orderId, String idempotencyKey) {
        SceneCompletionSyncRecordEntity record = new SceneCompletionSyncRecordEntity();
        record.setFinishOrderId(orderId);
        record.setTargetSystem(TARGET_SYSTEM);
        record.setIdempotencyKey(idempotencyKey);
        record.setSyncStatus(SYNC_STATUS_FAILED);
        record.setRetryCount(0);
        record.setLastSyncTime(LocalDateTime.now());
        return record;
    }

    private void invalidStatus() {
        throw new ServiceException(SceneErrorCodeConstants.COMPLETION_STATUS_INVALID);
    }

    private String summary(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null) {
            return "外部同步失败";
        }
        return message.substring(0, Math.min(500, message.length()));
    }

    private ApprovedCompletionDTO toApprovedCompletion(SceneCompletionOrderEntity order,
                                                        SceneProductionTaskEntity task) {
        return new ApprovedCompletionDTO(order.getFinishNo(), task.getId(), order.getWorkOrderId(),
                task.getWorkOrderNo(), order.getProductId(), task.getProductCode(), task.getProductName(),
                order.getBatchNo(), order.getFinishQuantity(), order.getGoodQuantity(),
                order.getDefectQuantity(), order.getAuditBy(), order.getAuditTime(), order.getAuditRemark());
    }

    private String number() {
        return "WG" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
