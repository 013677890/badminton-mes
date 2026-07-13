package com.badminton.mes.module.quality.convert;

import java.util.List;

import com.badminton.mes.module.quality.controller.vo.QualityInspectionCategoryRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionCategorySaveReqVO;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionCategoryEntity;

/** 检验分类显式转换器。 */
public final class QualityInspectionCategoryConvert {

    public static QualityInspectionCategoryEntity toEntity(QualityInspectionCategorySaveReqVO request) {
        QualityInspectionCategoryEntity entity = new QualityInspectionCategoryEntity();
        copyEditableFields(request, entity);
        return entity;
    }

    public static void copyEditableFields(QualityInspectionCategorySaveReqVO request,
                                          QualityInspectionCategoryEntity entity) {
        entity.setCategoryCode(request.getCategoryCode());
        entity.setCategoryName(request.getCategoryName());
        entity.setEnabledStatus(request.getEnabledStatus());
        entity.setRemark(request.getRemark());
    }

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

    public static List<QualityInspectionCategoryRespVO> toRespVOList(
            List<QualityInspectionCategoryEntity> entities) {
        return entities.stream().map(QualityInspectionCategoryConvert::toRespVO).toList();
    }

    private QualityInspectionCategoryConvert() {
    }
}
