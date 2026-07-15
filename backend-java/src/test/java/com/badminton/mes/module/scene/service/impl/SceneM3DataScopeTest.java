package com.badminton.mes.module.scene.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.barcode.service.BarcodeSceneService;
import com.badminton.mes.module.integration.service.CompletionOrderPublishService;
import com.badminton.mes.module.production.service.WorkOrderExecutionSummaryService;
import com.badminton.mes.module.scene.constants.SceneErrorCodeConstants;
import com.badminton.mes.module.scene.controller.vo.SceneCompletionCreateReqVO;
import com.badminton.mes.module.scene.controller.vo.SceneCompletionAuditReqVO;
import com.badminton.mes.module.scene.controller.vo.SceneCompletionSaveReqVO;
import com.badminton.mes.module.scene.controller.vo.SceneWorkReportReverseReqVO;
import com.badminton.mes.module.scene.controller.vo.SceneWorkReportSubmitReqVO;
import com.badminton.mes.module.scene.dal.entity.SceneCompletionOrderEntity;
import com.badminton.mes.module.scene.dal.entity.SceneDispatchDetailEntity;
import com.badminton.mes.module.scene.dal.entity.SceneProductionTaskEntity;
import com.badminton.mes.module.scene.dal.entity.SceneWorkReportEntity;
import com.badminton.mes.module.scene.dal.repository.SceneCompletionOrderRepository;
import com.badminton.mes.module.scene.dal.repository.SceneCompletionSyncRecordRepository;
import com.badminton.mes.module.scene.dal.repository.SceneDispatchDetailRepository;
import com.badminton.mes.module.scene.dal.repository.SceneProductionTaskRepository;
import com.badminton.mes.module.scene.dal.repository.SceneWorkReportRepository;
import com.badminton.mes.module.scene.service.CompletionSyncClient;
import com.badminton.mes.module.scene.service.CompletionSyncResultService;
import com.badminton.mes.module.scene.service.SceneDataScopeService;
import com.badminton.mes.module.scene.service.SceneProductionParameterService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M3 报工和完工的车间、产线对象级权限专项测试。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
class SceneM3DataScopeTest {

    private final SceneDataScopeService dataScopeService = new SceneDataScopeService();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    void operatorCannotReportAcrossWorkshop() {
        login(RoleCodeConstants.OPERATOR, 10L, 20L);
        assertReportDenied(task(11L, 20L));
    }

    @Test
    void operatorCannotReportAcrossLine() {
        login(RoleCodeConstants.OPERATOR, 10L, 20L);
        assertReportDenied(task(10L, 21L));
    }

