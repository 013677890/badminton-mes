package com.badminton.mes.module.wage.enums;

/** 结算审计动作。 */
public enum WageSettlementActionEnum {
    /** 首次计算 */
    CALCULATE,
    /** 重新计算 */
    RECALCULATE,
    /** 提交审核 */
    SUBMIT,
    /** 审核通过 */
    APPROVE,
    /** 审核驳回 */
    REJECT,
    /** 人工调整明细 */
    ADJUST
}
