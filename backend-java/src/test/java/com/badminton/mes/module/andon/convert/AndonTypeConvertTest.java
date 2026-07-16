package com.badminton.mes.module.andon.convert;

import com.badminton.mes.module.andon.controller.vo.AndonTypeSaveReqVO;
import com.badminton.mes.module.andon.dal.entity.AndonTypeEntity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 安灯类型转换器单元测试。 */
class AndonTypeConvertTest {

    @Test
    void toEntityCopiesEveryEditableField() {
        AndonTypeSaveReqVO request = new AndonTypeSaveReqVO();
        request.setTypeCode("EQUIPMENT_STOP");
        request.setTypeName("设备停机");
        request.setExceptionCategory("EQUIPMENT");
        request.setHandlingMode("ASSISTANCE");
        request.setResponseMinutes(10);
        request.setResponsibleRoleCode("EQUIPMENT_ADMIN");
        request.setNotificationChannels("IN_APP,WECHAT");
        request.setLightControlEnabled(true);
        request.setEnabledStatus(1);
        request.setRemark("需要维修响应");

        AndonTypeEntity entity = AndonTypeConvert.toEntity(request);

        assertThat(entity.getTypeCode()).isEqualTo("EQUIPMENT_STOP");
        assertThat(entity.getExceptionCategory()).isEqualTo("EQUIPMENT");
        assertThat(entity.getHandlingMode()).isEqualTo("ASSISTANCE");
        assertThat(entity.getResponseMinutes()).isEqualTo(10);
        assertThat(entity.getNotificationChannels()).isEqualTo("IN_APP,WECHAT");
        assertThat(entity.getLightControlEnabled()).isTrue();
        assertThat(entity.getRemark()).isEqualTo("需要维修响应");
    }

    @Test
    void responseConversionDoesNotExposeDeletionMetadata() {
        AndonTypeEntity entity = new AndonTypeEntity();
        entity.setId(7L);
        entity.setTypeCode("QUALITY_ALERT");
        entity.setTypeName("质量异常");
        entity.setDeleted(true);

        assertThat(AndonTypeConvert.toRespVO(entity))
                .extracting("id", "typeCode", "typeName")
                .containsExactly(7L, "QUALITY_ALERT", "质量异常");
    }
}
