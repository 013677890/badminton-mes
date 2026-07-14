package com.badminton.mes.module.andon.convert;

import com.badminton.mes.module.andon.controller.vo.AndonReasonRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonReasonSaveReqVO;
import com.badminton.mes.module.andon.dal.entity.AndonReasonEntity;
import com.badminton.mes.module.andon.dal.entity.AndonTypeEntity;

/** 安灯异常原因显式转换器。 */
public final class AndonReasonConvert {

    public static AndonReasonEntity toEntity(AndonReasonSaveReqVO request) {
        AndonReasonEntity entity = new AndonReasonEntity();
        copyEditableFields(request, entity);
        return entity;
    }

    public static void copyEditableFields(AndonReasonSaveReqVO request, AndonReasonEntity entity) {
        entity.setReasonCode(request.getReasonCode());
        entity.setReasonName(request.getReasonName());
        entity.setAndonTypeId(request.getAndonTypeId());
        entity.setReasonDescription(request.getReasonDescription());
        entity.setEnabledStatus(request.getEnabledStatus());
    }

    public static AndonReasonRespVO toRespVO(AndonReasonEntity entity, AndonTypeEntity andonType) {
        AndonReasonRespVO response = new AndonReasonRespVO();
        response.setId(entity.getId());
        response.setReasonCode(entity.getReasonCode());
        response.setReasonName(entity.getReasonName());
        response.setAndonTypeId(entity.getAndonTypeId());
        response.setAndonTypeCode(andonType.getTypeCode());
        response.setAndonTypeName(andonType.getTypeName());
        response.setReasonDescription(entity.getReasonDescription());
        response.setEnabledStatus(entity.getEnabledStatus());
        response.setCreateTime(entity.getCreateTime());
        response.setUpdateTime(entity.getUpdateTime());
        return response;
    }

    private AndonReasonConvert() {
    }
}
