package com.badminton.mes.module.wage.controller.vo;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 结算明细人工调整请求。 */
@Data
public class WageSettlementAdjustReqVO {
    /** 客户端读取时的结算版本 */
    @NotNull(message = "结算版本不能为空")
    @PositiveOrZero(message = "结算版本不能小于 0")
    private Integer settlementVersion;
    /** 调整后金额，单位元 */
    @NotNull(message = "调整金额不能为空")
    @DecimalMin(value = "0.0000", message = "调整金额不能小于 0")
    @DecimalMax(value = "922337203685477.5807", message = "调整金额超过系统支持范围")
    @Digits(integer = 15, fraction = 4, message = "调整金额最多 15 位整数和 4 位小数")
    private BigDecimal adjustedAmount;
    /** 调整原因 */
    @NotBlank(message = "调整原因不能为空")
    @Size(max = 255, message = "调整原因长度不能超过 255")
    private String reason;
}
