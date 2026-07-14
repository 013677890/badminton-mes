package com.badminton.mes.module.wage.convert;

import java.util.List;

import com.badminton.mes.module.wage.controller.vo.PieceRateRuleRespVO;
import com.badminton.mes.module.wage.controller.vo.PieceRateRuleSaveReqVO;
import com.badminton.mes.module.wage.controller.vo.WageRuleChangeLogRespVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementAuditLogRespVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementDetailRespVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementRespVO;
import com.badminton.mes.module.wage.controller.vo.WageWorkRecordItemReqVO;
import com.badminton.mes.module.wage.controller.vo.WageWorkRecordRespVO;
import com.badminton.mes.module.wage.dal.entity.PieceRateRuleEntity;
import com.badminton.mes.module.wage.dal.entity.WageRuleChangeLogEntity;
import com.badminton.mes.module.wage.dal.entity.WageSettlementAuditLogEntity;
import com.badminton.mes.module.wage.dal.entity.WageSettlementDetailEntity;
import com.badminton.mes.module.wage.dal.entity.WageSettlementEntity;
import com.badminton.mes.module.wage.dal.entity.WageWorkRecordEntity;
import com.badminton.mes.module.wage.service.support.WageAmountUtils;

/** 计件工资实体与接口对象转换器。 */
public final class WageConvert {

    /** 创建计件规则实体。 */
    public static PieceRateRuleEntity toRuleEntity(PieceRateRuleSaveReqVO reqVO) {
        PieceRateRuleEntity entity = new PieceRateRuleEntity();
        copyRule(reqVO, entity);
        return entity;
    }

    /** 将规则请求复制到实体。 */
    public static void copyRule(PieceRateRuleSaveReqVO reqVO, PieceRateRuleEntity entity) {
        entity.setProcessId(reqVO.getProcessId());
        entity.setProductId(reqVO.getProductId());
        entity.setUnitPriceBasis(WageAmountUtils.toAmountBasis(reqVO.getUnitPrice()));
        entity.setDefectDeductionRate(WageAmountUtils.toRateBasis(reqVO.getDefectDeductionRate()));
        entity.setEffectiveStart(reqVO.getEffectiveStart());
        entity.setEffectiveEnd(reqVO.getEffectiveEnd());
        entity.setStatus(reqVO.getStatus());
    }

    /** 转换计件规则响应。 */
    public static PieceRateRuleRespVO toRuleRespVO(PieceRateRuleEntity entity) {
        PieceRateRuleRespVO result = new PieceRateRuleRespVO();
        result.setId(entity.getId());
        result.setProcessId(entity.getProcessId());
        result.setProductId(entity.getProductId());
        result.setUnitPrice(WageAmountUtils.fromAmountBasis(entity.getUnitPriceBasis()));
        result.setDefectDeductionRate(WageAmountUtils.fromRateBasis(entity.getDefectDeductionRate()));
        result.setEffectiveStart(entity.getEffectiveStart());
        result.setEffectiveEnd(entity.getEffectiveEnd());
        result.setStatus(entity.getStatus());
        result.setVersion(entity.getVersion());
        result.setCreateTime(entity.getCreateTime());
        result.setUpdateTime(entity.getUpdateTime());
        return result;
    }

    /** 批量转换计件规则响应。 */
    public static List<PieceRateRuleRespVO> toRuleRespVOList(List<PieceRateRuleEntity> entities) {
        return entities.stream().map(WageConvert::toRuleRespVO).toList();
    }

    /** 转换规则变更日志响应。 */
    public static WageRuleChangeLogRespVO toRuleLogRespVO(WageRuleChangeLogEntity entity) {
        WageRuleChangeLogRespVO result = new WageRuleChangeLogRespVO();
        result.setId(entity.getId());
        result.setRuleId(entity.getRuleId());
        result.setChangeType(entity.getChangeType());
        result.setBeforeSnapshot(entity.getBeforeSnapshot());
        result.setAfterSnapshot(entity.getAfterSnapshot());
        result.setChangeReason(entity.getChangeReason());
        result.setOperateBy(entity.getOperateBy());
        result.setOperateTime(entity.getOperateTime());
        return result;
    }

    /** 转换报工导入请求为快照实体。 */
    public static WageWorkRecordEntity toWorkRecordEntity(WageWorkRecordItemReqVO reqVO, Long operatorId) {
        WageWorkRecordEntity entity = new WageWorkRecordEntity();
        entity.setSourceReportId(reqVO.getSourceReportId());
        entity.setEmployeeId(reqVO.getEmployeeId());
        entity.setWorkDate(reqVO.getWorkDate());
        entity.setWorkOrderId(reqVO.getWorkOrderId());
        entity.setProcessId(reqVO.getProcessId());
        entity.setProductId(reqVO.getProductId());
        entity.setQualifiedQuantity(reqVO.getQualifiedQuantity());
        entity.setDefectQuantity(reqVO.getDefectQuantity());
        entity.setSourceAuditTime(reqVO.getSourceAuditTime());
        entity.setCreateBy(operatorId);
        return entity;
    }

