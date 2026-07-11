package com.badminton.mes.module.wage.controller.vo;

import java.math.BigDecimal;

import lombok.Data;

/** 工序计件工资汇总响应。 */
@Data
public class ProcessWageSummaryRespVO {
    /** 工序主键 */
    private Long processId;
    /** 工序编码 */
    private String processCode;
    /** 工序名称 */
    private String processName;
    /** 合格数量 */
    private BigDecimal qualifiedQuantity;
    /** 不良数量 */
    private BigDecimal defectQuantity;
    /** 已审核工资金额，单位元 */
    private BigDecimal totalAmount;
}
