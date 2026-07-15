package com.badminton.mes.module.barcode.controller.vo;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 条码规则校验请求 VO：只做配置合法性检查，返回逐条错误，不落库。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodeRuleValidateReqVO {

    /** 流水号位数 */
    @NotNull(message = "流水号位数不能为空")
    @Min(value = 1, message = "流水号位数最小值为 1")
    @Max(value = 9, message = "流水号位数最大值为 9")
    private Integer serialLength;

    /** 组成明细 */
    @NotEmpty(message = "规则组成明细不能为空")
    @Valid
    private List<BarcodeRuleItemSaveReqVO> items;
}
