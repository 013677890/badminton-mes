package com.badminton.mes.group.c;

import com.badminton.mes.module.andon.controller.vo.AndonConfigurationSaveReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonReasonSaveReqVO;
import com.badminton.mes.module.andon.convert.AndonConfigurationConvert;
import com.badminton.mes.module.andon.convert.AndonReasonConvert;
import com.badminton.mes.module.device.controller.vo.DeviceCommissioningSaveReqVO;
import com.badminton.mes.module.device.convert.DeviceCommissioningConvert;
import com.badminton.mes.module.device.dal.entity.DeviceCountExceptionEntity;
import com.badminton.mes.module.device.convert.DeviceCountExceptionConvert;
import com.badminton.mes.module.equipment.controller.vo.EquipmentFaultPrincipleSaveReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentLedgerSaveReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenancePlanSaveReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenanceRecordSaveReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentRepairOrderSaveReqVO;
import com.badminton.mes.module.equipment.convert.EquipmentFaultPrincipleConvert;
import com.badminton.mes.module.equipment.convert.EquipmentLedgerConvert;
import com.badminton.mes.module.equipment.convert.EquipmentMaintenancePlanConvert;
import com.badminton.mes.module.equipment.convert.EquipmentMaintenanceRecordConvert;
import com.badminton.mes.module.equipment.convert.EquipmentRepairOrderConvert;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionItemSaveReqVO;
import com.badminton.mes.module.quality.convert.QualityInspectionItemConvert;
import com.badminton.mes.module.quality.convert.QualityInspectionRecordConvert;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionCategoryEntity;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionItemEntity;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionRecordEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** C 组扩展正常维度：覆盖设备、接入、质量和安灯剩余转换契约。 @author 范家权 */
class CTeamConversionNormalPathTest {

    @Test
    void equipmentFaultLedgerAndMaintenanceRequestsConvert() {
        EquipmentFaultPrincipleSaveReqVO fault = new EquipmentFaultPrincipleSaveReqVO();
        fault.setFaultCode("FAULT-01");
        fault.setCategoryId(1L);
        EquipmentLedgerSaveReqVO ledger = new EquipmentLedgerSaveReqVO();
        ledger.setEquipmentCode("EQ-01");
        ledger.setCategoryId(1L);
        EquipmentMaintenancePlanSaveReqVO plan = new EquipmentMaintenancePlanSaveReqVO();
        plan.setPlanCode("PLAN-01");
        plan.setEquipmentId(2L);
        EquipmentMaintenanceRecordSaveReqVO record = new EquipmentMaintenanceRecordSaveReqVO();
        record.setRecordNo("RECORD-01");
        record.setPlanId(3L);

        assertThat(EquipmentFaultPrincipleConvert.toEntity(fault).getFaultCode()).isEqualTo("FAULT-01");
        assertThat(EquipmentLedgerConvert.toEntity(ledger).getEquipmentCode()).isEqualTo("EQ-01");
        assertThat(EquipmentMaintenancePlanConvert.toEntity(plan).getPlanCode()).isEqualTo("PLAN-01");
        assertThat(EquipmentMaintenanceRecordConvert.toEntity(record).getRecordNo()).isEqualTo("RECORD-01");
    }

    @Test
    void equipmentRepairRequestCarriesEquipmentAndFaultReferences() {
        EquipmentRepairOrderSaveReqVO request = new EquipmentRepairOrderSaveReqVO();
        request.setRepairNo("REPAIR-01");
        request.setEquipmentId(9L);
        request.setFaultPrincipleId(10L);

        assertThat(EquipmentRepairOrderConvert.toEntity(request))
                .extracting("repairNo", "equipmentId", "faultPrincipleId")
                .containsExactly("REPAIR-01", 9L, 10L);
    }

    @Test
    void deviceCommissioningAndExceptionResponsesKeepTraceIds() {
        DeviceCommissioningSaveReqVO commissioning = new DeviceCommissioningSaveReqVO();
        commissioning.setAccessConfigId(12L);
        DeviceCountExceptionEntity exception = new DeviceCountExceptionEntity();
        exception.setId(13L);
        exception.setCountRecordId(14L);

        assertThat(DeviceCommissioningConvert.toEntity(commissioning).getAccessConfigId()).isEqualTo(12L);
        assertThat(DeviceCountExceptionConvert.toRespVO(exception))
                .extracting("id", "countRecordId")
                .containsExactly(13L, 14L);
    }

    @Test
    void qualityItemAndRecordResponsesIncludeCategoryAndResultCollections() {
        QualityInspectionItemSaveReqVO request = new QualityInspectionItemSaveReqVO();
        request.setItemCode("WEIGHT");
        request.setCategoryId(4L);
        QualityInspectionItemEntity item = QualityInspectionItemConvert.toEntity(request);
        QualityInspectionCategoryEntity category = new QualityInspectionCategoryEntity();
        category.setCategoryCode("APPEARANCE");
        QualityInspectionRecordEntity record = new QualityInspectionRecordEntity();
        record.setId(20L);
        record.setInspectionNo("INS-20");

        assertThat(QualityInspectionItemConvert.toRespVO(item, category).getCategoryCode())
                .isEqualTo("APPEARANCE");
        assertThat(QualityInspectionRecordConvert.toRespVO(record, java.util.List.of()).getResults())
                .isEmpty();
    }

    @Test
    void andonConfigurationAndReasonResponsesUseTypeSnapshot() {
        AndonConfigurationSaveReqVO configuration = new AndonConfigurationSaveReqVO();
        configuration.setAndonTypeId(5L);
        configuration.setProductionLineId(null);
        AndonReasonSaveReqVO reason = new AndonReasonSaveReqVO();
        reason.setReasonCode("REASON-01");
        reason.setAndonTypeId(5L);

        assertThat(AndonConfigurationConvert.toEntity(configuration).getScopeLineId()).isZero();
        assertThat(AndonReasonConvert.toEntity(reason).getReasonCode()).isEqualTo("REASON-01");
    }
}
