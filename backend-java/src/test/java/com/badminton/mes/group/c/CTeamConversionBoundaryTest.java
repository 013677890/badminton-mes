package com.badminton.mes.group.c;

import com.badminton.mes.module.device.convert.DeviceCommissioningConvert;
import com.badminton.mes.module.equipment.convert.EquipmentFaultPrincipleConvert;
import com.badminton.mes.module.equipment.convert.EquipmentLedgerConvert;
import com.badminton.mes.module.equipment.convert.EquipmentMaintenancePlanConvert;
import com.badminton.mes.module.equipment.convert.EquipmentMaintenanceRecordConvert;
import com.badminton.mes.module.equipment.convert.EquipmentRepairOrderConvert;
import com.badminton.mes.module.quality.convert.QualityInspectionCategoryConvert;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** C 组扩展边界维度：验证所有列表转换器的空集合契约。 @author 范家权 */
class CTeamConversionBoundaryTest {

    @Test
    void equipmentListConvertersReturnEmptyCollections() {
        assertThat(EquipmentFaultPrincipleConvert.toRespVOList(List.of())).isEmpty();
        assertThat(EquipmentLedgerConvert.toRespVOList(List.of())).isEmpty();
        assertThat(EquipmentMaintenancePlanConvert.toRespVOList(List.of())).isEmpty();
        assertThat(EquipmentMaintenanceRecordConvert.toRespVOList(List.of())).isEmpty();
        assertThat(EquipmentRepairOrderConvert.toRespVOList(List.of())).isEmpty();
    }

    @Test
    void deviceAndQualityListConvertersReturnEmptyCollections() {
        assertThat(DeviceCommissioningConvert.toRespVOList(List.of())).isEmpty();
        assertThat(QualityInspectionCategoryConvert.toRespVOList(List.of())).isEmpty();
    }
}
