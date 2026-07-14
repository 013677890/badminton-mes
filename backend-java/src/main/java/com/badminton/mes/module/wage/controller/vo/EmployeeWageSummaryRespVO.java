package com.badminton.mes.module.wage.controller.vo;

import java.math.BigDecimal;

import lombok.Data;

/** 员工计件工资汇总响应。 */
@Data
public class EmployeeWageSummaryRespVO {
    /** 员工主键 */
    private Long employeeId;
    /** 员工工号 */
    private String employeeNo;
    /** 员工姓名 */
    private String employeeName;
    /** 合格数量 */
    private BigDecimal qualifiedQuantity;
    /** 不良数量 */
    private BigDecimal defectQuantity;
    /** 已审核工资金额，单位元 */
    private BigDecimal totalAmount;
}
