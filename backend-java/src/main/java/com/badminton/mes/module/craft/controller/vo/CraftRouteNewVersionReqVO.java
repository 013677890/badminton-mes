package com.badminton.mes.module.craft.controller.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 基于历史路线创建新版本请求 VO。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
public class CraftRouteNewVersionReqVO {

    /** 源路线客户端预期版本号 */
    @NotNull(message = "源路线版本号不能为空")
    @PositiveOrZero(message = "源路线版本号不能小于 0")
    private Integer version;

    /** 新业务版本 */
    @NotBlank(message = "新路线版本不能为空")
    @Size(max = 32, message = "新路线版本长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "路线版本只能包含字母、数字、点、下划线和连字符")
    private String newRoutingVersion;

    /** 创建新版本原因 */
    @NotBlank(message = "创建新版本原因不能为空")
    @Size(max = 255, message = "创建新版本原因长度不能超过 255")
    private String reason;
}
