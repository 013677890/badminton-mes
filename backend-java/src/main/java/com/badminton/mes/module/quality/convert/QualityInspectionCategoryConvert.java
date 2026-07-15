package com.badminton.mes.module.quality.convert;

import java.util.List;

import com.badminton.mes.module.quality.controller.vo.QualityInspectionCategoryRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionCategorySaveReqVO;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionCategoryEntity;

/**
 * 检验分类显式转换器。
 *
 * <p>仅负责字段映射，不承担编码唯一性、默认状态、审计字段或逻辑删除规则；这些业务语义由 Service 统一处理。</p>
 */
public final class QualityInspectionCategoryConvert {

    /** 将新增请求中的可编辑主数据复制到新实体。 */
    public static QualityInspectionCategoryEntity toEntity(QualityInspectionCategorySaveReqVO request) {
        QualityInspectionCategoryEntity entity = new QualityInspectionCategoryEntity();
        copyEditableFields(request, entity);
        return entity;
    }

    /**
     * 覆盖分类的可编辑字段，不触碰主键、审计字段和删除标记。
     *
     * <p>启用状态允许为 {@code null}，由调用方在创建时补默认值、在更新时决定是否保留原值。</p>
     */
    public static void copyEditableFields(QualityInspectionCategorySaveReqVO request,
                                          QualityInspectionCategoryEntity entity) {
        entity.setCategoryCode(request.getCategoryCode());
        entity.setCategoryName(request.getCategoryName());
        entity.setEnabledStatus(request.getEnabledStatus());
        entity.setRemark(request.getRemark());
    }

    /** 将分类当前主数据转换为详情响应。 */
    public static QualityInspectionCategoryRespVO toRespVO(QualityInspectionCategoryEntity entity) {
        QualityInspectionCategoryRespVO response = new QualityInspectionCategoryRespVO();
        response.setId(entity.getId());
        response.setCategoryCode(entity.getCategoryCode());
        response.setCategoryName(entity.getCategoryName());
        response.setEnabledStatus(entity.getEnabledStatus());
        response.setRemark(entity.getRemark());
        response.setCreateTime(entity.getCreateTime());
        response.setUpdateTime(entity.getUpdateTime());
        return response;
    }

    /** 批量转换分类当前主数据，保持输入列表顺序。 */
    public static List<QualityInspectionCategoryRespVO> toRespVOList(
            List<QualityInspectionCategoryEntity> entities) {
        return entities.stream().map(QualityInspectionCategoryConvert::toRespVO).toList();
    }

    /** 工具类不允许实例化。 */
    private QualityInspectionCategoryConvert() {
    }
}
