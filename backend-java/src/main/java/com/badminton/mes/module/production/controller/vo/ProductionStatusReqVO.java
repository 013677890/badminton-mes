package com.badminton.mes.module.production.controller.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/** 生产基础资料状态变更请求。 */
@Data
public class ProductionStatusReqVO {
    /** 客户端读取时的版本 */
    @NotNull(message = "数据版本不能为空")
    @PositiveOrZero(message = "数据版本不能小于 0")
    private Integer version;
    /** 目标状态 */
    @NotNull(message = "目标状态不能为空")
    @Min(value = 0, message = "目标状态不合法")
    @Max(value = 1, message = "目标状态不合法")
    private Integer status;
}
