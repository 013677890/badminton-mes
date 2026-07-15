package com.badminton.mes.module.equipment.service.impl;

import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.equipment.constants.EquipmentErrorCodeConstants;
import com.badminton.mes.module.equipment.controller.vo.EquipmentLedgerSaveReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentCategoryEntity;
import com.badminton.mes.module.equipment.dal.entity.EquipmentLedgerEntity;
import com.badminton.mes.module.equipment.dal.redis.EquipmentCache;
import com.badminton.mes.module.equipment.dal.redis.EquipmentRedisKeyConstants;
import com.badminton.mes.module.equipment.dal.repository.EquipmentCategoryRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentLedgerRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentMaintenancePlanRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentMaintenanceRecordRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentManufacturerRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentRepairOrderRepository;

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
 * {@link EquipmentLedgerServiceImpl} 单元测试。
 *
 * <p>使用 Mockito 隔离台账及其类别、制造商、报修、保养和缓存依赖，直接构造被测 Service，不启动
 * Spring 容器。重点覆盖创建默认值、保养状态保护、删除前跨聚合引用校验、逻辑删除编码改写，以及
 * 成功写入后的详情缓存失效；异常分支同时验证持久化和缓存副作用均未发生。
 */
@ExtendWith(MockitoExtension.class)
class EquipmentLedgerServiceImplTest {

    /** 各用例复用的设备主键。 */
    private static final Long EQUIPMENT_ID = 100L;

    /** 测试设备绑定的有效类别主键。 */
    private static final Long CATEGORY_ID = 10L;

    @Mock
    private EquipmentLedgerRepository ledgerRepository;

    @Mock
    private EquipmentCategoryRepository categoryRepository;

    @Mock
    private EquipmentManufacturerRepository manufacturerRepository;

    @Mock
    private EquipmentRepairOrderRepository repairOrderRepository;

    @Mock
    private EquipmentMaintenancePlanRepository maintenancePlanRepository;

    @Mock
    private EquipmentMaintenanceRecordRepository maintenanceRecordRepository;

    @Mock
    private EquipmentCache equipmentCache;

    private EquipmentLedgerServiceImpl ledgerService;

    /** 每个用例重新构造 Service，避免可变实体状态跨测试泄漏。 */
    @BeforeEach
    void setUp() {
        ledgerService = new EquipmentLedgerServiceImpl(
                ledgerRepository,
                categoryRepository,
                manufacturerRepository,
                repairOrderRepository,
                maintenancePlanRepository,
                maintenanceRecordRepository,
                equipmentCache);
    }

    @Test
    @DisplayName("创建设备台账：默认启用且设备状态为空闲")
    void createEquipmentLedgerAppliesDefaultStatuses() {
        EquipmentLedgerSaveReqVO request = buildSaveRequest();
        request.setStatus(null);
        request.setEquipmentStatus(null);
        when(categoryRepository.findByIdAndDeletedFalse(CATEGORY_ID))
                .thenReturn(Optional.of(new EquipmentCategoryEntity()));
        when(ledgerRepository.saveAndFlush(any(EquipmentLedgerEntity.class)))
                .thenAnswer(invocation -> {
                    EquipmentLedgerEntity ledger = invocation.getArgument(0);
                    ledger.setId(EQUIPMENT_ID);
                    return ledger;
                });

        Long createdId = ledgerService.createEquipmentLedger(request);

        assertThat(createdId).isEqualTo(EQUIPMENT_ID);
        ArgumentCaptor<EquipmentLedgerEntity> ledgerCaptor =
                ArgumentCaptor.forClass(EquipmentLedgerEntity.class);
        verify(ledgerRepository).saveAndFlush(ledgerCaptor.capture());
        assertThat(ledgerCaptor.getValue().getStatus()).isEqualTo(1);
        assertThat(ledgerCaptor.getValue().getEquipmentStatus()).isEqualTo("IDLE");
        assertThat(ledgerCaptor.getValue().getCreateBy()).isEqualTo(1L);
    }

