package com.badminton.mes.module.system.controller.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改本人密码请求 VO。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
public class AuthPasswordReqVO {

    /** 旧密码 */
    @NotBlank(message = "旧密码不能为空")
    @Size(max = 64, message = "旧密码长度不能超过 64")
    private String oldPassword;

    /** 新密码 */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "新密码长度必须在 6 到 32 之间")
    private String newPassword;
}
