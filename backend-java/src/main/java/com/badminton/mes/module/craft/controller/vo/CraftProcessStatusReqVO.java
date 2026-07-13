package com.badminton.mes.module.craft.controller.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 工序启停状态修改请求 VO。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
public class CraftProcessStatusReqVO {

    /** 客户端读取工序时获得的版本号 */
    @NotNull(message = "工序版本不能为空")
    @PositiveOrZero(message = "工序版本不能小于 0")
    private Integer version;

    /** 状态：1 启用 0 停用 */
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态取值为 0 或 1")
    @Max(value = 1, message = "状态取值为 0 或 1")
    private Integer status;

    /** 启停原因 */
    @NotBlank(message = "启停原因不能为空")
    @Size(max = 255, message = "启停原因长度不能超过 255")
    private String reason;
}
