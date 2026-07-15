package com.badminton.mes.module.equipment.service.impl;

import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.equipment.constants.EquipmentErrorCodeConstants;
import com.badminton.mes.module.equipment.controller.vo.EquipmentRepairOrderSaveReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentFaultPrincipleEntity;
import com.badminton.mes.module.equipment.dal.entity.EquipmentLedgerEntity;
import com.badminton.mes.module.equipment.dal.entity.EquipmentRepairOrderEntity;
import com.badminton.mes.module.equipment.dal.repository.EquipmentFaultPrincipleRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentLedgerRepository;
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
 * {@link EquipmentRepairOrderServiceImpl} 单元测试。
 *
 * <p>使用 Mockito 隔离报修单、设备台账和故障原理持久层，直接构造被测 Service。重点覆盖创建默认
 * 字段、故障原理与设备类别的跨聚合匹配、维修状态机及维修中删除保护；非法分支验证不会触发保存，
 * 从而同时确认异常路径没有持久化副作用。
 */
@ExtendWith(MockitoExtension.class)
class EquipmentRepairOrderServiceImplTest {

    /** 测试报修单主键。 */
    private static final Long REPAIR_ORDER_ID = 300L;

    /** 报修设备主键。 */
    private static final Long EQUIPMENT_ID = 100L;

    /** 报修设备所属类别主键。 */
    private static final Long CATEGORY_ID = 10L;

    /** 用于适用类别校验的故障原理主键。 */
    private static final Long FAULT_PRINCIPLE_ID = 200L;

    @Mock
    private EquipmentRepairOrderRepository repairOrderRepository;

    @Mock
    private EquipmentLedgerRepository ledgerRepository;

    @Mock
    private EquipmentFaultPrincipleRepository faultPrincipleRepository;

    private EquipmentRepairOrderServiceImpl repairOrderService;

    /** 每个用例重建被测 Service，保持 Mock 调用历史独立。 */
    @BeforeEach
    void setUp() {
        repairOrderService = new EquipmentRepairOrderServiceImpl(
                repairOrderRepository,
                ledgerRepository,
                faultPrincipleRepository);
    }

    @Test
    @DisplayName("创建报修单：自动补充单号、报修时间、报修人和初始状态")
    void createRepairOrderAppliesDefaultFields() {
        EquipmentRepairOrderSaveReqVO request = buildRequest(null);
        when(ledgerRepository.findByIdAndDeletedFalseForUpdate(EQUIPMENT_ID))
                .thenReturn(Optional.of(buildEquipment("IDLE")));
        when(repairOrderRepository.saveAndFlush(any(EquipmentRepairOrderEntity.class)))
                .thenAnswer(invocation -> {
                    EquipmentRepairOrderEntity repairOrder = invocation.getArgument(0);
                    repairOrder.setId(REPAIR_ORDER_ID);
                    return repairOrder;
                });

        Long createdId = repairOrderService.createEquipmentRepairOrder(request);

        assertThat(createdId).isEqualTo(REPAIR_ORDER_ID);
        ArgumentCaptor<EquipmentRepairOrderEntity> repairOrderCaptor =
                ArgumentCaptor.forClass(EquipmentRepairOrderEntity.class);
        verify(repairOrderRepository).saveAndFlush(repairOrderCaptor.capture());
        EquipmentRepairOrderEntity savedRepairOrder = repairOrderCaptor.getValue();
        assertThat(savedRepairOrder.getRepairNo()).startsWith("REP-");
        assertThat(savedRepairOrder.getReportTime()).isNotNull();
        assertThat(savedRepairOrder.getReportUserId()).isEqualTo(1L);
        assertThat(savedRepairOrder.getRepairStatus()).isEqualTo("REPORTED");
        assertThat(savedRepairOrder.getCreateBy()).isEqualTo(1L);
    }

