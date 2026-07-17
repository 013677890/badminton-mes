package com.badminton.mes.module.andon.convert;

import java.util.List;

import com.badminton.mes.module.andon.controller.vo.AndonTypeRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonTypeSaveReqVO;
import com.badminton.mes.module.andon.dal.entity.AndonTypeEntity;

/**
 * 安灯类型显式转换器。
 *
 * <p>转换器完整复制类型的异常类别、三种处理模式规则、通知渠道和模拟灯控开关，但不解释规则、
 * 不应用默认值。协助模式字段完整性以及更新时空状态的保留策略由 Service 负责。
 */
public final class AndonTypeConvert {

    /** 将保存请求转换为类型实体，审计及逻辑删除字段留给 Service 补齐。 */
    public static AndonTypeEntity toEntity(AndonTypeSaveReqVO request) {
        AndonTypeEntity entity = new AndonTypeEntity();
        copyEditableFields(request, entity);
        return entity;
    }

    /**
     * 覆盖类型全部业务可编辑字段；可空启用状态和灯控开关是否保留原值由调用场景决定。
     */
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

    /** 将类型实体的规则、状态与审计时间映射为对外响应。 */
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

    /** 保持输入顺序批量转换类型列表，供分页查询复用单条映射。 */
    public static List<AndonTypeRespVO> toRespVOList(List<AndonTypeEntity> entities) {
        return entities.stream().map(AndonTypeConvert::toRespVO).toList();
    }

    /** 工具类不允许实例化。 */
    private AndonTypeConvert() {
    }
}
