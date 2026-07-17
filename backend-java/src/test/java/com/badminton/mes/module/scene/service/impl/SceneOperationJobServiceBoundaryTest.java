package com.badminton.mes.module.scene.service.impl;

import com.badminton.mes.common.core.ErrorCode;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.barcode.service.BarcodeSceneService;
import com.badminton.mes.module.scene.controller.vo.SceneOperationScanReqVO;
import com.badminton.mes.module.scene.controller.vo.SceneProductionParameterRespVO;
import com.badminton.mes.module.scene.dal.entity.SceneBatchProcessHistoryEntity;
import com.badminton.mes.module.scene.dal.entity.SceneBatchStatusEntity;
import com.badminton.mes.module.scene.dal.entity.SceneBatchStatusHistoryEntity;
import com.badminton.mes.module.scene.dal.entity.SceneDispatchDetailEntity;
import com.badminton.mes.module.scene.dal.entity.SceneDispatchOrderEntity;
import com.badminton.mes.module.scene.dal.entity.SceneProductionTaskEntity;
import com.badminton.mes.module.scene.dal.repository.SceneBatchProcessHistoryRepository;
import com.badminton.mes.module.scene.dal.repository.SceneBatchStatusHistoryRepository;
import com.badminton.mes.module.scene.dal.repository.SceneBatchStatusRepository;
import com.badminton.mes.module.scene.dal.repository.SceneDispatchDetailRepository;
import com.badminton.mes.module.scene.dal.repository.SceneDispatchOrderRepository;
import com.badminton.mes.module.scene.dal.repository.SceneProductionTaskRepository;
import com.badminton.mes.module.scene.enums.SceneBatchStatusEnum;
import com.badminton.mes.module.scene.enums.SceneDispatchStatusEnum;
import com.badminton.mes.module.scene.enums.SceneOperationStatusEnum;
import com.badminton.mes.module.scene.enums.SceneTaskStatusEnum;
import com.badminton.mes.module.scene.service.SceneDataScopeService;
import com.badminton.mes.module.scene.service.SceneProductionParameterService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.badminton.mes.module.scene.constants.SceneErrorCodeConstants.DATA_SCOPE_DENIED;
import static com.badminton.mes.module.scene.constants.SceneErrorCodeConstants.OPERATION_SEQUENCE_INVALID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M2 工序扫码、人员权限、顺序门禁和状态联动测试。
 *
 * @author 范家权
 */
@ExtendWith(MockitoExtension.class)
class SceneOperationJobServiceBoundaryTest {

    @Mock
    private SceneDispatchDetailRepository detailRepository;

    @Mock
    private SceneDispatchOrderRepository orderRepository;

    @Mock
    private SceneProductionTaskRepository taskRepository;

    @Mock
    private SceneBatchStatusRepository batchRepository;

    @Mock
    private SceneBatchStatusHistoryRepository statusHistoryRepository;

    @Mock
    private SceneBatchProcessHistoryRepository processHistoryRepository;

    @Mock
    private BarcodeSceneService barcodeService;

    @Mock
    private SceneProductionParameterService parameterService;

    @Mock
    private SceneDataScopeService dataScope;

