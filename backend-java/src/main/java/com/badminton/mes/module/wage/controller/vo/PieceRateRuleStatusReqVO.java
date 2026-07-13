package com.badminton.mes.module.wage.controller.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 计件规则状态变更请求。 */
@Data
public class PieceRateRuleStatusReqVO {
    /** 客户端读取时的规则版本 */
    @NotNull(message = "规则版本不能为空")
    @PositiveOrZero(message = "规则版本不能小于 0")
    private Integer version;
    /** 目标状态 */
    @NotNull(message = "目标状态不能为空")
    @Min(value = 0, message = "目标状态不合法")
    @Max(value = 1, message = "目标状态不合法")
    private Integer status;
    /** 状态变更原因 */
    @NotBlank(message = "状态变更原因不能为空")
    @Size(max = 255, message = "状态变更原因长度不能超过 255")
    private String reason;
}