    @Test
    @DisplayName("创建报修单：故障原理与设备类别不匹配时拒绝创建")
    void createRepairOrderRejectsFaultPrincipleCategoryMismatch() {
        EquipmentRepairOrderSaveReqVO request = buildRequest("REPORTED");
        request.setFaultPrincipleId(FAULT_PRINCIPLE_ID);
        EquipmentFaultPrincipleEntity faultPrinciple = new EquipmentFaultPrincipleEntity();
        faultPrinciple.setId(FAULT_PRINCIPLE_ID);
        faultPrinciple.setCategoryId(99L);
        when(ledgerRepository.findByIdAndDeletedFalseForUpdate(EQUIPMENT_ID))
                .thenReturn(Optional.of(buildEquipment("IDLE")));
        when(faultPrincipleRepository.findByIdAndDeletedFalseForUpdate(FAULT_PRINCIPLE_ID))
                .thenReturn(Optional.of(faultPrinciple));

        assertThatThrownBy(() -> repairOrderService.createEquipmentRepairOrder(request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                EquipmentErrorCodeConstants.EQUIPMENT_FAULT_PRINCIPLE_CATEGORY_NOT_MATCH));
        verify(repairOrderRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("更新报修单：进入维修中状态时自动补充维修开始时间")
    void updateRepairOrderEnteringRepairingSetsStartTime() {
        EquipmentRepairOrderEntity repairOrder = buildRepairOrder("REPORTED");
        when(repairOrderRepository.findByIdAndDeletedFalse(REPAIR_ORDER_ID))
                .thenReturn(Optional.of(repairOrder));
        when(ledgerRepository.findByIdAndDeletedFalseForUpdate(EQUIPMENT_ID))
                .thenReturn(Optional.of(buildEquipment("IDLE")));

        repairOrderService.updateEquipmentRepairOrder(
                REPAIR_ORDER_ID, buildRequest("REPAIRING"));

        assertThat(repairOrder.getRepairStatus()).isEqualTo("REPAIRING");
        assertThat(repairOrder.getRepairStartTime()).isNotNull();
        verify(repairOrderRepository).saveAndFlush(repairOrder);
    }

    @Test
    @DisplayName("更新报修单：已完成状态不允许重新进入维修中")
    void updateRepairOrderRejectsTransitionFromFinished() {
        EquipmentRepairOrderEntity repairOrder = buildRepairOrder("FINISHED");
        when(repairOrderRepository.findByIdAndDeletedFalse(REPAIR_ORDER_ID))
                .thenReturn(Optional.of(repairOrder));
        when(ledgerRepository.findByIdAndDeletedFalseForUpdate(EQUIPMENT_ID))
                .thenReturn(Optional.of(buildEquipment("IDLE")));

        assertThatThrownBy(() -> repairOrderService.updateEquipmentRepairOrder(
                REPAIR_ORDER_ID, buildRequest("REPAIRING")))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                EquipmentErrorCodeConstants.EQUIPMENT_REPAIR_STATUS_OPERATION_NOT_ALLOWED));
        verify(repairOrderRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("删除报修单：维修中任务不允许删除")
    void deleteRepairOrderRejectsRepairingOrder() {
        when(repairOrderRepository.findByIdAndDeletedFalse(REPAIR_ORDER_ID))
                .thenReturn(Optional.of(buildRepairOrder("REPAIRING")));

        assertThatThrownBy(() -> repairOrderService.deleteEquipmentRepairOrder(REPAIR_ORDER_ID))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                EquipmentErrorCodeConstants.EQUIPMENT_REPAIR_STATUS_OPERATION_NOT_ALLOWED));
        verify(repairOrderRepository, never()).save(any());
    }

    /** 构造指定目标状态的合法报修保存请求。 */
    private EquipmentRepairOrderSaveReqVO buildRequest(String repairStatus) {
        EquipmentRepairOrderSaveReqVO request = new EquipmentRepairOrderSaveReqVO();
        request.setEquipmentId(EQUIPMENT_ID);
        request.setFaultDescription("传动轴异常振动");
        request.setRepairStatus(repairStatus);
        return request;
    }

    /** 构造指定当前状态的有效报修实体。 */
    private EquipmentRepairOrderEntity buildRepairOrder(String repairStatus) {
        EquipmentRepairOrderEntity repairOrder = new EquipmentRepairOrderEntity();
        repairOrder.setId(REPAIR_ORDER_ID);
        repairOrder.setRepairNo("REP-001");
        repairOrder.setEquipmentId(EQUIPMENT_ID);
        repairOrder.setFaultDescription("传动轴异常振动");
        repairOrder.setRepairStatus(repairStatus);
        repairOrder.setDeleted(false);
        return repairOrder;
    }

    /** 构造指定运行状态、类别固定且未删除的设备实体。 */
    private EquipmentLedgerEntity buildEquipment(String equipmentStatus) {
        EquipmentLedgerEntity equipment = new EquipmentLedgerEntity();
        equipment.setId(EQUIPMENT_ID);
        equipment.setCategoryId(CATEGORY_ID);
        equipment.setEquipmentStatus(equipmentStatus);
        equipment.setStatus(1);
        equipment.setDeleted(false);
        return equipment;
    }
}
