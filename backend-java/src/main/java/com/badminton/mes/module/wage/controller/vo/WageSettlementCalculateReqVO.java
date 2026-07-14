package com.badminton.mes.module.wage.controller.vo;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 工资结算计算请求。 */
@Data
public class WageSettlementCalculateReqVO {
    /** 结算开始日期 */
    @NotNull(message = "结算开始日期不能为空")
    private LocalDate periodStart;
    /** 结算结束日期 */
    @NotNull(message = "结算结束日期不能为空")
    private LocalDate periodEnd;
    /** 员工范围，空表示全部员工 */
    @Size(max = 100, message = "单批次最多指定 100 名员工")
    private List<@NotNull @Positive Long> employeeIds;
    /** 计算说明 */
    @Size(max = 255, message = "计算说明长度不能超过 255")
    private String reason;
}
