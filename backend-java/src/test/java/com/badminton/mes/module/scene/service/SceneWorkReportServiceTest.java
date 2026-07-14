package com.badminton.mes.module.scene.service;

import java.time.LocalDateTime;
import java.util.Optional;

import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.module.craft.dal.entity.CraftProcessEntity;
import com.badminton.mes.module.craft.dal.repository.CraftProcessRepository;
import com.badminton.mes.module.integration.dal.entity.DeviceCountRecordEntity;
import com.badminton.mes.module.integration.dal.entity.EquipmentBindingEntity;
import com.badminton.mes.module.production.dal.entity.DispatchOrderEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.scene.dal.entity.SceneExecutionTaskEntity;
import com.badminton.mes.module.scene.dal.entity.SceneProcessTaskEntity;
import com.badminton.mes.module.scene.dal.entity.SceneWorkReportEntity;
import com.badminton.mes.module.scene.dal.repository.SceneExecutionTaskRepository;
import com.badminton.mes.module.scene.dal.repository.SceneProcessTaskRepository;
import com.badminton.mes.module.scene.dal.repository.SceneWorkReportRepository;
import com.badminton.mes.module.system.dal.repository.UserRepository;
import com.badminton.mes.module.system.dal.entity.UserEntity;
import com.badminton.mes.module.scene.enums.SceneExecutionTaskStatusEnum;
import com.badminton.mes.module.wage.dal.repository.WageWorkRecordRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SceneWorkReportService} 单元测试。
 *
 * @author Codex
 * @date 2026/07/13
 */
@ExtendWith(MockitoExtension.class)
class SceneWorkReportServiceTest {

    @Mock private SceneWorkReportRepository reportRepository;
    @Mock private SceneExecutionTaskRepository taskRepository;
    @Mock private SceneProcessTaskRepository processTaskRepository;
    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private CraftProcessRepository craftProcessRepository;
    @Mock private UserRepository userRepository;
    @Mock private WageWorkRecordRepository wageWorkRecordRepository;
    @InjectMocks private SceneWorkReportService service;

    @BeforeEach
    void setUp() {
        LoginUser user = new LoginUser();
        user.setUserId(9L);
        SecurityContextHolder.set("scene-report-test", user);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    void deviceAutoReportCreatesApprovedReportAndWageSnapshot() {
        DeviceCountRecordEntity record = new DeviceCountRecordEntity();
        record.setId(11L);
        record.setProcessId(12L);
        record.setIncrementValue(30L);
        record.setCollectTime(LocalDateTime.of(2026, 7, 13, 10, 0));
        EquipmentBindingEntity binding = new EquipmentBindingEntity();
        binding.setAutoReport(true);
        binding.setDefaultEmployeeId(13L);
        DispatchOrderEntity dispatch = new DispatchOrderEntity();
        dispatch.setId(14L);
        dispatch.setWorkOrderId(15L);
        SceneExecutionTaskEntity task = new SceneExecutionTaskEntity();
        task.setId(16L);
        task.setPlanQuantity(30);
        task.setTaskStatus(SceneExecutionTaskStatusEnum.PENDING.getStatus());
        SceneProcessTaskEntity processTask = new SceneProcessTaskEntity();
        processTask.setId(19L);
        processTask.setProductionTaskId(16L);
        processTask.setProcessId(12L);
        processTask.setSequenceNo(1);
        processTask.setTaskStatus(SceneExecutionTaskStatusEnum.PENDING.getStatus());
        CraftProcessEntity process = new CraftProcessEntity();
        process.setId(12L);
        UserEntity employee = new UserEntity();
        employee.setId(13L);
        WorkOrderEntity workOrder = new WorkOrderEntity();
        workOrder.setId(15L);
        workOrder.setProductId(17L);
        when(reportRepository.findBySourceTypeAndSourceRecordIdAndDeletedFalse(2, 11L))
                .thenReturn(Optional.empty());
        when(taskRepository.findByDispatchOrderIdAndDeletedFalse(14L)).thenReturn(Optional.of(task));
        when(taskRepository.findByIdForUpdate(16L)).thenReturn(Optional.of(task));
        when(processTaskRepository.findByProductionTaskIdForUpdate(16L))
                .thenReturn(java.util.List.of(processTask));
        when(workOrderRepository.findByIdAndDeletedFalse(15L)).thenReturn(Optional.of(workOrder));
        when(craftProcessRepository.findByIdAndStatusAndDeletedFalse(
                12L, CommonStatusEnum.ENABLED.getStatus())).thenReturn(Optional.of(process));
        when(userRepository.findByIdAndStatusAndDeletedFalse(
                13L, CommonStatusEnum.ENABLED.getStatus())).thenReturn(Optional.of(employee));
        doAnswer(invocation -> {
            SceneWorkReportEntity report = invocation.getArgument(0);
            report.setId(18L);
            return report;
        }).when(reportRepository).saveAndFlush(any());

        Long reportId = service.createDeviceReport(record, binding, dispatch);

        assertThat(reportId).isEqualTo(18L);
        assertThat(processTask.getQualifiedQuantity()).isEqualByComparingTo("30");
        assertThat(processTask.getTaskStatus()).isEqualTo(SceneExecutionTaskStatusEnum.COMPLETED.getStatus());
        assertThat(task.getTaskStatus()).isEqualTo(SceneExecutionTaskStatusEnum.COMPLETED.getStatus());
        verify(wageWorkRecordRepository).insertIdempotently(
                eq(18L), eq(13L), eq(record.getCollectTime().toLocalDate()),
                eq(15L), eq(12L), eq(17L), any(), any(), any(), eq(9L));
    }
}
