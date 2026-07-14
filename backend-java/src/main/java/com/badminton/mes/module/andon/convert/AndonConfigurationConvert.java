package com.badminton.mes.module.andon.convert;

import com.badminton.mes.module.andon.controller.vo.AndonConfigurationRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonConfigurationSaveReqVO;
import com.badminton.mes.module.andon.dal.entity.AndonConfigurationEntity;
import com.badminton.mes.module.andon.dal.entity.AndonTypeEntity;

/** 安灯异常处理配置显式转换器。 */
public final class AndonConfigurationConvert {

    public static AndonConfigurationEntity toEntity(AndonConfigurationSaveReqVO request) {
        AndonConfigurationEntity entity = new AndonConfigurationEntity();
        copyEditableFields(request, entity);
        return entity;
    }

    public static void copyEditableFields(AndonConfigurationSaveReqVO request,
                                          AndonConfigurationEntity entity) {
        entity.setAndonTypeId(request.getAndonTypeId());
        entity.setProductionLineId(request.getProductionLineId());
        entity.setScopeLineId(request.getProductionLineId() == null ? 0L : request.getProductionLineId());
        entity.setHandlerUserId(request.getHandlerUserId());
        entity.setHandlerRoleCode(request.getHandlerRoleCode());
        entity.setEscalationUserId(request.getEscalationUserId());
        entity.setEscalationRoleCode(request.getEscalationRoleCode());
        entity.setResponseMinutes(request.getResponseMinutes());
        entity.setEscalationMinutes(request.getEscalationMinutes());
        entity.setNotificationChannels(request.getNotificationChannels());
        entity.setEnabledStatus(request.getEnabledStatus());
        entity.setRemark(request.getRemark());
    }

    public static AndonConfigurationRespVO toRespVO(AndonConfigurationEntity entity,
                                                     AndonTypeEntity andonType) {
        AndonConfigurationRespVO response = new AndonConfigurationRespVO();
        response.setId(entity.getId());
        response.setAndonTypeId(entity.getAndonTypeId());
        response.setAndonTypeCode(andonType.getTypeCode());
        response.setAndonTypeName(andonType.getTypeName());
        response.setProductionLineId(entity.getProductionLineId());
        response.setHandlerUserId(entity.getHandlerUserId());
        response.setHandlerRoleCode(entity.getHandlerRoleCode());
        response.setEscalationUserId(entity.getEscalationUserId());
        response.setEscalationRoleCode(entity.getEscalationRoleCode());
        response.setResponseMinutes(entity.getResponseMinutes());
        response.setEscalationMinutes(entity.getEscalationMinutes());
        response.setNotificationChannels(entity.getNotificationChannels());
        response.setEnabledStatus(entity.getEnabledStatus());
        response.setRemark(entity.getRemark());
        response.setCreateTime(entity.getCreateTime());
        response.setUpdateTime(entity.getUpdateTime());
        return response;
    }

    private AndonConfigurationConvert() {
    }
}
