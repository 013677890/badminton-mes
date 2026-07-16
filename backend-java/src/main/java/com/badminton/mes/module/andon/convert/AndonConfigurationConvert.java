package com.badminton.mes.module.andon.convert;

import com.badminton.mes.module.andon.controller.vo.AndonConfigurationRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonConfigurationSaveReqVO;
import com.badminton.mes.module.andon.dal.entity.AndonConfigurationEntity;
import com.badminton.mes.module.andon.dal.entity.AndonTypeEntity;

/**
 * 安灯协助处理配置显式转换器。
 *
 * <p>保存映射同时维护展示用的可空 {@code productionLineId} 与唯一约束用的 {@code scopeLineId}：
 * 空产线被规范为 {@code 0}，表示全局规则；具体产线则使用其真实主键。责任主体、响应/升级时限和
 * 通知渠道仅做字段复制，组合合法性由 Service 统一校验。
 */
public final class AndonConfigurationConvert {

    /** 将保存请求转换为配置实体，不补充启用默认值、创建人和逻辑删除字段。 */
    public static AndonConfigurationEntity toEntity(AndonConfigurationSaveReqVO request) {
        AndonConfigurationEntity entity = new AndonConfigurationEntity();
        copyEditableFields(request, entity);
        return entity;
    }

    /**
     * 覆盖全部业务可编辑字段，并同步计算用于全局/产线范围唯一性的规范化范围标识。
     */
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

    /**
     * 将配置实体与已验证的类型展示信息组装为响应；内部范围哨兵值不暴露给调用方。
     */
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

    /** 工具类不允许实例化。 */
    private AndonConfigurationConvert() {
    }
}
