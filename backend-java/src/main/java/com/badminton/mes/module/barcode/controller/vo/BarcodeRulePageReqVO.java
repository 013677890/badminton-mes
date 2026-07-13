package com.badminton.mes.module.barcode.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 条码规则分页查询请求 VO，继承 {@link PageParam} 获得分页参数与入参保护。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BarcodeRulePageReqVO extends PageParam {

    /** 规则编码前缀，右模糊匹配，可空 */
    @Size(max = 32, message = "规则编码长度不能超过 32")
    private String ruleCode;

    /** 规则名称关键字，包含匹配，可空 */
    @Size(max = 64, message = "规则名称长度不能超过 64")
    private String ruleName;

    /** 适用条码类型 id，可空 */
    @Positive(message = "条码类型 id 必须为正数")
    private Long barcodeTypeId;

    /** 状态：1 启用 0 停用，可空 */
    @Min(value = 0, message = "状态取值为 0-1")
    @Max(value = 1, message = "状态取值为 0-1")
    private Integer status;
}
