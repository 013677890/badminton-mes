package com.badminton.mes.module.quality.convert;

import com.badminton.mes.module.quality.controller.vo.QualityInspectionItemRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionItemSaveReqVO;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionCategoryEntity;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionItemEntity;

/**
 * 检验项目显式转换器。
 *
 * <p>项目实体保存可复用的当前检验主数据；分类编码和名称不落在项目实体中，而是在响应阶段从分类主数据
 * 冗余装配。该冗余用于展示，不是历史快照，因此分类变更后需要级联失效项目缓存。</p>
 */
public final class QualityInspectionItemConvert {

    /** 将请求中的项目主数据和规则字段复制到新实体。 */
    public static QualityInspectionItemEntity toEntity(QualityInspectionItemSaveReqVO request) {
        QualityInspectionItemEntity entity = new QualityInspectionItemEntity();
        copyEditableFields(request, entity);
        return entity;
    }

    /**
     * 覆盖项目全部可编辑字段，不处理值类型与判定规则组合校验。
     *
     * <p>必检和启用字段的 {@code null} 默认/保留语义由 Service 根据创建或更新场景决定。</p>
     */
    public static void copyEditableFields(QualityInspectionItemSaveReqVO request,
                                          QualityInspectionItemEntity entity) {
        entity.setItemCode(request.getItemCode());
        entity.setItemName(request.getItemName());
        entity.setCategoryId(request.getCategoryId());
        entity.setValueType(request.getValueType());
        entity.setUnit(request.getUnit());
        entity.setStandardValue(request.getStandardValue());
        entity.setLowerLimit(request.getLowerLimit());
        entity.setUpperLimit(request.getUpperLimit());
        entity.setJudgmentMethod(request.getJudgmentMethod());
        entity.setInspectionMethod(request.getInspectionMethod());
        entity.setRequiredFlag(request.getRequiredFlag());
        entity.setEnabledStatus(request.getEnabledStatus());
        entity.setRemark(request.getRemark());
    }

    /**
     * 组装项目详情，并冗余所属分类的当前编码和名称。
     *
     * <p>调用方必须先保证分类存在且有效；这里不降级为空分类，避免掩盖主数据引用损坏。</p>
     */
    public static QualityInspectionItemRespVO toRespVO(QualityInspectionItemEntity entity,
                                                        QualityInspectionCategoryEntity category) {
        QualityInspectionItemRespVO response = new QualityInspectionItemRespVO();
        response.setId(entity.getId());
        response.setItemCode(entity.getItemCode());
        response.setItemName(entity.getItemName());
        response.setCategoryId(entity.getCategoryId());
        response.setCategoryCode(category.getCategoryCode());
        response.setCategoryName(category.getCategoryName());
        response.setValueType(entity.getValueType());
        response.setUnit(entity.getUnit());
        response.setStandardValue(entity.getStandardValue());
        response.setLowerLimit(entity.getLowerLimit());
        response.setUpperLimit(entity.getUpperLimit());
        response.setJudgmentMethod(entity.getJudgmentMethod());
        response.setInspectionMethod(entity.getInspectionMethod());
        response.setRequiredFlag(entity.getRequiredFlag());
        response.setEnabledStatus(entity.getEnabledStatus());
        response.setRemark(entity.getRemark());
        response.setCreateTime(entity.getCreateTime());
        response.setUpdateTime(entity.getUpdateTime());
        return response;
    }

    /** 工具类不允许实例化。 */
    private QualityInspectionItemConvert() {
    }
}
