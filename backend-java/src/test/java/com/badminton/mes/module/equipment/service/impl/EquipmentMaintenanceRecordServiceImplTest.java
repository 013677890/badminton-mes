package com.badminton.mes.module.equipment.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.equipment.constants.EquipmentErrorCodeConstants;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenanceRecordSaveReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentLedgerEntity;
import com.badminton.mes.module.equipment.dal.entity.EquipmentMaintenancePlanEntity;
import com.badminton.mes.module.equipment.dal.entity.EquipmentMaintenanceRecordEntity;
import com.badminton.mes.module.equipment.dal.redis.EquipmentCache;
import com.badminton.mes.module.equipment.dal.redis.EquipmentRedisKeyConstants;
import com.badminton.mes.module.equipment.dal.repository.EquipmentLedgerRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentMaintenancePlanRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentMaintenanceRecordRepository;
import com.badminton.mes.module.system.dal.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link EquipmentMaintenanceRecordServiceImpl} 单元测试。
 *
 * <p>使用 Mockito 隔离保养记录、计划、设备、用户和缓存依赖，直接测试状态机编排。覆盖新任务强制
 * 初始化、开始时设备状态快照与缓存失效、完成时设备恢复和计划周期回写，以及非法跳转和终态删除
 * 拒绝；副作用断言确保跨聚合更新仅在合法迁移后发生。
 */
@ExtendWith(MockitoExtension.class)
class EquipmentMaintenanceRecordServiceImplTest {

    /** 测试保养记录主键。 */
    private static final Long RECORD_ID = 300L;

    /** 测试保养计划主键。 */
    private static final Long PLAN_ID = 200L;

    /** 计划绑定设备主键。 */
    private static final Long EQUIPMENT_ID = 100L;

    @Mock
    private EquipmentMaintenanceRecordRepository maintenanceRecordRepository;

    @Mock
    private EquipmentMaintenancePlanRepository maintenancePlanRepository;

    @Mock
    private EquipmentLedgerRepository ledgerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EquipmentCache equipmentCache;

    private EquipmentMaintenanceRecordServiceImpl maintenanceRecordService;

    /** 每个用例重建状态机服务，避免实体变更和 Mock 交互跨用例残留。 */
    @BeforeEach
    void setUp() {
        maintenanceRecordService = new EquipmentMaintenanceRecordServiceImpl(
                maintenanceRecordRepository,
                maintenancePlanRepository,
                ledgerRepository,
                userRepository,
                equipmentCache);
    }

    @Test
    @DisplayName("创建保养记录：强制初始化为待处理并清除执行结果")
    void createMaintenanceRecordInitializesPendingState() {
        EquipmentMaintenanceRecordSaveReqVO request = buildRequest("PENDING");
        request.setStartTime(LocalDateTime.now().minusHours(2));
        request.setFinishTime(LocalDateTime.now().minusHours(1));
        request.setMaintenanceResult("NORMAL");
        EquipmentMaintenancePlanEntity plan = buildPlan();
        when(maintenancePlanRepository.findByIdAndDeletedFalseForUpdate(PLAN_ID))
                .thenReturn(Optional.of(plan));
        when(ledgerRepository.findByIdAndDeletedFalseForUpdate(EQUIPMENT_ID))
                .thenReturn(Optional.of(buildEquipment("IDLE")));
        when(maintenanceRecordRepository.saveAndFlush(any(EquipmentMaintenanceRecordEntity.class)))
                .thenAnswer(invocation -> {
                    EquipmentMaintenanceRecordEntity record = invocation.getArgument(0);
                    record.setId(RECORD_ID);
                    return record;
                });

        Long createdId = maintenanceRecordService.createEquipmentMaintenanceRecord(request);

        assertThat(createdId).isEqualTo(RECORD_ID);
        ArgumentCaptor<EquipmentMaintenanceRecordEntity> recordCaptor =
                ArgumentCaptor.forClass(EquipmentMaintenanceRecordEntity.class);
        verify(maintenanceRecordRepository).saveAndFlush(recordCaptor.capture());
        EquipmentMaintenanceRecordEntity savedRecord = recordCaptor.getValue();
        assertThat(savedRecord.getEquipmentId()).isEqualTo(EQUIPMENT_ID);
        assertThat(savedRecord.getRecordStatus()).isEqualTo("PENDING");
        assertThat(savedRecord.getStartTime()).isNull();
        assertThat(savedRecord.getFinishTime()).isNull();
        assertThat(savedRecord.getMaintenanceResult()).isNull();
        assertThat(savedRecord.getCreateBy()).isEqualTo(1L);
    }

