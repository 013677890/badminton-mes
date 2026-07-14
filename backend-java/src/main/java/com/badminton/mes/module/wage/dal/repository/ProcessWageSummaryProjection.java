package com.badminton.mes.module.wage.dal.repository;

import java.math.BigDecimal;

/** 工序计件工资汇总投影。 */
public interface ProcessWageSummaryProjection {
    /** 工序主键 */
    Long getProcessId();
    /** 合格数量合计 */
    BigDecimal getQualifiedQuantity();
    /** 不良数量合计 */
    BigDecimal getDefectQuantity();
    /** 工资金额合计，万分之一元 */
    BigDecimal getAmountBasis();
}
