package com.badminton.mes.module.craft.controller.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 工艺路线审核或停用请求 VO。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
public class CraftRouteStatusReqVO {

    /** 客户端读取路线时获得的版本号 */
    @NotNull(message = "路线版本号不能为空")
    @PositiveOrZero(message = "路线版本号不能小于 0")
    private Integer version;

    /** 状态变更原因 */
    @NotBlank(message = "状态变更原因不能为空")
    @Size(max = 255, message = "状态变更原因长度不能超过 255")
    private String reason;
}
