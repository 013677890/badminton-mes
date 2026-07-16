package com.badminton.mes.module.scene.service.impl;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.scene.controller.vo.SceneProductionTaskSaveReqVO;
import com.badminton.mes.module.scene.dal.entity.SceneProductionTaskEntity;
import com.badminton.mes.module.scene.dal.redis.SceneNumberSequence;
import com.badminton.mes.module.scene.dal.repository.SceneDependencyQueryRepository;
import com.badminton.mes.module.scene.dal.repository.SceneDependencyQueryRepository.WorkOrderSnapshot;
import com.badminton.mes.module.scene.dal.repository.SceneDispatchDetailRepository;
import com.badminton.mes.module.scene.dal.repository.SceneProductionTaskRepository;
import com.badminton.mes.module.scene.dal.repository.SceneTaskOperateLogRepository;
import com.badminton.mes.module.scene.enums.SceneTaskStatusEnum;
import com.badminton.mes.module.scene.service.SceneDataScopeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.badminton.mes.module.scene.constants.SceneErrorCodeConstants.PARAM_VALUE_INVALID;
import static com.badminton.mes.module.scene.constants.SceneErrorCodeConstants.TASK_QUANTITY_EXCEEDED;
import static com.badminton.mes.module.scene.constants.SceneErrorCodeConstants.TASK_STATUS_INVALID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M2 生产任务创建快照、数量边界和 CAS 冲突测试。
 *
 * @author 范家权
 */
@ExtendWith(MockitoExtension.class)
class SceneProductionTaskServiceBoundaryTest {

    @Mock
    private SceneProductionTaskRepository taskRepository;

    @Mock
    private SceneTaskOperateLogRepository logRepository;

    @Mock
    private SceneDispatchDetailRepository detailRepository;

    @Mock
    private SceneDependencyQueryRepository dependencyRepository;

    @Mock
    private SceneNumberSequence numberSequence;

    @Mock
    private SceneDataScopeService dataScopeService;

