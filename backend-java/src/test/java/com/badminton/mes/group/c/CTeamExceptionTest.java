package com.badminton.mes.group.c;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.andon.constants.AndonErrorCodeConstants;
import com.badminton.mes.module.andon.controller.vo.AndonTypeSaveReqVO;
import com.badminton.mes.module.andon.dal.repository.AndonTypeRepository;
import com.badminton.mes.module.andon.service.impl.AndonTypeServiceImpl;
import com.badminton.mes.module.quality.constants.QualityErrorCodeConstants;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionPlanSaveReqVO;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionItemRepository;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionPlanItemRepository;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionPlanRepository;
import com.badminton.mes.module.quality.service.impl.QualityInspectionPlanServiceImpl;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionItemSaveReqVO;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionItemRepository;
import com.badminton.mes.module.quality.service.impl.QualityInspectionItemServiceImpl;
import com.badminton.mes.module.device.constants.DeviceErrorCodeConstants;
import com.badminton.mes.module.device.controller.vo.DeviceAccessConfigSaveReqVO;
import com.badminton.mes.module.device.dal.repository.DeviceAccessConfigRepository;
import com.badminton.mes.module.device.dal.repository.DeviceCommissioningRecordRepository;
import com.badminton.mes.module.device.dal.repository.DeviceCountRecordRepository;
import com.badminton.mes.module.device.service.impl.DeviceAccessConfigServiceImpl;
import com.badminton.mes.module.equipment.constants.EquipmentErrorCodeConstants;
import com.badminton.mes.module.equipment.controller.vo.EquipmentFaultPrincipleSaveReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentLedgerSaveReqVO;
import com.badminton.mes.module.equipment.dal.repository.EquipmentCategoryRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentFaultPrincipleRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentLedgerRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentRepairOrderRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentManufacturerRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentMaintenancePlanRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentMaintenanceRecordRepository;
import com.badminton.mes.module.equipment.service.impl.EquipmentFaultPrincipleServiceImpl;
import com.badminton.mes.module.equipment.service.impl.EquipmentLedgerServiceImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** C 组异常维度：安灯规则和质量方案重复约束。 @author 范家权 */
class CTeamExceptionTest {

