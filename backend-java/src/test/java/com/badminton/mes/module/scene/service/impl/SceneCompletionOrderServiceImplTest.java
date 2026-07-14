package com.badminton.mes.module.scene.service.impl;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.common.security.SecurityContextHolder;
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
import com.badminton.mes.module.scene.service.SceneDataScopeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 完工单状态、数量和同步测试。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
class SceneCompletionOrderServiceImplTest {

    private final SceneCompletionOrderRepository orderRepository = mock(SceneCompletionOrderRepository.class);
    private final SceneCompletionSyncRecordRepository syncRecordRepository =
            mock(SceneCompletionSyncRecordRepository.class);
    private final SceneProductionTaskRepository taskRepository = mock(SceneProductionTaskRepository.class);
    private final SceneDataScopeService dataScopeService = mock(SceneDataScopeService.class);
    private final CompletionSyncClient syncClient = mock(CompletionSyncClient.class);
    private final CompletionSyncResultService syncResultService = mock(CompletionSyncResultService.class);
    private final SceneCompletionOrderServiceImpl service = new SceneCompletionOrderServiceImpl(
            orderRepository, syncRecordRepository, taskRepository, dataScopeService,
            syncClient, syncResultService);

    @BeforeEach
    void setUp() {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(9L);
        loginUser.setRoleCodes(List.of(RoleCodeConstants.ADMIN));
        SecurityContextHolder.set("test-token", loginUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    void duplicateTaskCreateReturnsExistingOrderId() {
        SceneProductionTaskEntity task = task();
        SceneCompletionOrderEntity existed = order(0);
        when(taskRepository.findByIdAndDeletedFalseForUpdate(1L)).thenReturn(Optional.of(task));
        when(orderRepository.findByTaskIdAndDeletedFalse(1L)).thenReturn(Optional.of(existed));

        assertThat(service.create(createRequest(10))).isEqualTo(5L);
        verify(orderRepository, never()).saveAndFlush(any());
    }

    @Test
    void draftCanUpdateFinishQuantityWithoutChangingTask() {
        SceneProductionTaskEntity task = task();
        SceneCompletionOrderEntity order = order(0);
        when(orderRepository.findByIdAndDeletedFalseForUpdate(5L)).thenReturn(Optional.of(order));
        when(taskRepository.findByIdAndDeletedFalseForUpdate(1L)).thenReturn(Optional.of(task));

        service.update(5L, saveRequest(8));

        assertThat(order.getFinishQuantity()).isEqualTo(8);
        assertThat(order.getGoodQuantity()).isEqualTo(8);
        assertThat(task.getFinishQuantity()).isZero();
        verify(taskRepository, never()).save(task);
        verify(syncClient, never()).sync(any(), any(), any());
    }

    @Test
    void rejectedOrderCanUpdateAndResubmit() {
        SceneProductionTaskEntity task = task();
        SceneCompletionOrderEntity order = order(3);
        when(orderRepository.findByIdAndDeletedFalseForUpdate(5L)).thenReturn(Optional.of(order));
        when(taskRepository.findByIdAndDeletedFalseForUpdate(1L)).thenReturn(Optional.of(task));
        when(taskRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(task));

        service.update(5L, saveRequest(6));
        service.submit(5L);

        assertThat(order.getFinishQuantity()).isEqualTo(6);
        assertThat(order.getFinishStatus()).isEqualTo(1);
    }

    @Test
    void pendingAuditOrderCannotBeUpdated() {
        SceneCompletionOrderEntity order = order(1);
        when(orderRepository.findByIdAndDeletedFalseForUpdate(5L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.update(5L, saveRequest(6)))
                .isInstanceOfSatisfying(ServiceException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(SceneErrorCodeConstants.COMPLETION_STATUS_INVALID));
    }

    @Test
    void updateQuantityCannotExceedAvailableGoodQuantity() {
        when(orderRepository.findByIdAndDeletedFalseForUpdate(5L)).thenReturn(Optional.of(order(0)));
        when(taskRepository.findByIdAndDeletedFalseForUpdate(1L)).thenReturn(Optional.of(task()));

        assertThatThrownBy(() -> service.update(5L, saveRequest(21)))
                .isInstanceOfSatisfying(ServiceException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(SceneErrorCodeConstants.COMPLETION_QUANTITY_INVALID));
    }

    @Test
    void approvedOrderUpdatesTaskFinishQuantityOnce() {
        SceneProductionTaskEntity task = task();
        SceneCompletionOrderEntity order = order(1);
        when(orderRepository.findByIdAndDeletedFalseForUpdate(5L)).thenReturn(Optional.of(order));
        when(taskRepository.findByIdAndDeletedFalseForUpdate(1L)).thenReturn(Optional.of(task));

        SceneCompletionAuditReqVO reqVO = new SceneCompletionAuditReqVO();
        reqVO.setApproved(true);
        service.audit(5L, reqVO);

        assertThat(order.getFinishStatus()).isEqualTo(2);
        assertThat(task.getFinishQuantity()).isEqualTo(10);
    }

    @Test
    void rejectedAuditDoesNotUpdateTaskFinishQuantity() {
        SceneProductionTaskEntity task = task();
        SceneCompletionOrderEntity order = order(1);
        when(orderRepository.findByIdAndDeletedFalseForUpdate(5L)).thenReturn(Optional.of(order));
        when(taskRepository.findByIdAndDeletedFalseForUpdate(1L)).thenReturn(Optional.of(task));

        SceneCompletionAuditReqVO reqVO = new SceneCompletionAuditReqVO();
        reqVO.setApproved(false);
        service.audit(5L, reqVO);

        assertThat(order.getFinishStatus()).isEqualTo(3);
        assertThat(task.getFinishQuantity()).isZero();
        verify(taskRepository, never()).save(task);
    }

    @Test
    void successfulSyncUsesStableIdempotencyKeyAndAtomicWriter() {
        SceneCompletionOrderEntity order = order(2);
        when(orderRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(order));
        when(taskRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(task()));
        when(syncRecordRepository.findByFinishOrderIdAndTargetSystemAndDeletedFalse(5L, "ERP"))
                .thenReturn(Optional.empty());

        service.sync(5L);

        verify(syncClient).sync(order, "ERP", "FINISH:WG001:ERP");
        verify(syncResultService).saveResult(org.mockito.ArgumentMatchers.eq(order), any(),
                org.mockito.ArgumentMatchers.eq(1), org.mockito.ArgumentMatchers.isNull());
    }

    @Test
    void failedSyncPersistsFailureBeforeReturningBusinessError() {
        SceneCompletionOrderEntity order = order(2);
        when(orderRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(order));
        when(taskRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(task()));
        when(syncRecordRepository.findByFinishOrderIdAndTargetSystemAndDeletedFalse(5L, "ERP"))
                .thenReturn(Optional.empty());
        org.mockito.Mockito.doThrow(new IllegalStateException("ERP unavailable"))
                .when(syncClient).sync(order, "ERP", "FINISH:WG001:ERP");

        assertThatThrownBy(() -> service.sync(5L))
                .isInstanceOfSatisfying(ServiceException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(SceneErrorCodeConstants.COMPLETION_SYNC_FAILED));
        verify(syncResultService).saveResult(org.mockito.ArgumentMatchers.eq(order), any(),
                org.mockito.ArgumentMatchers.eq(2), org.mockito.ArgumentMatchers.eq("ERP unavailable"));
    }

    @Test
    void successfulSyncReplayDoesNotCallExternalSystem() {
        SceneCompletionOrderEntity order = order(2);
        SceneCompletionSyncRecordEntity record = syncRecord(1, 1);
        when(orderRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(order));
        when(taskRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(task()));
        when(syncRecordRepository.findByFinishOrderIdAndTargetSystemAndDeletedFalse(5L, "ERP"))
                .thenReturn(Optional.of(record));

        service.sync(5L);

        verify(syncClient, never()).sync(any(), any(), any());
        verify(syncResultService, never()).saveResult(any(), any(),
                org.mockito.ArgumentMatchers.anyInt(), any());
    }

    @Test
    void fourthSyncAttemptIsRejectedBeforeExternalCall() {
        SceneCompletionOrderEntity order = order(2);
        SceneCompletionSyncRecordEntity record = syncRecord(2, 3);
        when(orderRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(order));
        when(taskRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(task()));
        when(syncRecordRepository.findByFinishOrderIdAndTargetSystemAndDeletedFalse(5L, "ERP"))
                .thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.sync(5L))
                .isInstanceOfSatisfying(ServiceException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(SceneErrorCodeConstants.COMPLETION_SYNC_RETRY_EXCEEDED));
        verify(syncClient, never()).sync(any(), any(), any());
    }

    private SceneCompletionCreateReqVO createRequest(int quantity) {
        SceneCompletionCreateReqVO reqVO = new SceneCompletionCreateReqVO();
        reqVO.setTaskId(1L);
        reqVO.setFinishQuantity(quantity);
        return reqVO;
    }

    private SceneCompletionSaveReqVO saveRequest(int quantity) {
        SceneCompletionSaveReqVO reqVO = new SceneCompletionSaveReqVO();
        reqVO.setFinishQuantity(quantity);
        return reqVO;
    }

    private SceneProductionTaskEntity task() {
        SceneProductionTaskEntity task = new SceneProductionTaskEntity();
        task.setId(1L);
        task.setWorkshopId(10L);
        task.setLineId(20L);
        task.setWorkOrderId(30L);
        task.setProductId(40L);
        task.setBatchNo("BATCH-1");
        task.setPlanQuantity(20);
        task.setGoodQuantity(20);
        task.setFinishQuantity(0);
        return task;
    }

    private SceneCompletionOrderEntity order(int status) {
        SceneCompletionOrderEntity order = new SceneCompletionOrderEntity();
        order.setId(5L);
        order.setTaskId(1L);
        order.setFinishNo("WG001");
        order.setFinishQuantity(10);
        order.setGoodQuantity(10);
        order.setFinishStatus(status);
        return order;
    }

    private SceneCompletionSyncRecordEntity syncRecord(int status, int retryCount) {
        SceneCompletionSyncRecordEntity record = new SceneCompletionSyncRecordEntity();
        record.setFinishOrderId(5L);
        record.setSyncStatus(status);
        record.setRetryCount(retryCount);
        return record;
    }
}
