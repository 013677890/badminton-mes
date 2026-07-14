package com.badminton.mes.module.scene.service;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.craft.dal.entity.CraftRouteDetailEntity;
import com.badminton.mes.module.craft.dal.repository.CraftRouteDetailRepository;
import com.badminton.mes.module.production.dal.entity.DispatchOrderEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.scene.dal.entity.SceneProcessTaskEntity;
import com.badminton.mes.module.scene.dal.entity.SceneExecutionTaskEntity;
import com.badminton.mes.module.scene.dal.repository.SceneProcessTaskRepository;
import com.badminton.mes.module.scene.dal.repository.SceneExecutionTaskRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SceneTaskService} 单元测试。
 *
 * @author Codex
 * @date 2026/07/13
 */
@ExtendWith(MockitoExtension.class)
class SceneTaskServiceTest {

    @Mock
    private SceneExecutionTaskRepository productionTaskRepository;

    @Mock
    private SceneProcessTaskRepository processTaskRepository;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private CraftRouteDetailRepository routeDetailRepository;

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
        DispatchOrderEntity dispatch = new DispatchOrderEntity();
        dispatch.setId(10L);
        dispatch.setDispatchNo("DO001");
        dispatch.setWorkOrderId(20L);
        dispatch.setLineId(30L);
        dispatch.setShiftId(40L);
        dispatch.setPlanQuantity(100);
        WorkOrderEntity workOrder = new WorkOrderEntity();
        workOrder.setId(20L);
        workOrder.setRoutingId(50L);
        CraftRouteDetailEntity first = buildStep(61L, 71L, 1);
        CraftRouteDetailEntity second = buildStep(62L, 72L, 2);
        when(productionTaskRepository.findByDispatchOrderIdAndDeletedFalse(10L))
                .thenReturn(Optional.empty());
        when(workOrderRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(workOrder));
        when(routeDetailRepository.findByRouteIdAndDeletedFalseOrderBySequenceNoAsc(50L))
                .thenReturn(List.of(first, second));
        doAnswer(invocation -> {
            SceneExecutionTaskEntity task = invocation.getArgument(0);
            task.setId(80L);
            return task;
        }).when(productionTaskRepository).saveAndFlush(any());

        Long taskId = sceneTaskService.issueDispatch(dispatch);

        assertThat(taskId).isEqualTo(80L);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SceneProcessTaskEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(processTaskRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(SceneProcessTaskEntity::getSequenceNo)
                .containsExactly(1, 2);
    }

    private CraftRouteDetailEntity buildStep(Long id, Long processId, int sequenceNo) {
        CraftRouteDetailEntity step = new CraftRouteDetailEntity();
        step.setId(id);
        step.setProcessId(processId);
        step.setSequenceNo(sequenceNo);
        return step;
    }
}
