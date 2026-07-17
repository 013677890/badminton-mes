package com.badminton.mes.module.system.controller.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 小程序账号注册请求 VO。
 *
 * @author Codex
 * @date 2026/07/16
 */
@Data
public class AuthRegisterReqVO {

    /** 工号 */
    @NotBlank(message = "工号不能为空")
    @Size(max = 32, message = "工号长度不能超过 32")
    private String userNo;

    /** 姓名 */
    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名长度不能超过 64")
    private String userName;

    /** 登录密码 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度必须在 6 到 32 之间")
    private String password;

    /** 自助注册职位 id */
    @NotNull(message = "请选择职位")
    @Positive(message = "职位 id 必须为正数")
    private Long roleId;
}
