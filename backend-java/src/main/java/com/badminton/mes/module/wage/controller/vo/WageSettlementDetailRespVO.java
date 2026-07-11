package com.badminton.mes.module.wage.controller.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

/** 工资结算明细响应。 */
@Data
public class WageSettlementDetailRespVO {
    /** 主键 */
    private Long id;
    /** 结算主键 */
    private Long settlementId;
    /** 报工快照主键 */
    private Long workRecordId;
    /** 规则主键 */
    private Long ruleId;
    /** 员工主键 */
    private Long employeeId;
    /** 作业日期 */
    private LocalDate workDate;
    /** 工单主键 */
    private Long workOrderId;
    /** 工序主键 */
    private Long processId;
    /** 产品主键 */
    private Long productId;
    /** 合格数量 */
    private BigDecimal qualifiedQuantity;
    /** 不良数量 */
    private BigDecimal defectQuantity;
    /** 单价，单位元 */
    private BigDecimal unitPrice;
    /** 不良扣减率，百分比 */
    private BigDecimal defectDeductionRate;
    /** 系统计算金额，单位元 */
    private BigDecimal calculatedAmount;
    /** 人工调整金额，单位元 */
    private BigDecimal adjustedAmount;
    /** 最终金额，单位元 */
    private BigDecimal finalAmount;
}
