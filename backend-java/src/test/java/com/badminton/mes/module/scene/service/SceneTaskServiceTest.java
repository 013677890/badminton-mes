package com.badminton.mes.module.scene.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.craft.dal.entity.CraftRouteDetailEntity;
import com.badminton.mes.module.craft.dal.repository.CraftRouteDetailRepository;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.dal.entity.DispatchOrderEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.scene.dal.entity.SceneProcessTaskEntity;
import com.badminton.mes.module.scene.dal.entity.SceneProductionTaskEntity;
import com.badminton.mes.module.scene.dal.repository.SceneDependencyQueryRepository;
import com.badminton.mes.module.scene.dal.repository.SceneDependencyQueryRepository.WorkOrderSnapshot;
import com.badminton.mes.module.scene.dal.repository.SceneProcessTaskRepository;
import com.badminton.mes.module.scene.dal.repository.SceneProductionTaskRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SceneTaskService} 单元测试。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@ExtendWith(MockitoExtension.class)
class SceneTaskServiceTest {

    @Mock
    private SceneProductionTaskRepository productionTaskRepository;

    @Mock
    private SceneProcessTaskRepository processTaskRepository;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private CraftRouteDetailRepository routeDetailRepository;

    @Mock
    private SceneDependencyQueryRepository dependencyQueryRepository;

    @InjectMocks
    private SceneTaskService sceneTaskService;

    @BeforeEach
    void setUp() {
        LoginUser user = new LoginUser();
        user.setUserId(9L);
        SecurityContextHolder.set("scene-task-test", user);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    void issueDispatchCreatesTaskAndOrderedProcessSnapshots() {
        DispatchOrderEntity dispatch = buildDispatch();
        WorkOrderEntity workOrder = new WorkOrderEntity();
        workOrder.setId(20L);
        workOrder.setRoutingId(50L);
        CraftRouteDetailEntity first = buildStep(61L, 71L, 1);
        CraftRouteDetailEntity second = buildStep(62L, 72L, 2);
        when(productionTaskRepository.findByDispatchOrderIdAndDeletedFalse(10L))
                .thenReturn(Optional.empty());
        when(workOrderRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(workOrder));
        when(dependencyQueryRepository.findWorkOrderSnapshot(20L, 30L))
                .thenReturn(Optional.of(buildSnapshot()));
        when(routeDetailRepository.findByRouteIdAndDeletedFalseOrderBySequenceNoAsc(50L))
                .thenReturn(List.of(first, second));
        doAnswer(invocation -> {
            SceneProductionTaskEntity task = invocation.getArgument(0);
            task.setId(80L);
            return task;
        }).when(productionTaskRepository).saveAndFlush(any());

        Long taskId = sceneTaskService.issueDispatch(dispatch);

        assertThat(taskId).isEqualTo(80L);
        ArgumentCaptor<SceneProductionTaskEntity> taskCaptor =
                ArgumentCaptor.forClass(SceneProductionTaskEntity.class);
        verify(productionTaskRepository).saveAndFlush(taskCaptor.capture());
        SceneProductionTaskEntity saved = taskCaptor.getValue();
        assertThat(saved.getTaskNo()).isEqualTo("DO001");
        assertThat(saved.getSourceType()).isEqualTo(1);
        assertThat(saved.getWorkOrderNo()).isEqualTo("WO20260713001");
        assertThat(saved.getProductId()).isEqualTo(3L);
        assertThat(saved.getProductCode()).isEqualTo("P001");
        assertThat(saved.getProductName()).isEqualTo("训练级羽毛球");
        assertThat(saved.getBatchNo()).isEqualTo("B20260713");
        assertThat(saved.getRoutingId()).isEqualTo(50L);
        assertThat(saved.getRoutingCode()).isEqualTo("RT01");
        assertThat(saved.getRoutingVersion()).isEqualTo("V1");
        assertThat(saved.getWorkshopId()).isEqualTo(7L);
        assertThat(saved.getWorkshopName()).isEqualTo("一号车间");
        assertThat(saved.getLineId()).isEqualTo(30L);
        assertThat(saved.getLineName()).isEqualTo("一号产线");
        assertThat(saved.getPlanDate()).isEqualTo(LocalDate.of(2026, 7, 14));
        assertThat(saved.getPlanStartTime()).isEqualTo(LocalDateTime.of(2026, 7, 14, 8, 0));
        assertThat(saved.getPlanEndTime()).isEqualTo(LocalDateTime.of(2026, 7, 14, 17, 0));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SceneProcessTaskEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(processTaskRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(SceneProcessTaskEntity::getSequenceNo)
                .containsExactly(1, 2);
    }

    @Test
    void issueDispatchRejectsWhenSnapshotUnavailable() {
        DispatchOrderEntity dispatch = buildDispatch();
        WorkOrderEntity workOrder = new WorkOrderEntity();
        workOrder.setId(20L);
        workOrder.setRoutingId(50L);
        when(productionTaskRepository.findByDispatchOrderIdAndDeletedFalse(10L))
                .thenReturn(Optional.empty());
        when(workOrderRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(workOrder));
        when(dependencyQueryRepository.findWorkOrderSnapshot(20L, 30L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> sceneTaskService.issueDispatch(dispatch))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                ProductionErrorCodeConstants.WORK_ORDER_ROUTING_NOT_AVAILABLE));
    }

    private DispatchOrderEntity buildDispatch() {
        DispatchOrderEntity dispatch = new DispatchOrderEntity();
        dispatch.setId(10L);
        dispatch.setDispatchNo("DO001");
        dispatch.setWorkOrderId(20L);
        dispatch.setLineId(30L);
        dispatch.setShiftId(40L);
        dispatch.setPlanQuantity(100);
        dispatch.setPlanDate(LocalDate.of(2026, 7, 14));
        dispatch.setPlanStartTime(LocalDateTime.of(2026, 7, 14, 8, 0));
        dispatch.setPlanEndTime(LocalDateTime.of(2026, 7, 14, 17, 0));
        return dispatch;
    }

    private WorkOrderSnapshot buildSnapshot() {
        return new WorkOrderSnapshot(20L, "WO20260713001", 3L, "P001", "训练级羽毛球",
                "B20260713", 50L, "RT01", "V1", 7L, "一号车间", 30L, "一号产线", 100, 0);
    }

    private CraftRouteDetailEntity buildStep(Long id, Long processId, int sequenceNo) {
        CraftRouteDetailEntity step = new CraftRouteDetailEntity();
        step.setId(id);
        step.setProcessId(processId);
        step.setSequenceNo(sequenceNo);
        return step;
    }
}