    @Test
    void workshopManagerCannotReverseReportAcrossWorkshop() {
        login(RoleCodeConstants.WORKSHOP_MANAGER, 10L, 20L);
        SceneWorkReportRepository reportRepository = mock(SceneWorkReportRepository.class);
        SceneProductionTaskRepository taskRepository = mock(SceneProductionTaskRepository.class);
        SceneDispatchDetailRepository detailRepository = mock(SceneDispatchDetailRepository.class);
        SceneProductionParameterService parameterService = mock(SceneProductionParameterService.class);
        BarcodeSceneService barcodeService = mock(BarcodeSceneService.class);
        SceneWorkReportTransactionalService service = new SceneWorkReportTransactionalService(
                reportRepository, taskRepository, detailRepository, dataScopeService,
                parameterService, barcodeService, mock(WorkOrderExecutionSummaryService.class));
        SceneWorkReportEntity source = new SceneWorkReportEntity();
        source.setId(3L);
        source.setTaskId(1L);
        source.setDispatchDetailId(2L);
        source.setRecordType(1);
        when(reportRepository.findByRequestNoAndDeletedFalse("REV-SCOPE")).thenReturn(Optional.empty());
        when(reportRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(source));
        when(reportRepository.existsBySourceReportIdAndDeletedFalse(3L)).thenReturn(false);
        when(taskRepository.findByIdAndDeletedFalseForUpdate(1L))
                .thenReturn(Optional.of(task(11L, 20L)));
        SceneWorkReportReverseReqVO reqVO = new SceneWorkReportReverseReqVO();
        reqVO.setRequestNo("REV-SCOPE");

        assertThatThrownBy(() -> service.reverse(3L, reqVO))
                .isInstanceOfSatisfying(ServiceException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(SceneErrorCodeConstants.DATA_SCOPE_DENIED));
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    void teamLeaderCannotCreateCompletionAcrossWorkshop() {
        login(RoleCodeConstants.TEAM_LEADER, 10L, 20L);
        CompletionFixture fixture = completionFixture(task(11L, 20L));

        assertThatThrownBy(() -> fixture.service.create(createRequest()))
                .isInstanceOfSatisfying(ServiceException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(SceneErrorCodeConstants.DATA_SCOPE_DENIED));
        verify(fixture.orderRepository, never()).saveAndFlush(any());
    }

    @Test
    void teamLeaderCannotCreateCompletionAcrossLine() {
        login(RoleCodeConstants.TEAM_LEADER, 10L, 20L);
        CompletionFixture fixture = completionFixture(task(10L, 21L));

        assertThatThrownBy(() -> fixture.service.create(createRequest()))
                .isInstanceOf(ServiceException.class);
        verify(fixture.orderRepository, never()).saveAndFlush(any());
    }

    @Test
    void teamLeaderCannotUpdateCompletionAcrossLine() {
        login(RoleCodeConstants.TEAM_LEADER, 10L, 20L);
        CompletionFixture fixture = completionFixture(task(10L, 21L));
        SceneCompletionOrderEntity order = new SceneCompletionOrderEntity();
        order.setId(5L);
        order.setTaskId(1L);
        order.setFinishStatus(0);
        when(fixture.orderRepository.findByIdAndDeletedFalseForUpdate(5L))
                .thenReturn(Optional.of(order));
        SceneCompletionSaveReqVO reqVO = new SceneCompletionSaveReqVO();
        reqVO.setFinishQuantity(5);

        assertThatThrownBy(() -> fixture.service.update(5L, reqVO))
                .isInstanceOfSatisfying(ServiceException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(SceneErrorCodeConstants.DATA_SCOPE_DENIED));
        verify(fixture.orderRepository, never()).save(order);
    }

    @Test
    void workshopManagerCanCreateCompletionOnAnotherLineInOwnWorkshop() {
        login(RoleCodeConstants.WORKSHOP_MANAGER, 10L, 20L);
        CompletionFixture fixture = completionFixture(task(10L, 99L));
        when(fixture.orderRepository.findByTaskIdAndDeletedFalse(1L)).thenReturn(Optional.empty());
        when(fixture.orderRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            SceneCompletionOrderEntity order = invocation.getArgument(0);
            order.setId(5L);
            return order;
        });

        assertThat(fixture.service.create(createRequest())).isEqualTo(5L);
    }

    @Test
    void workshopManagerCannotSyncCompletionAcrossWorkshop() {
        login(RoleCodeConstants.WORKSHOP_MANAGER, 10L, 20L);
        CompletionFixture fixture = completionFixture(task(11L, 20L));
        SceneCompletionOrderEntity order = new SceneCompletionOrderEntity();
        order.setId(5L);
        order.setTaskId(1L);
        order.setFinishStatus(2);
        when(fixture.orderRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(order));
        when(fixture.taskRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(task(11L, 20L)));

        assertThatThrownBy(() -> fixture.service.sync(5L)).isInstanceOf(ServiceException.class);
        verify(fixture.syncClient, never()).sync(any(), any(), any());
    }

    @Test
    void workshopManagerCannotAuditCompletionAcrossWorkshop() {
        login(RoleCodeConstants.WORKSHOP_MANAGER, 10L, 20L);
        CompletionFixture fixture = completionFixture(task(11L, 20L));
        SceneCompletionOrderEntity order = new SceneCompletionOrderEntity();
        order.setId(5L);
        order.setTaskId(1L);
        order.setFinishStatus(1);
        when(fixture.orderRepository.findByIdAndDeletedFalseForUpdate(5L))
                .thenReturn(Optional.of(order));
        SceneCompletionAuditReqVO reqVO = new SceneCompletionAuditReqVO();
        reqVO.setApproved(true);

        assertThatThrownBy(() -> fixture.service.audit(5L, reqVO))
                .isInstanceOfSatisfying(ServiceException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(SceneErrorCodeConstants.DATA_SCOPE_DENIED));
        verify(fixture.orderRepository, never()).save(order);
    }

