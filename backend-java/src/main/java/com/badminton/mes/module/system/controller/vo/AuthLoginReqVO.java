package com.badminton.mes.module.system.controller.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求 VO。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
public class AuthLoginReqVO {

    /** 工号 */
    @NotBlank(message = "工号不能为空")
    @Size(max = 32, message = "工号长度不能超过 32")
    private String userNo;

    /** 密码(明文仅存在于请求过程，不落库不打日志) */
    @NotBlank(message = "密码不能为空")
    @Size(max = 64, message = "密码长度不能超过 64")
    private String password;
}
