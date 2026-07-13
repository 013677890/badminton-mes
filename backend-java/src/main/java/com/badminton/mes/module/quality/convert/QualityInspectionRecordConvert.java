package com.badminton.mes.module.quality.convert;

import java.util.List;

import com.badminton.mes.module.quality.controller.vo.QualityInspectionRecordRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionResultRespVO;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionRecordEntity;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionResultEntity;

/** 质量检验单显式转换器。 */
public final class QualityInspectionRecordConvert {

    public static QualityInspectionRecordRespVO toRespVO(
            QualityInspectionRecordEntity record,
            List<QualityInspectionResultEntity> results) {
        QualityInspectionRecordRespVO response = toSummaryRespVO(record);
        response.setResults(results.stream().map(QualityInspectionRecordConvert::toResultRespVO).toList());
        return response;
    }

    public static QualityInspectionRecordRespVO toSummaryRespVO(QualityInspectionRecordEntity record) {
        QualityInspectionRecordRespVO response = new QualityInspectionRecordRespVO();
        response.setId(record.getId());
        response.setInspectionNo(record.getInspectionNo());
        response.setInspectionType(record.getInspectionType());
        response.setPlanId(record.getPlanId());
        response.setPlanCode(record.getPlanCodeSnapshot());
        response.setPlanVersion(record.getPlanVersionSnapshot());
        response.setWorkOrderId(record.getWorkOrderId());
        response.setSourceDocumentId(record.getSourceDocumentId());
        response.setSourceDocumentNo(record.getSourceDocumentNo());
        response.setProductId(record.getProductId());
        response.setCustomerId(record.getCustomerId());
        response.setProductionLineId(record.getProductionLineId());
        response.setBatchNo(record.getBatchNo());
        response.setSampleQuantity(record.getSampleQuantity());
        response.setRecordStatus(record.getRecordStatus());
        response.setConclusion(record.getConclusion());
        response.setReleaseStatus(record.getReleaseStatus());
        response.setNonconformanceDescription(record.getNonconformanceDescription());
        response.setDisposition(record.getDisposition());
        response.setInspectorId(record.getInspectorId());
        response.setInspectedAt(record.getInspectedAt());
        response.setCreateTime(record.getCreateTime());
        response.setUpdateTime(record.getUpdateTime());
        response.setResults(List.of());
        return response;
    }

    private static QualityInspectionResultRespVO toResultRespVO(QualityInspectionResultEntity result) {
        QualityInspectionResultRespVO response = new QualityInspectionResultRespVO();
        response.setId(result.getId());
        response.setInspectionItemId(result.getInspectionItemId());
        response.setItemCode(result.getItemCodeSnapshot());
        response.setItemName(result.getItemNameSnapshot());
        response.setValueType(result.getValueTypeSnapshot());
        response.setUnit(result.getUnitSnapshot());
        response.setRequiredFlag(result.getRequiredFlag());
        response.setStandardValue(result.getStandardValueSnapshot());
        response.setLowerLimit(result.getLowerLimitSnapshot());
        response.setUpperLimit(result.getUpperLimitSnapshot());
        response.setJudgmentMethod(result.getJudgmentMethodSnapshot());
        response.setMeasuredValue(result.getMeasuredValue());
        response.setJudgmentResult(result.getJudgmentResult());
        response.setDefectDescription(result.getDefectDescription());
        response.setSortOrder(result.getSortOrder());
        return response;
    }

    private QualityInspectionRecordConvert() {
    }
}
