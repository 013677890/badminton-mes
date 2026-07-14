package com.badminton.mes.module.scene.service;

import java.util.Optional;

import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.integration.dal.entity.CompletionOrderEntity;
import com.badminton.mes.module.integration.dal.repository.CompletionOrderRepository;
import com.badminton.mes.module.integration.enums.CompletionAuditStatusEnum;
import com.badminton.mes.module.production.dal.entity.ProductEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;
import com.badminton.mes.module.production.dal.repository.ProductRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.scene.controller.vo.CompletionOrderSaveReqVO;
import com.badminton.mes.module.scene.dal.entity.SceneProcessTaskEntity;
import com.badminton.mes.module.scene.dal.entity.SceneExecutionTaskEntity;
import com.badminton.mes.module.scene.dal.repository.SceneProcessTaskRepository;
import com.badminton.mes.module.scene.dal.repository.SceneExecutionTaskRepository;
import com.badminton.mes.module.scene.dal.repository.SceneWorkReportRepository;
import com.badminton.mes.module.scene.enums.SceneExecutionTaskStatusEnum;
import com.badminton.mes.module.scene.enums.WorkReportAuditStatusEnum;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

/**
 * {@link SceneCompletionOrderService} 单元测试。
 *
 * @author Codex
 * @date 2026/07/13
 */
@ExtendWith(MockitoExtension.class)
class SceneCompletionOrderServiceTest {

    @Mock private CompletionOrderRepository completionOrderRepository;
    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private SceneExecutionTaskRepository productionTaskRepository;
    @Mock private SceneProcessTaskRepository processTaskRepository;
    @Mock private SceneWorkReportRepository workReportRepository;
    @InjectMocks private SceneCompletionOrderService service;

    @BeforeEach
    void setUp() {
        LoginUser user = new LoginUser();
        user.setUserId(9L);
        SecurityContextHolder.set("completion-test", user);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    void createAndApproveCompletionOrder() {
        WorkOrderEntity order = new WorkOrderEntity();
        order.setId(10L);
        order.setWorkOrderNo("WO001");
        order.setProductId(11L);
        ProductEntity product = new ProductEntity();
        product.setId(11L);
        product.setProductCode("P001");
        product.setProductName("羽毛球");
        SceneExecutionTaskEntity productionTask = new SceneExecutionTaskEntity();
        productionTask.setId(20L);
        productionTask.setWorkOrderId(10L);
        productionTask.setTaskStatus(SceneExecutionTaskStatusEnum.COMPLETED.getStatus());
        SceneProcessTaskEntity processTask = new SceneProcessTaskEntity();
        processTask.setProductionTaskId(20L);
        processTask.setProcessId(30L);
        processTask.setSequenceNo(1);
        processTask.setTaskStatus(SceneExecutionTaskStatusEnum.COMPLETED.getStatus());
        when(productionTaskRepository.findByIdForUpdate(20L))
                .thenReturn(Optional.of(productionTask));
        when(processTaskRepository.findByProductionTaskIdForUpdate(20L))
                .thenReturn(java.util.List.of(processTask));
        when(processTaskRepository.findByProductionTaskIdAndDeletedFalseOrderBySequenceNoAsc(20L))
                .thenReturn(java.util.List.of(processTask));
        when(workOrderRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdAndDeletedFalse(11L)).thenReturn(Optional.of(product));
        doAnswer(invocation -> {
            CompletionOrderEntity entity = invocation.getArgument(0);
            entity.setId(12L);
            return entity;
        }).when(completionOrderRepository).save(any());
        CompletionOrderSaveReqVO request = new CompletionOrderSaveReqVO();
        request.setProductionTaskId(20L);
        request.setWorkOrderId(10L);
        request.setBatchNo("B001");
        request.setCompletionQuantity(100);
        request.setGoodQuantity(95);
        request.setDefectQuantity(5);

        Long id = service.createCompletionOrder(request);

        assertThat(id).isEqualTo(12L);
        CompletionOrderEntity pending = new CompletionOrderEntity();
        pending.setId(12L);
        pending.setProductionTaskId(20L);
        pending.setWorkOrderId(10L);
        pending.setCompletionQuantity(100);
        pending.setDefectQuantity(5);
        pending.setAuditStatus(CompletionAuditStatusEnum.PENDING.getStatus());
        when(completionOrderRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(pending));
        when(workReportRepository.sumApprovedQuantity(
                20L, 30L, WorkReportAuditStatusEnum.APPROVED.getStatus()))
                .thenReturn(new java.math.BigDecimal("100"));
        when(completionOrderRepository.sumCompletionQuantityByTaskAndStatus(
                20L, CompletionAuditStatusEnum.APPROVED.getStatus())).thenReturn(0L);
        when(workOrderRepository.increaseCompletionQuantity(10L, 100, 5)).thenReturn(1);

        service.approveCompletionOrder(12L, "通过");

        assertThat(pending.getAuditStatus()).isEqualTo(CompletionAuditStatusEnum.APPROVED.getStatus());
        verify(completionOrderRepository).save(pending);
    }

    @Test
    void approveRejectsQuantityBeyondApprovedFinalProcessReports() {
        CompletionOrderEntity pending = new CompletionOrderEntity();
        pending.setId(12L);
        pending.setProductionTaskId(20L);
        pending.setWorkOrderId(10L);
        pending.setCompletionQuantity(100);
        pending.setDefectQuantity(5);
        pending.setAuditStatus(CompletionAuditStatusEnum.PENDING.getStatus());
        SceneExecutionTaskEntity productionTask = new SceneExecutionTaskEntity();
        productionTask.setId(20L);
        productionTask.setWorkOrderId(10L);
        productionTask.setTaskStatus(SceneExecutionTaskStatusEnum.COMPLETED.getStatus());
        SceneProcessTaskEntity processTask = new SceneProcessTaskEntity();
        processTask.setProductionTaskId(20L);
        processTask.setProcessId(30L);
        processTask.setSequenceNo(1);
        processTask.setTaskStatus(SceneExecutionTaskStatusEnum.COMPLETED.getStatus());
        when(completionOrderRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(pending));
        when(productionTaskRepository.findByIdForUpdate(20L))
                .thenReturn(Optional.of(productionTask));
        when(processTaskRepository.findByProductionTaskIdForUpdate(20L))
                .thenReturn(java.util.List.of(processTask));
        when(processTaskRepository.findByProductionTaskIdAndDeletedFalseOrderBySequenceNoAsc(20L))
                .thenReturn(java.util.List.of(processTask));
        when(workReportRepository.sumApprovedQuantity(
                20L, 30L, WorkReportAuditStatusEnum.APPROVED.getStatus()))
                .thenReturn(new java.math.BigDecimal("80"));
        when(completionOrderRepository.sumCompletionQuantityByTaskAndStatus(
                20L, CompletionAuditStatusEnum.APPROVED.getStatus())).thenReturn(0L);

        assertThatThrownBy(() -> service.approveCompletionOrder(12L, "通过"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("可完工报工数量不足");
        verify(workOrderRepository, never()).increaseCompletionQuantity(any(), any(), any());
    }
}
