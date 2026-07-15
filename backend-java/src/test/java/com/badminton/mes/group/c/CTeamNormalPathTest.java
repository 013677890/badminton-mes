package com.badminton.mes.group.c;

import com.badminton.mes.module.andon.controller.vo.AndonTypeSaveReqVO;
import com.badminton.mes.module.andon.convert.AndonTypeConvert;
import com.badminton.mes.module.device.controller.vo.DeviceAccessConfigSaveReqVO;
import com.badminton.mes.module.device.convert.DeviceAccessConfigConvert;
import com.badminton.mes.module.equipment.controller.vo.EquipmentCategorySaveReqVO;
import com.badminton.mes.module.equipment.convert.EquipmentCategoryConvert;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionCategorySaveReqVO;
import com.badminton.mes.module.quality.convert.QualityInspectionCategoryConvert;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** C 组正常路径维度：设备、设备接入、质量和安灯基础功能。 @author 范家权 */
class CTeamNormalPathTest {

    @Test
    void equipmentCategoryCopiesHierarchyAndStatus() {
        EquipmentCategorySaveReqVO request = new EquipmentCategorySaveReqVO();
        request.setCategoryCode("CAT-01");
        request.setCategoryName("测试设备");
        request.setParentId(3L);
        request.setSortOrder(2);
        request.setStatus(1);

        assertThat(EquipmentCategoryConvert.toEntity(request))
                .extracting("categoryCode", "parentId", "sortOrder", "status")
                .containsExactly("CAT-01", 3L, 2, 1);
    }

    @Test
    void deviceAccessConfigCopiesTrustedConfigurationFields() {
        DeviceAccessConfigSaveReqVO request = new DeviceAccessConfigSaveReqVO();
        request.setConfigCode("CFG-01");
        request.setConfigName("计数点");
        request.setEquipmentId(11L);
        request.setCollectionPointCode("POINT-A");
        request.setProcessId(21L);
        request.setProductionLineId(31L);
        request.setCountMode("CUMULATIVE");
        request.setEnabledStatus(1);

        assertThat(DeviceAccessConfigConvert.toEntity(request))
                .extracting("configCode", "equipmentId", "collectionPointCode", "processId")
                .containsExactly("CFG-01", 11L, "POINT-A", 21L);
    }

    @Test
    void qualityCategoryCopiesEditableFields() {
        QualityInspectionCategorySaveReqVO request = new QualityInspectionCategorySaveReqVO();
        request.setCategoryCode("QC-01");
        request.setCategoryName("外观");
        request.setEnabledStatus(1);
        request.setRemark("基础检验分类");

        assertThat(QualityInspectionCategoryConvert.toEntity(request))
                .extracting("categoryCode", "categoryName", "enabledStatus", "remark")
                .containsExactly("QC-01", "外观", 1, "基础检验分类");
    }

    @Test
    void andonTypeCopiesHandlingAndNotificationContract() {
        AndonTypeSaveReqVO request = new AndonTypeSaveReqVO();
        request.setTypeCode("EQUIPMENT_STOP");
        request.setTypeName("设备停机");
        request.setExceptionCategory("EQUIPMENT");
        request.setHandlingMode("ASSISTANCE");
        request.setResponseMinutes(10);
        request.setResponsibleRoleCode("EQUIPMENT_ADMIN");
        request.setNotificationChannels("IN_APP");
        request.setLightControlEnabled(true);

        assertThat(AndonTypeConvert.toEntity(request))
                .extracting("typeCode", "handlingMode", "responseMinutes", "lightControlEnabled")
                .containsExactly("EQUIPMENT_STOP", "ASSISTANCE", 10, true);
    }
}