    @Test
    void assistanceAndonTypeRequiresResponseOwnerAndNotification() {
        AndonTypeRepository repository = mock(AndonTypeRepository.class);
        when(repository.existsByTypeCodeAndDeletedFalse("STOP")).thenReturn(false);
        AndonTypeSaveReqVO request = new AndonTypeSaveReqVO();
        request.setTypeCode("STOP");
        request.setTypeName("停机");
        request.setHandlingMode("ASSISTANCE");

        assertThatThrownBy(() -> new AndonTypeServiceImpl(repository).createType(request))
                .isInstanceOfSatisfying(ServiceException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isSameAs(AndonErrorCodeConstants.TYPE_RULE_INVALID));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void qualityPlanDuplicateCodeIsRejectedBeforeItemValidation() {
        QualityInspectionPlanRepository planRepository = mock(QualityInspectionPlanRepository.class);
        QualityInspectionPlanItemRepository itemRepository = mock(QualityInspectionPlanItemRepository.class);
        QualityInspectionItemRepository inspectionRepository = mock(QualityInspectionItemRepository.class);
        when(planRepository.existsByPlanCodeAndVersionNoAndDeletedFalse("PLAN-01", 1)).thenReturn(true);
        QualityInspectionPlanSaveReqVO request = new QualityInspectionPlanSaveReqVO();
        request.setPlanCode("PLAN-01");
        request.setItems(java.util.List.of());

        assertThatThrownBy(() -> new QualityInspectionPlanServiceImpl(
                planRepository, itemRepository, inspectionRepository).createPlan(request))
                .isInstanceOfSatisfying(ServiceException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isSameAs(QualityErrorCodeConstants.PLAN_CODE_DUPLICATE));
        verify(inspectionRepository, never()).findAllById(any());
    }

    @Test
    void qualityItemDuplicateCodeIsRejectedBeforeCategoryValidation() {
        QualityInspectionItemRepository repository = mock(QualityInspectionItemRepository.class);
        when(repository.existsByItemCodeAndDeletedFalse("ITEM-01")).thenReturn(true);
        QualityInspectionItemSaveReqVO request = new QualityInspectionItemSaveReqVO();
        request.setItemCode("ITEM-01");

        assertThatThrownBy(() -> new QualityInspectionItemServiceImpl(repository, mock(
                com.badminton.mes.module.quality.dal.repository.QualityInspectionCategoryRepository.class), mock(
                com.badminton.mes.module.quality.dal.repository.QualityInspectionPlanItemRepository.class))
                .createItem(request))
                .isInstanceOfSatisfying(ServiceException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isSameAs(QualityErrorCodeConstants.ITEM_CODE_DUPLICATE));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deviceAccessConfigDuplicateCodeIsRejectedBeforeEquipmentLookup() {
        DeviceAccessConfigRepository repository = mock(DeviceAccessConfigRepository.class);
        when(repository.existsByConfigCodeAndDeletedFalse("CFG-01")).thenReturn(true);
        DeviceAccessConfigSaveReqVO request = new DeviceAccessConfigSaveReqVO();
        request.setConfigCode("CFG-01");

        assertThatThrownBy(() -> new DeviceAccessConfigServiceImpl(repository,
                mock(DeviceCommissioningRecordRepository.class), mock(DeviceCountRecordRepository.class),
                mock(com.badminton.mes.module.equipment.service.EquipmentLedgerService.class))
                .createAccessConfig(request))
                .isInstanceOfSatisfying(ServiceException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isSameAs(DeviceErrorCodeConstants.ACCESS_CONFIG_CODE_DUPLICATE));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void equipmentFaultDuplicateCodeIsRejectedBeforeCategoryLookup() {
        EquipmentFaultPrincipleRepository repository = mock(EquipmentFaultPrincipleRepository.class);
        when(repository.existsByFaultCodeAndDeletedFalse("FAULT-01")).thenReturn(true);
        EquipmentFaultPrincipleSaveReqVO request = new EquipmentFaultPrincipleSaveReqVO();
        request.setFaultCode("FAULT-01");

        assertThatThrownBy(() -> new EquipmentFaultPrincipleServiceImpl(repository,
                mock(EquipmentCategoryRepository.class), mock(EquipmentRepairOrderRepository.class))
                .createEquipmentFaultPrinciple(request))
                .isInstanceOfSatisfying(ServiceException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isSameAs(EquipmentErrorCodeConstants.EQUIPMENT_FAULT_PRINCIPLE_CODE_DUPLICATE));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void equipmentLedgerDuplicateCodeIsRejectedBeforeCategoryAndManufacturerLookup() {
        EquipmentLedgerRepository repository = mock(EquipmentLedgerRepository.class);
        when(repository.existsByEquipmentCodeAndDeletedFalse("EQ-01")).thenReturn(true);
        EquipmentLedgerSaveReqVO request = new EquipmentLedgerSaveReqVO();
        request.setEquipmentCode("EQ-01");

        assertThatThrownBy(() -> new EquipmentLedgerServiceImpl(repository,
                mock(EquipmentCategoryRepository.class), mock(EquipmentManufacturerRepository.class),
                mock(EquipmentRepairOrderRepository.class), mock(EquipmentMaintenancePlanRepository.class),
                mock(EquipmentMaintenanceRecordRepository.class)).createEquipmentLedger(request))
                .isInstanceOfSatisfying(ServiceException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isSameAs(EquipmentErrorCodeConstants.EQUIPMENT_LEDGER_CODE_DUPLICATE));
        verify(repository, never()).saveAndFlush(any());
    }
}
