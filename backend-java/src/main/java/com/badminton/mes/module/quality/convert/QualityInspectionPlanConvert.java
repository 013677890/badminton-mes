package com.badminton.mes.module.quality.convert;

import java.util.List;
import java.util.Map;

import com.badminton.mes.module.quality.controller.vo.QualityInspectionPlanItemRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionPlanRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionPlanSaveReqVO;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionItemEntity;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionPlanEntity;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionPlanItemEntity;

/** 检验标准方案显式转换器。 */
public final class QualityInspectionPlanConvert {

    public static QualityInspectionPlanEntity toEntity(QualityInspectionPlanSaveReqVO request) {
        QualityInspectionPlanEntity entity = new QualityInspectionPlanEntity();
        copyEditableFields(request, entity);
        return entity;
    }

    public static void copyEditableFields(QualityInspectionPlanSaveReqVO request,
                                          QualityInspectionPlanEntity entity) {
        entity.setPlanCode(request.getPlanCode());
        entity.setPlanName(request.getPlanName());
        entity.setProductId(request.getProductId());
        entity.setCustomerId(request.getCustomerId());
        entity.setInspectionType(request.getInspectionType());
        entity.setEffectiveDate(request.getEffectiveDate());
        entity.setDefaultFlag(Boolean.TRUE.equals(request.getDefaultFlag()));
        entity.setRemark(request.getRemark());
    }

    public static QualityInspectionPlanRespVO toRespVO(
            QualityInspectionPlanEntity entity,
            List<QualityInspectionPlanItemEntity> planItems,
            Map<Long, QualityInspectionItemEntity> inspectionItemsById) {
        QualityInspectionPlanRespVO response = toSummaryRespVO(entity);
        response.setItems(planItems.stream()
                .map(planItem -> toItemRespVO(planItem, inspectionItemsById.get(planItem.getInspectionItemId())))
                .toList());
        return response;
    }

    public static QualityInspectionPlanRespVO toSummaryRespVO(QualityInspectionPlanEntity entity) {
        QualityInspectionPlanRespVO response = new QualityInspectionPlanRespVO();
        response.setId(entity.getId());
        response.setPlanCode(entity.getPlanCode());
        response.setPlanName(entity.getPlanName());
        response.setProductId(entity.getProductId());
        response.setCustomerId(entity.getCustomerId());
        response.setInspectionType(entity.getInspectionType());
        response.setVersionNo(entity.getVersionNo());
        response.setPlanStatus(entity.getPlanStatus());
        response.setEffectiveDate(entity.getEffectiveDate());
        response.setDefaultFlag(entity.getDefaultFlag());
        response.setRemark(entity.getRemark());
        response.setCreateBy(entity.getCreateBy());
        response.setAuditBy(entity.getAuditBy());
        response.setAuditTime(entity.getAuditTime());
        response.setCreateTime(entity.getCreateTime());
        response.setUpdateTime(entity.getUpdateTime());
        response.setItems(List.of());
        return response;
    }

    private static QualityInspectionPlanItemRespVO toItemRespVO(
            QualityInspectionPlanItemEntity planItem,
            QualityInspectionItemEntity inspectionItem) {
        QualityInspectionPlanItemRespVO response = new QualityInspectionPlanItemRespVO();
        response.setId(planItem.getId());
        response.setInspectionItemId(planItem.getInspectionItemId());
        if (inspectionItem != null) {
            response.setItemCode(inspectionItem.getItemCode());
            response.setItemName(inspectionItem.getItemName());
            response.setValueType(inspectionItem.getValueType());
            response.setUnit(inspectionItem.getUnit());
        }
        response.setSortOrder(planItem.getSortOrder());
        response.setSampleQuantity(planItem.getSampleQuantity());
        response.setRequiredFlag(planItem.getRequiredFlag());
        response.setStandardValue(planItem.getStandardValue());
        response.setLowerLimit(planItem.getLowerLimit());
        response.setUpperLimit(planItem.getUpperLimit());
        response.setJudgmentMethod(planItem.getJudgmentMethod());
        return response;
    }

    private QualityInspectionPlanConvert() {
    }
}
