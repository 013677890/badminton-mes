package com.badminton.mes.group.c;

import com.badminton.mes.module.andon.convert.AndonTypeConvert;
import com.badminton.mes.module.andon.dal.entity.AndonTypeEntity;
import com.badminton.mes.module.device.convert.DeviceAccessConfigConvert;
import com.badminton.mes.module.device.dal.entity.DeviceAccessConfigEntity;
import com.badminton.mes.module.equipment.convert.EquipmentManufacturerConvert;
import com.badminton.mes.module.equipment.dal.entity.EquipmentManufacturerEntity;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionPlanSaveReqVO;
import com.badminton.mes.module.quality.convert.QualityInspectionPlanConvert;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** C 组边界条件维度：空集合、可选字段和响应脱敏。 @author 范家权 */
class CTeamBoundaryTest {

    @Test
    void equipmentManufacturerEmptyListConvertsToEmptyList() {
        assertThat(EquipmentManufacturerConvert.toRespVOList(List.of())).isEmpty();
    }

    @Test
    void deviceAccessEmptyListConvertsToEmptyList() {
        assertThat(DeviceAccessConfigConvert.toRespVOList(List.of())).isEmpty();
    }

    @Test
    void qualityPlanNullDefaultFlagNormalizesToFalse() {
        QualityInspectionPlanSaveReqVO request = new QualityInspectionPlanSaveReqVO();
        request.setPlanCode("PLAN-01");
        request.setPlanName("首件");
        request.setInspectionType("FIRST_ARTICLE");

        assertThat(QualityInspectionPlanConvert.toEntity(request).getDefaultFlag()).isFalse();
    }

    @Test
    void responseConversionDoesNotExposeDeletionFlag() {
        AndonTypeEntity type = new AndonTypeEntity();
        type.setId(9L);
        type.setTypeCode("QUALITY");
        type.setTypeName("质量异常");
        type.setDeleted(true);

        assertThat(AndonTypeConvert.toRespVO(type))
                .extracting("id", "typeCode", "typeName")
                .containsExactly(9L, "QUALITY", "质量异常");
    }

    @Test
    void deviceAndEquipmentResponseConversionKeepsNullableOptionalFields() {
        DeviceAccessConfigEntity config = new DeviceAccessConfigEntity();
        EquipmentManufacturerEntity manufacturer = new EquipmentManufacturerEntity();
        assertThat(DeviceAccessConfigConvert.toRespVO(config).getDataSource()).isNull();
        assertThat(EquipmentManufacturerConvert.toRespVO(manufacturer).getManufacturerCode()).isNull();
    }
}