    private void assertReportDenied(SceneProductionTaskEntity task) {
        SceneWorkReportRepository reportRepository = mock(SceneWorkReportRepository.class);
        SceneProductionTaskRepository taskRepository = mock(SceneProductionTaskRepository.class);
        SceneDispatchDetailRepository detailRepository = mock(SceneDispatchDetailRepository.class);
        SceneProductionParameterService parameterService = mock(SceneProductionParameterService.class);
        BarcodeSceneService barcodeService = mock(BarcodeSceneService.class);
        SceneWorkReportTransactionalService service = new SceneWorkReportTransactionalService(
                reportRepository, taskRepository, detailRepository, dataScopeService,
                parameterService, barcodeService, mock(WorkOrderExecutionSummaryService.class));
        SceneDispatchDetailEntity detail = new SceneDispatchDetailEntity();
        detail.setId(2L);
        detail.setTaskId(1L);
        when(reportRepository.findByRequestNoAndDeletedFalse("REQ-SCOPE")).thenReturn(Optional.empty());
        when(detailRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(detail));
        when(taskRepository.findByIdAndDeletedFalseForUpdate(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.submit(reportRequest(), 1))
                .isInstanceOfSatisfying(ServiceException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(SceneErrorCodeConstants.DATA_SCOPE_DENIED));
        verify(reportRepository, never()).saveAndFlush(any());
        verify(barcodeService, never()).validateAndRecordUse(any(), any(), any(), any(),
                any(), any(), any(), any());
    }

    private CompletionFixture completionFixture(SceneProductionTaskEntity task) {
        SceneCompletionOrderRepository orderRepository = mock(SceneCompletionOrderRepository.class);
        SceneCompletionSyncRecordRepository syncRecordRepository =
                mock(SceneCompletionSyncRecordRepository.class);
        SceneProductionTaskRepository taskRepository = mock(SceneProductionTaskRepository.class);
        CompletionSyncClient syncClient = mock(CompletionSyncClient.class);
        CompletionSyncResultService syncResultService = mock(CompletionSyncResultService.class);
        when(taskRepository.findByIdAndDeletedFalseForUpdate(1L)).thenReturn(Optional.of(task));
        SceneCompletionOrderServiceImpl service = new SceneCompletionOrderServiceImpl(
                orderRepository, syncRecordRepository, taskRepository, dataScopeService,
                syncClient, syncResultService, mock(WorkOrderExecutionSummaryService.class),
                mock(CompletionOrderPublishService.class));
        return new CompletionFixture(service, orderRepository, taskRepository, syncClient);
    }

    private SceneProductionTaskEntity task(Long workshopId, Long lineId) {
        SceneProductionTaskEntity task = new SceneProductionTaskEntity();
        task.setId(1L);
        task.setWorkshopId(workshopId);
        task.setLineId(lineId);
        task.setGoodQuantity(20);
        task.setFinishQuantity(0);
        return task;
    }

    private SceneCompletionCreateReqVO createRequest() {
        SceneCompletionCreateReqVO reqVO = new SceneCompletionCreateReqVO();
        reqVO.setTaskId(1L);
        reqVO.setFinishQuantity(10);
        return reqVO;
    }

    private SceneWorkReportSubmitReqVO reportRequest() {
        SceneWorkReportSubmitReqVO reqVO = new SceneWorkReportSubmitReqVO();
        reqVO.setRequestNo("REQ-SCOPE");
        reqVO.setDispatchDetailId(2L);
        reqVO.setReportTime(LocalDateTime.now());
        return reqVO;
    }

    private void login(String roleCode, Long workshopId, Long lineId) {
        LoginUser user = new LoginUser();
        user.setUserId(9L);
        user.setWorkshopId(workshopId);
        user.setLineId(lineId);
        user.setRoleCodes(List.of(roleCode));
        SecurityContextHolder.set("scope-token", user);
    }

    private record CompletionFixture(SceneCompletionOrderServiceImpl service,
                                     SceneCompletionOrderRepository orderRepository,
                                     SceneProductionTaskRepository taskRepository,
                                     CompletionSyncClient syncClient) {
    }
}
