package com.badminton.mes.module.system.controller.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户启用/停用请求 VO。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
public class UserStatusReqVO {

    /** 目标状态：1 启用 0 停用 */
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态取值只能为 0 或 1")
    @Max(value = 1, message = "状态取值只能为 0 或 1")
    private Integer status;
}