    @Test
    @DisplayName("开始保养：设备进入保养状态并失效台账缓存")
    void startMaintenanceUpdatesEquipmentAndEvictsLedgerCache() {
        EquipmentMaintenanceRecordEntity record = buildRecord("PENDING");
        EquipmentLedgerEntity equipment = buildEquipment("IDLE");
        when(maintenanceRecordRepository.findByIdAndDeletedFalseForUpdate(RECORD_ID))
                .thenReturn(Optional.of(record));
        when(maintenancePlanRepository.findByIdAndDeletedFalseForUpdate(PLAN_ID))
                .thenReturn(Optional.of(buildPlan()));
        when(ledgerRepository.findByIdAndDeletedFalseForUpdate(EQUIPMENT_ID))
                .thenReturn(Optional.of(equipment));

        maintenanceRecordService.updateEquipmentMaintenanceRecord(
                RECORD_ID, buildRequest("IN_PROGRESS"));

        assertThat(record.getRecordStatus()).isEqualTo("IN_PROGRESS");
        assertThat(record.getStartTime()).isNotNull();
        assertThat(record.getPreviousEquipmentStatus()).isEqualTo("IDLE");
        assertThat(equipment.getEquipmentStatus()).isEqualTo("MAINTAINING");
        verify(ledgerRepository).save(equipment);
        verify(maintenanceRecordRepository).saveAndFlush(record);
        verify(equipmentCache).evictDetailAfterCommit(
                EquipmentRedisKeyConstants.LEDGER_RESOURCE,
                EQUIPMENT_ID);
    }

    @Test
    @DisplayName("完成保养：恢复设备原状态并根据最新完成时间更新计划")
    void completeMaintenanceRestoresEquipmentAndUpdatesPlan() {
        LocalDateTime startTime = LocalDateTime.now().minusHours(2);
        LocalDateTime finishTime = LocalDateTime.now().minusHours(1);
        EquipmentMaintenanceRecordEntity record = buildRecord("IN_PROGRESS");
        record.setStartTime(startTime);
        record.setPreviousEquipmentStatus("STOPPED");
        EquipmentMaintenancePlanEntity plan = buildPlan();
        EquipmentLedgerEntity equipment = buildEquipment("MAINTAINING");
        EquipmentMaintenanceRecordSaveReqVO request = buildRequest("COMPLETED");
        request.setStartTime(startTime);
        request.setFinishTime(finishTime);
        request.setMaintenanceResult("NORMAL");
        when(maintenanceRecordRepository.findByIdAndDeletedFalseForUpdate(RECORD_ID))
                .thenReturn(Optional.of(record));
        when(maintenancePlanRepository.findByIdAndDeletedFalseForUpdate(PLAN_ID))
                .thenReturn(Optional.of(plan));
        when(ledgerRepository.findByIdAndDeletedFalseForUpdate(EQUIPMENT_ID))
                .thenReturn(Optional.of(equipment));
        when(maintenanceRecordRepository.findLatestCompletedTimeByPlanId(PLAN_ID))
                .thenReturn(Optional.of(finishTime));

        maintenanceRecordService.updateEquipmentMaintenanceRecord(RECORD_ID, request);

        assertThat(record.getRecordStatus()).isEqualTo("COMPLETED");
        assertThat(equipment.getEquipmentStatus()).isEqualTo("STOPPED");
        assertThat(plan.getLastMaintenanceTime()).isEqualTo(finishTime);
        assertThat(plan.getNextMaintenanceTime()).isEqualTo(finishTime.plusDays(30));
        verify(ledgerRepository).save(equipment);
        verify(maintenancePlanRepository).save(plan);
        verify(equipmentCache).evictDetailAfterCommit(
                EquipmentRedisKeyConstants.LEDGER_RESOURCE,
                EQUIPMENT_ID);
    }