    private SceneOperationJobServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SceneOperationJobServiceImpl(detailRepository, orderRepository,
                taskRepository, batchRepository, statusHistoryRepository,
                processHistoryRepository, barcodeService, parameterService, dataScope);
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(51L);
        loginUser.setRoleCodes(List.of("OPERATOR"));
        SecurityContextHolder.set("unit-test-token", loginUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    void shouldRecordBarcodeUseAndCreateFirstBatchHistoriesWhenScanning() {
        SceneDispatchDetailEntity detail = detail();
        SceneProductionTaskEntity task = task();
        when(detailRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(detail));
        when(taskRepository.findByIdAndDeletedFalse(30L)).thenReturn(Optional.of(task));
        when(batchRepository.findByBatchNoAndDeletedFalse("BATCH-001"))
                .thenReturn(Optional.empty());
        when(batchRepository.saveAndFlush(any(SceneBatchStatusEntity.class))).thenAnswer(invocation -> {
            SceneBatchStatusEntity entity = invocation.getArgument(0);
            entity.setId(70L);
            return entity;
        });
        SceneOperationScanReqVO request = new SceneOperationScanReqVO();
        request.setBarcodeValue("BC-001");
        request.setEquipmentId(61L);

        service.scan(10L, request);

        verify(barcodeService).validateAndRecordUse(
                "BC-001", 30L, 201L, "BATCH-001", 40L, 51L, 61L, 1);
        ArgumentCaptor<SceneBatchStatusHistoryEntity> statusCaptor =
                ArgumentCaptor.forClass(SceneBatchStatusHistoryEntity.class);
        verify(statusHistoryRepository).save(statusCaptor.capture());
        assertThat(statusCaptor.getValue().getBatchStatusId()).isEqualTo(70L);
        assertThat(statusCaptor.getValue().getToStatus())
                .isEqualTo(SceneBatchStatusEnum.IN_PROCESS.getStatus());
        assertThat(statusCaptor.getValue().getChangeReason()).isEqualTo("首道工序进入生产");
        assertThat(statusCaptor.getValue().getOperatorId()).isEqualTo(51L);

        ArgumentCaptor<SceneBatchProcessHistoryEntity> processCaptor =
                ArgumentCaptor.forClass(SceneBatchProcessHistoryEntity.class);
        verify(processHistoryRepository).save(processCaptor.capture());
        assertThat(processCaptor.getValue().getBatchStatusId()).isEqualTo(70L);
        assertThat(processCaptor.getValue().getDispatchDetailId()).isEqualTo(10L);
        assertThat(processCaptor.getValue().getActionType()).isEqualTo(1);
        assertThat(processCaptor.getValue().getOperatorId()).isEqualTo(51L);
    }

    @Test
    void shouldRejectOperationAssignedToAnotherOperator() {
        SceneDispatchDetailEntity detail = detail();
        detail.setUserId(999L);
        when(detailRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(detail));
        when(taskRepository.findByIdAndDeletedFalse(30L)).thenReturn(Optional.of(task()));

        assertServiceError(() -> service.start(10L), DATA_SCOPE_DENIED);

        verify(detailRepository, never()).save(any(SceneDispatchDetailEntity.class));
        verify(processHistoryRepository, never()).save(any(SceneBatchProcessHistoryEntity.class));
    }

    @Test
    void shouldRejectSkippedKeyProcessEvenWhenOrdinaryProcessesMayBeSkipped() {
        SceneDispatchDetailEntity current = detail();
        current.setKeyProcess(true);
        current.setSeq(2);
        SceneDispatchDetailEntity previous = detail();
        previous.setId(9L);
        previous.setSeq(1);
        previous.setDetailStatus(SceneOperationStatusEnum.PENDING.getStatus());
        when(detailRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(current));
        when(taskRepository.findByIdAndDeletedFalse(30L)).thenReturn(Optional.of(task()));
        when(detailRepository.findByDispatchIdAndDeletedFalseOrderBySeqAsc(20L))
                .thenReturn(List.of(previous, current));

        assertServiceError(() -> service.start(10L), OPERATION_SEQUENCE_INVALID);

        verify(parameterService, never()).getEffectiveParameter(any());
        verify(detailRepository, never()).save(current);
    }

    @Test
    void shouldStartOrdinaryProcessAndLinkTaskDispatchAndBatchStatusesWhenSkipEnabled() {
        SceneDispatchDetailEntity detail = detail();
        detail.setSeq(2);
        SceneProductionTaskEntity task = task();
        task.setTaskStatus(SceneTaskStatusEnum.RELEASED.getStatus());
        SceneProductionParameterRespVO parameter = new SceneProductionParameterRespVO();
        parameter.setParamValue("1");
        SceneDispatchOrderEntity order = new SceneDispatchOrderEntity();
        order.setId(20L);
        order.setDispatchStatus(SceneDispatchStatusEnum.CONFIRMED.getStatus());
        SceneBatchStatusEntity batch = new SceneBatchStatusEntity();
        batch.setId(70L);
        batch.setBatchNo("BATCH-001");

        when(detailRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(detail));
        when(taskRepository.findByIdAndDeletedFalse(30L)).thenReturn(Optional.of(task));
        when(parameterService.getEffectiveParameter(any())).thenReturn(parameter);
        when(orderRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(order));
        when(batchRepository.findByBatchNoAndDeletedFalse("BATCH-001"))
                .thenReturn(Optional.of(batch));

        service.start(10L);

        assertThat(detail.getDetailStatus()).isEqualTo(SceneOperationStatusEnum.IN_PROGRESS.getStatus());
        assertThat(detail.getPaused()).isFalse();
        assertThat(detail.getActualStartTime()).isNotNull();
        assertThat(batch.getCurrentProcessId()).isEqualTo(40L);
        assertThat(batch.getCurrentProcessName()).isEqualTo("穿线");
        verify(orderRepository).transition(20L,
                SceneDispatchStatusEnum.CONFIRMED.getStatus(),
                SceneDispatchStatusEnum.IN_PROGRESS.getStatus());
        verify(taskRepository).transition(30L,
                SceneTaskStatusEnum.RELEASED.getStatus(),
                SceneTaskStatusEnum.IN_PRODUCTION.getStatus(), null);
        ArgumentCaptor<SceneBatchProcessHistoryEntity> historyCaptor =
                ArgumentCaptor.forClass(SceneBatchProcessHistoryEntity.class);
        verify(processHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getActionType()).isEqualTo(2);
    }

    private static SceneDispatchDetailEntity detail() {
        SceneDispatchDetailEntity detail = new SceneDispatchDetailEntity();
        detail.setId(10L);
        detail.setDispatchId(20L);
        detail.setTaskId(30L);
        detail.setProcessId(40L);
        detail.setProcessCode("PX-001");
        detail.setProcessName("穿线");
        detail.setSeq(1);
        detail.setKeyProcess(false);
        detail.setInspect(false);
        detail.setScanRequired(false);
        detail.setDetailStatus(SceneOperationStatusEnum.PENDING.getStatus());
        detail.setPaused(false);
        return detail;
    }

    private static SceneProductionTaskEntity task() {
        SceneProductionTaskEntity task = new SceneProductionTaskEntity();
        task.setId(30L);
        task.setProductId(201L);
        task.setBatchNo("BATCH-001");
        task.setWorkshopId(501L);
        task.setLineId(301L);
        task.setTaskStatus(SceneTaskStatusEnum.RELEASED.getStatus());
        return task;
    }

    private static void assertServiceError(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ServiceException.class)
                .satisfies(throwable -> assertThat(((ServiceException) throwable).getErrorCode())
                        .isEqualTo(expected));
    }
}