    /** 转换报工快照响应。 */
    public static WageWorkRecordRespVO toWorkRecordRespVO(WageWorkRecordEntity entity) {
        WageWorkRecordRespVO result = new WageWorkRecordRespVO();
        result.setId(entity.getId());
        result.setSourceReportId(entity.getSourceReportId());
        result.setEmployeeId(entity.getEmployeeId());
        result.setWorkDate(entity.getWorkDate());
        result.setWorkOrderId(entity.getWorkOrderId());
        result.setProcessId(entity.getProcessId());
        result.setProductId(entity.getProductId());
        result.setQualifiedQuantity(entity.getQualifiedQuantity());
        result.setDefectQuantity(entity.getDefectQuantity());
        result.setSourceAuditTime(entity.getSourceAuditTime());
        result.setCreateTime(entity.getCreateTime());
        return result;
    }

    /** 转换结算批次响应。 */
    public static WageSettlementRespVO toSettlementRespVO(WageSettlementEntity entity) {
        WageSettlementRespVO result = new WageSettlementRespVO();
        result.setId(entity.getId());
        result.setSettlementNo(entity.getSettlementNo());
        result.setPeriodStart(entity.getPeriodStart());
        result.setPeriodEnd(entity.getPeriodEnd());
        result.setSettlementStatus(entity.getSettlementStatus());
        result.setTotalQualifiedQuantity(entity.getTotalQualifiedQuantity());
        result.setTotalDefectQuantity(entity.getTotalDefectQuantity());
        result.setTotalAmount(WageAmountUtils.fromAmountBasis(entity.getTotalAmountBasis()));
        result.setVersion(entity.getVersion());
        result.setSubmitBy(entity.getSubmitBy());
        result.setSubmitTime(entity.getSubmitTime());
        result.setAuditBy(entity.getAuditBy());
        result.setAuditTime(entity.getAuditTime());
        result.setAuditReason(entity.getAuditReason());
        result.setCreateTime(entity.getCreateTime());
        result.setUpdateTime(entity.getUpdateTime());
        return result;
    }

    /** 转换结算明细响应。 */
    public static WageSettlementDetailRespVO toDetailRespVO(WageSettlementDetailEntity entity) {
        WageSettlementDetailRespVO result = new WageSettlementDetailRespVO();
        result.setId(entity.getId());
        result.setSettlementId(entity.getSettlementId());
        result.setWorkRecordId(entity.getWorkRecordId());
        result.setRuleId(entity.getRuleId());
        result.setEmployeeId(entity.getEmployeeId());
        result.setWorkDate(entity.getWorkDate());
        result.setWorkOrderId(entity.getWorkOrderId());
        result.setProcessId(entity.getProcessId());
        result.setProductId(entity.getProductId());
        result.setQualifiedQuantity(entity.getQualifiedQuantity());
        result.setDefectQuantity(entity.getDefectQuantity());
        result.setUnitPrice(WageAmountUtils.fromAmountBasis(entity.getUnitPriceBasis()));
        result.setDefectDeductionRate(WageAmountUtils.fromRateBasis(entity.getDefectDeductionRate()));
        result.setCalculatedAmount(WageAmountUtils.fromAmountBasis(entity.getCalculatedAmountBasis()));
        result.setAdjustedAmount(WageAmountUtils.fromAmountBasis(entity.getAdjustedAmountBasis()));
        result.setFinalAmount(WageAmountUtils.fromAmountBasis(entity.getFinalAmountBasis()));
        return result;
    }

    /** 转换结算审计日志响应。 */
    public static WageSettlementAuditLogRespVO toAuditLogRespVO(WageSettlementAuditLogEntity entity) {
        WageSettlementAuditLogRespVO result = new WageSettlementAuditLogRespVO();
        result.setId(entity.getId());
        result.setDetailId(entity.getDetailId());
        result.setActionType(entity.getActionType());
        result.setFromStatus(entity.getFromStatus());
        result.setToStatus(entity.getToStatus());
        result.setBeforeAmount(WageAmountUtils.fromAmountBasis(entity.getBeforeAmountBasis()));
        result.setAfterAmount(WageAmountUtils.fromAmountBasis(entity.getAfterAmountBasis()));
        result.setActionReason(entity.getActionReason());
        result.setOperateBy(entity.getOperateBy());
        result.setOperateTime(entity.getOperateTime());
        return result;
    }

    private WageConvert() {
    }
}