    @Test
    @DisplayName("更新保养记录：禁止从待处理直接跳转到已完成")
    void updateMaintenanceRecordRejectsSkippedTransition() {
        EquipmentMaintenanceRecordEntity record = buildRecord("PENDING");
        when(maintenanceRecordRepository.findByIdAndDeletedFalseForUpdate(RECORD_ID))
                .thenReturn(Optional.of(record));
        when(maintenancePlanRepository.findByIdAndDeletedFalseForUpdate(PLAN_ID))
                .thenReturn(Optional.of(buildPlan()));
        when(ledgerRepository.findByIdAndDeletedFalseForUpdate(EQUIPMENT_ID))
                .thenReturn(Optional.of(buildEquipment("IDLE")));
        EquipmentMaintenanceRecordSaveReqVO request = buildRequest("COMPLETED");
        request.setMaintenanceResult("NORMAL");

        assertThatThrownBy(() -> maintenanceRecordService.updateEquipmentMaintenanceRecord(
                RECORD_ID, request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_STATUS_OPERATION_NOT_ALLOWED));
        verify(maintenanceRecordRepository, never()).saveAndFlush(any());
        verify(ledgerRepository, never()).save(any());
    }

    @Test
    @DisplayName("删除保养记录：已完成记录作为历史数据不可删除")
    void deleteMaintenanceRecordRejectsCompletedRecord() {
        when(maintenanceRecordRepository.findByIdAndDeletedFalseForUpdate(RECORD_ID))
                .thenReturn(Optional.of(buildRecord("COMPLETED")));

        assertThatThrownBy(() -> maintenanceRecordService.deleteEquipmentMaintenanceRecord(RECORD_ID))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_TERMINAL_RECORD_IMMUTABLE));
        verify(maintenanceRecordRepository, never()).save(any());
    }

    /** 构造指定目标状态、具备必填字段的保养请求。 */
    private EquipmentMaintenanceRecordSaveReqVO buildRequest(String recordStatus) {
        EquipmentMaintenanceRecordSaveReqVO request = new EquipmentMaintenanceRecordSaveReqVO();
        request.setRecordNo("MNT-001");
        request.setPlanId(PLAN_ID);
        request.setScheduledTime(LocalDateTime.now().minusDays(1));
        request.setMaintenanceContent("检查传动和润滑系统");
        request.setRecordStatus(recordStatus);
        return request;
    }

    /** 构造指定当前状态的有效保养记录。 */
    private EquipmentMaintenanceRecordEntity buildRecord(String recordStatus) {
        EquipmentMaintenanceRecordEntity record = new EquipmentMaintenanceRecordEntity();
        record.setId(RECORD_ID);
        record.setRecordNo("MNT-001");
        record.setPlanId(PLAN_ID);
        record.setEquipmentId(EQUIPMENT_ID);
        record.setScheduledTime(LocalDateTime.now().minusDays(1));
        record.setMaintenanceContent("检查传动和润滑系统");
        record.setRecordStatus(recordStatus);
        record.setDeleted(false);
        return record;
    }

    /** 构造已启用、周期为 30 天且绑定测试设备的计划。 */
    private EquipmentMaintenancePlanEntity buildPlan() {
        EquipmentMaintenancePlanEntity plan = new EquipmentMaintenancePlanEntity();
        plan.setId(PLAN_ID);
        plan.setEquipmentId(EQUIPMENT_ID);
        plan.setCycleDays(30);
        plan.setStatus(1);
        plan.setDeleted(false);
        return plan;
    }

    /** 构造指定运行状态的有效设备台账。 */
    private EquipmentLedgerEntity buildEquipment(String equipmentStatus) {
        EquipmentLedgerEntity equipment = new EquipmentLedgerEntity();
        equipment.setId(EQUIPMENT_ID);
        equipment.setEquipmentStatus(equipmentStatus);
        equipment.setStatus(1);
        equipment.setDeleted(false);
        return equipment;
    }
}
