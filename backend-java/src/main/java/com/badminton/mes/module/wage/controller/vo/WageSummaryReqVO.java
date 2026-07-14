package com.badminton.mes.module.wage.controller.vo;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 已审核工资汇总请求。 */
@Data
public class WageSummaryReqVO {
    /** 统计开始日期 */
    @NotNull(message = "统计开始日期不能为空")
    private LocalDate periodStart;
    /** 统计结束日期 */
    @NotNull(message = "统计结束日期不能为空")
    private LocalDate periodEnd;
    /** 员工或工序主键范围，空表示全部 */
    @Size(max = 100, message = "汇总筛选最多包含 100 个主键")
    private List<@NotNull @Positive Long> ids;
}
