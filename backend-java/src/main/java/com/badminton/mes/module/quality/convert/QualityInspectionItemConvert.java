package com.badminton.mes.module.quality.convert;

import com.badminton.mes.module.quality.controller.vo.QualityInspectionItemRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionItemSaveReqVO;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionCategoryEntity;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionItemEntity;

/** 检验项目显式转换器。 */
public final class QualityInspectionItemConvert {

    public static QualityInspectionItemEntity toEntity(QualityInspectionItemSaveReqVO request) {
        QualityInspectionItemEntity entity = new QualityInspectionItemEntity();
        copyEditableFields(request, entity);
        return entity;
    }

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

    private QualityInspectionItemConvert() {
    }
}
