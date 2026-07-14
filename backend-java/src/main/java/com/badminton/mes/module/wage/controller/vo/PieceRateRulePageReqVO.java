package com.badminton.mes.module.wage.controller.vo;

import java.time.LocalDate;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 计件规则分页请求。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PieceRateRulePageReqVO extends PageParam {
    /** 工序主键 */
    @Positive(message = "工序 id 必须为正数")
    private Long processId;
    /** 产品主键 */
    @Positive(message = "产品 id 必须为正数")
    private Long productId;
    /** 状态 */
    @Min(value = 0, message = "规则状态不合法")
    @Max(value = 1, message = "规则状态不合法")
    private Integer status;
    /** 查询指定日期生效的规则 */
    private LocalDate effectiveDate;
}