    private SceneProductionTaskServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SceneProductionTaskServiceImpl(taskRepository, logRepository,
                detailRepository, dependencyRepository, numberSequence, dataScopeService);
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(7001L);
        loginUser.setRoleCodes(List.of("PMC"));
        SecurityContextHolder.set("unit-test-token", loginUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    void shouldFreezeReleasedWorkOrderSnapshotWhenCreatingTask() {
        SceneProductionTaskSaveReqVO request = request(60);
        WorkOrderSnapshot snapshot = snapshot(100);
        when(dependencyRepository.findReleasedWorkOrder(101L, 301L))
                .thenReturn(Optional.of(snapshot));
        when(taskRepository.sumAllocated(101L, List.of(
                SceneTaskStatusEnum.CANCELLED.getStatus(), SceneTaskStatusEnum.CLOSED.getStatus())))
                .thenReturn(40L);
        when(numberSequence.nextTaskNo()).thenReturn("RW202607130001");
        when(taskRepository.saveAndFlush(any(SceneProductionTaskEntity.class))).thenAnswer(invocation -> {
            SceneProductionTaskEntity entity = invocation.getArgument(0);
            entity.setId(9001L);
            return entity;
        });

        Long taskId = service.createTask(request);

        assertThat(taskId).isEqualTo(9001L);
        ArgumentCaptor<SceneProductionTaskEntity> captor =
                ArgumentCaptor.forClass(SceneProductionTaskEntity.class);
        verify(taskRepository).saveAndFlush(captor.capture());
        SceneProductionTaskEntity task = captor.getValue();
        assertThat(task.getTaskNo()).isEqualTo("RW202607130001");
        assertThat(task.getWorkOrderNo()).isEqualTo("WO-001");
        assertThat(task.getProductId()).isEqualTo(201L);
        assertThat(task.getProductCode()).isEqualTo("P-001");
        assertThat(task.getBatchNo()).isEqualTo("BATCH-001");
        assertThat(task.getRoutingId()).isEqualTo(401L);
        assertThat(task.getRoutingVersion()).isEqualTo("V3");
        assertThat(task.getWorkshopId()).isEqualTo(501L);
        assertThat(task.getLineId()).isEqualTo(301L);
        assertThat(task.getPlanQuantity()).isEqualTo(60);
        assertThat(task.getTaskStatus()).isEqualTo(SceneTaskStatusEnum.PENDING_AUDIT.getStatus());
        assertThat(task.getCreateBy()).isEqualTo(7001L);
        verify(dataScopeService).check(501L, 301L);
    }

    @Test
    void shouldRejectQuantityBeyondUnallocatedWorkOrderAmount() {
        SceneProductionTaskSaveReqVO request = request(60);
        when(dependencyRepository.findReleasedWorkOrder(101L, 301L))
                .thenReturn(Optional.of(snapshot(100)));
        when(taskRepository.sumAllocated(101L, List.of(
                SceneTaskStatusEnum.CANCELLED.getStatus(), SceneTaskStatusEnum.CLOSED.getStatus())))
                .thenReturn(41L);

        assertServiceError(() -> service.createTask(request), TASK_QUANTITY_EXCEEDED);

        verify(numberSequence, never()).nextTaskNo();
        verify(taskRepository, never()).saveAndFlush(any(SceneProductionTaskEntity.class));
    }

    @Test
    void shouldRejectReversedPlanTimeBeforeReadingDependencies() {
        SceneProductionTaskSaveReqVO request = request(10);
        request.setPlanStartTime(LocalDateTime.of(2026, 7, 13, 18, 0));
        request.setPlanEndTime(LocalDateTime.of(2026, 7, 13, 8, 0));

        assertServiceError(() -> service.createTask(request), PARAM_VALUE_INVALID);

        verify(dependencyRepository, never()).findReleasedWorkOrder(any(), any());
    }

    @Test
    void shouldNotWriteAuditLogWhenCasTransitionLosesRace() {
        SceneProductionTaskEntity task = new SceneProductionTaskEntity();
        task.setId(9001L);
        task.setWorkshopId(501L);
        task.setLineId(301L);
        task.setTaskStatus(SceneTaskStatusEnum.PENDING_AUDIT.getStatus());
        when(taskRepository.findByIdAndDeletedFalse(9001L)).thenReturn(Optional.of(task));
        when(taskRepository.transition(9001L,
                SceneTaskStatusEnum.PENDING_AUDIT.getStatus(),
                SceneTaskStatusEnum.AUDITED.getStatus(), null)).thenReturn(0);

        assertServiceError(() -> service.auditTask(9001L), TASK_STATUS_INVALID);

        verify(logRepository, never()).save(any());
    }

    private static SceneProductionTaskSaveReqVO request(int planQuantity) {
        SceneProductionTaskSaveReqVO request = new SceneProductionTaskSaveReqVO();
        request.setWorkOrderId(101L);
        request.setLineId(301L);
        request.setShiftId(801L);
        request.setPlanDate(LocalDate.of(2026, 7, 13));
        request.setPlanQuantity(planQuantity);
        request.setPlanStartTime(LocalDateTime.of(2026, 7, 13, 8, 0));
        request.setPlanEndTime(LocalDateTime.of(2026, 7, 13, 18, 0));
        return request;
    }

    private static WorkOrderSnapshot snapshot(int planQuantity) {
        return new WorkOrderSnapshot(101L, "WO-001", 201L, "P-001", "羽毛球",
                "BATCH-001", 401L, "ROUTE-001", "V3", 501L, "一车间",
                301L, "一号线", planQuantity, 0);
    }

    private static void assertServiceError(Runnable action,
                                           com.badminton.mes.common.core.ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ServiceException.class)
                .satisfies(throwable -> assertThat(((ServiceException) throwable).getErrorCode())
                        .isEqualTo(expected));
    }
}
