package com.badminton.mes.module.andon.convert;

import com.badminton.mes.module.andon.controller.vo.AndonReasonRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonReasonSaveReqVO;
import com.badminton.mes.module.andon.dal.entity.AndonReasonEntity;
import com.badminton.mes.module.andon.dal.entity.AndonTypeEntity;

/**
 * 安灯异常原因显式转换器。
 *
 * <p>创建与更新共用同一组可编辑字段映射，主键、创建人、逻辑删除和审计时间由持久化层或 Service
 * 管理。响应转换额外合并所属安灯类型的编码、名称，避免控制层理解实体关联关系。
 */
public final class AndonReasonConvert {

    /** 将保存请求转换为原因实体，不补充启用状态默认值和审计字段。 */
    public static AndonReasonEntity toEntity(AndonReasonSaveReqVO request) {
        AndonReasonEntity entity = new AndonReasonEntity();
        copyEditableFields(request, entity);
        return entity;
    }

    /**
     * 覆盖原因的业务可编辑字段；调用方负责在可空状态未传入时决定是应用默认值还是保留原值。
     */
    public static void copyEditableFields(AndonReasonSaveReqVO request, AndonReasonEntity entity) {
        entity.setReasonCode(request.getReasonCode());
        entity.setReasonName(request.getReasonName());
        entity.setAndonTypeId(request.getAndonTypeId());
        entity.setReasonDescription(request.getReasonDescription());
        entity.setEnabledStatus(request.getEnabledStatus());
    }

    /** 将原因实体及已验证的所属类型组装为对外响应。 */
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

    /** 工具类不允许实例化。 */
    private AndonReasonConvert() {
    }
}
