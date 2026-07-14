package com.badminton.mes.module.andon.convert;

import java.util.List;

import com.badminton.mes.module.andon.controller.vo.AndonTypeRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonTypeSaveReqVO;
import com.badminton.mes.module.andon.dal.entity.AndonTypeEntity;

/** 安灯类型显式转换器。 */
public final class AndonTypeConvert {

    public static AndonTypeEntity toEntity(AndonTypeSaveReqVO request) {
        AndonTypeEntity entity = new AndonTypeEntity();
        copyEditableFields(request, entity);
        return entity;
    }

    public static void copyEditableFields(AndonTypeSaveReqVO request, AndonTypeEntity entity) {
        entity.setTypeCode(request.getTypeCode());
        entity.setTypeName(request.getTypeName());
        entity.setExceptionCategory(request.getExceptionCategory());
        entity.setHandlingMode(request.getHandlingMode());
        entity.setResponseMinutes(request.getResponseMinutes());
        entity.setResponsibleRoleCode(request.getResponsibleRoleCode());
        entity.setNotificationChannels(request.getNotificationChannels());
        entity.setLightControlEnabled(request.getLightControlEnabled());
        entity.setEnabledStatus(request.getEnabledStatus());
        entity.setRemark(request.getRemark());
    }

    public static AndonTypeRespVO toRespVO(AndonTypeEntity entity) {
        AndonTypeRespVO response = new AndonTypeRespVO();
        response.setId(entity.getId());
        response.setTypeCode(entity.getTypeCode());
        response.setTypeName(entity.getTypeName());
        response.setExceptionCategory(entity.getExceptionCategory());
        response.setHandlingMode(entity.getHandlingMode());
        response.setResponseMinutes(entity.getResponseMinutes());
        response.setResponsibleRoleCode(entity.getResponsibleRoleCode());
        response.setNotificationChannels(entity.getNotificationChannels());
        response.setLightControlEnabled(entity.getLightControlEnabled());
        response.setEnabledStatus(entity.getEnabledStatus());
        response.setRemark(entity.getRemark());
        response.setCreateTime(entity.getCreateTime());
        response.setUpdateTime(entity.getUpdateTime());
        return response;
    }

    public static List<AndonTypeRespVO> toRespVOList(List<AndonTypeEntity> entities) {
        return entities.stream().map(AndonTypeConvert::toRespVO).toList();
    }

    private AndonTypeConvert() {
    }
}
