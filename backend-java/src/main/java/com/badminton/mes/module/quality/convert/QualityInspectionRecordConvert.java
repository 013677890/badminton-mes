package com.badminton.mes.module.quality.convert;

import java.util.List;

import com.badminton.mes.module.quality.controller.vo.QualityInspectionRecordRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionResultRespVO;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionRecordEntity;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionResultEntity;

/**
 * 质量检验单显式转换器。
 *
 * <p>检验单响应坚持读取落库快照：方案编码/版本来自检验单快照，项目名称、值类型、单位和判定规则来自
 * 结果快照，不回查当前方案或项目主数据，从而保证历史检验记录可审计、可复现。</p>
 */
public final class QualityInspectionRecordConvert {

    /** 组装检验单完整详情，并按调用方给定顺序转换全部历史结果快照。 */
    public static QualityInspectionRecordRespVO toRespVO(
            QualityInspectionRecordEntity record,
            List<QualityInspectionResultEntity> results) {
        QualityInspectionRecordRespVO response = toSummaryRespVO(record);
        response.setResults(results.stream().map(QualityInspectionRecordConvert::toResultRespVO).toList());
        return response;
    }

    /**
     * 转换检验单摘要。
     *
     * <p>方案编号和版本使用创建检验单时固化的快照；结果列表显式置空，供分页查询复用。</p>
     */
    public static QualityInspectionRecordRespVO toSummaryRespVO(QualityInspectionRecordEntity record) {
        QualityInspectionRecordRespVO response = new QualityInspectionRecordRespVO();
        response.setId(record.getId());
        response.setInspectionNo(record.getInspectionNo());
        response.setInspectionType(record.getInspectionType());
        response.setPlanId(record.getPlanId());
        response.setPlanCode(record.getPlanCodeSnapshot());
        response.setPlanVersion(record.getPlanVersionSnapshot());
        response.setWorkOrderId(record.getWorkOrderId());
        response.setProductionTaskId(record.getProductionTaskId());
        response.setSourceDocumentId(record.getSourceDocumentId());
        response.setSourceDocumentNo(record.getSourceDocumentNo());
        response.setProductId(record.getProductId());
        response.setCustomerId(record.getCustomerId());
        response.setProductionLineId(record.getProductionLineId());
        response.setProcessId(record.getProcessId());
        response.setBatchNo(record.getBatchNo());
        response.setSampleQuantity(record.getSampleQuantity());
        response.setRecordStatus(record.getRecordStatus());
        response.setConclusion(record.getConclusion());
        response.setReleaseStatus(record.getReleaseStatus());
        response.setDefectGroupNo(record.getDefectGroupNo());
        response.setDefectQuantity(record.getDefectQuantity());
        response.setNonconformanceDescription(record.getNonconformanceDescription());
        response.setDisposition(record.getDisposition());
        response.setInspectorId(record.getInspectorId());
        response.setInspectedAt(record.getInspectedAt());
        response.setCreateTime(record.getCreateTime());
        response.setUpdateTime(record.getUpdateTime());
        response.setResults(List.of());
        return response;
    }

    /**
     * 转换单项检验结果。
     *
     * <p>项目描述与规则全部读取快照列，实测值、判定和缺陷描述读取本次检验填写结果，
     * 两部分共同还原检验发生时的依据与事实。</p>
     */
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

    /** 工具类不允许实例化。 */
    private QualityInspectionRecordConvert() {
    }
}
