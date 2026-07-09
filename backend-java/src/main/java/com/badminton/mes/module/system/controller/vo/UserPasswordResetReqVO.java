package com.badminton.mes.module.system.controller.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员重置用户密码请求 VO。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
public class UserPasswordResetReqVO {

    /** 新密码 */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "新密码长度必须在 6 到 32 之间")
    private String newPassword;
}