    @Test
    @DisplayName("修改设备台账：保养进行中时不允许退出保养状态")
    void updateEquipmentLedgerRejectsLeavingMaintenanceStatus() {
        EquipmentLedgerEntity existingLedger = buildLedger("MAINTAINING");
        EquipmentLedgerSaveReqVO request = buildSaveRequest();
        request.setEquipmentStatus("IDLE");
        when(ledgerRepository.findByIdAndDeletedFalseForUpdate(EQUIPMENT_ID))
                .thenReturn(Optional.of(existingLedger));
        when(categoryRepository.findByIdAndDeletedFalse(CATEGORY_ID))
                .thenReturn(Optional.of(new EquipmentCategoryEntity()));
        when(maintenanceRecordRepository.countByEquipmentIdAndRecordStatusInAndDeletedFalse(
                any(), any())).thenReturn(1L);

        assertThatThrownBy(() -> ledgerService.updateEquipmentLedger(EQUIPMENT_ID, request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                EquipmentErrorCodeConstants.EQUIPMENT_STATUS_OPERATION_NOT_ALLOWED));
        verify(ledgerRepository, never()).save(any());
        verify(equipmentCache, never()).evictDetailAfterCommit(any(), any());
    }

    @Test
    @DisplayName("修改设备台账：保存成功后失效详情缓存")
    void updateEquipmentLedgerEvictsDetailCache() {
        EquipmentLedgerEntity existingLedger = buildLedger("IDLE");
        EquipmentLedgerSaveReqVO request = buildSaveRequest();
        request.setEquipmentStatus("STOPPED");
        when(ledgerRepository.findByIdAndDeletedFalseForUpdate(EQUIPMENT_ID))
                .thenReturn(Optional.of(existingLedger));
        when(categoryRepository.findByIdAndDeletedFalse(CATEGORY_ID))
                .thenReturn(Optional.of(new EquipmentCategoryEntity()));

        ledgerService.updateEquipmentLedger(EQUIPMENT_ID, request);

        assertThat(existingLedger.getEquipmentStatus()).isEqualTo("STOPPED");
        verify(ledgerRepository).save(existingLedger);
        verify(equipmentCache).evictDetailAfterCommit(
                EquipmentRedisKeyConstants.LEDGER_RESOURCE,
                EQUIPMENT_ID);
    }

    @Test
    @DisplayName("删除设备台账：存在活动报修单时拒绝删除")
    void deleteEquipmentLedgerRejectsActiveRepairOrder() {
        EquipmentLedgerEntity existingLedger = buildLedger("STOPPED");
        when(ledgerRepository.findByIdAndDeletedFalseForUpdate(EQUIPMENT_ID))
                .thenReturn(Optional.of(existingLedger));
        when(repairOrderRepository.countByEquipmentIdAndRepairStatusInAndDeletedFalse(
                any(), any())).thenReturn(1L);

        assertThatThrownBy(() -> ledgerService.deleteEquipmentLedger(EQUIPMENT_ID))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                EquipmentErrorCodeConstants.EQUIPMENT_LEDGER_HAS_REPAIR_ORDER));
        verify(ledgerRepository, never()).save(any());
    }

    @Test
    @DisplayName("删除设备台账：无业务引用时重命名并清除详情缓存")
    void deleteEquipmentLedgerRenamesCodeAndEvictsCache() {
        EquipmentLedgerEntity existingLedger = buildLedger("STOPPED");
        when(ledgerRepository.findByIdAndDeletedFalseForUpdate(EQUIPMENT_ID))
                .thenReturn(Optional.of(existingLedger));

        ledgerService.deleteEquipmentLedger(EQUIPMENT_ID);

        assertThat(existingLedger.getEquipmentCode()).isEqualTo("__DELETED_2S");
        assertThat(existingLedger.getDeleted()).isTrue();
        verify(ledgerRepository).save(existingLedger);
        verify(equipmentCache).evictDetailAfterCommit(
                EquipmentRedisKeyConstants.LEDGER_RESOURCE,
                EQUIPMENT_ID);
    }

    /** 构造通过基础业务校验的台账保存请求。 */
    private EquipmentLedgerSaveReqVO buildSaveRequest() {
        EquipmentLedgerSaveReqVO request = new EquipmentLedgerSaveReqVO();
        request.setEquipmentCode("EQ-001");
        request.setEquipmentName("一号成型设备");
        request.setCategoryId(CATEGORY_ID);
        request.setEquipmentStatus("IDLE");
        request.setStatus(1);
        return request;
    }

    /** 构造指定运行状态的有效设备台账实体。 */
    private EquipmentLedgerEntity buildLedger(String equipmentStatus) {
        EquipmentLedgerEntity ledger = new EquipmentLedgerEntity();
        ledger.setId(EQUIPMENT_ID);
        ledger.setEquipmentCode("EQ-001");
        ledger.setEquipmentName("一号成型设备");
        ledger.setCategoryId(CATEGORY_ID);
        ledger.setEquipmentStatus(equipmentStatus);
        ledger.setStatus(1);
        ledger.setDeleted(false);
        return ledger;
    }
}
