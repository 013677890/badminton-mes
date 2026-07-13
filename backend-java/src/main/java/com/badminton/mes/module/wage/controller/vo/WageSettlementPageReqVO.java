package com.badminton.mes.module.wage.controller.vo;

import java.time.LocalDate;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 工资结算分页请求。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WageSettlementPageReqVO extends PageParam {
    /** 结算状态 */
    @Min(value = 0, message = "结算状态不合法")
    @Max(value = 3, message = "结算状态不合法")
    private Integer settlementStatus;
    /** 周期开始下限 */
    private LocalDate periodStartBegin;
    /** 周期结束上限 */
    private LocalDate periodEndEnd;
}
