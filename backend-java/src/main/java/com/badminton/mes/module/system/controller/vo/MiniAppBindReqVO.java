package com.badminton.mes.module.system.controller.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 微信小程序首次绑定 MES 工号请求。
 *
 * @author Codex
 * @date 2026/07/15
 */
@Data
public class MiniAppBindReqVO {

    /** 微信登录返回的短期绑定票据 */
    @NotBlank(message = "绑定票据不能为空")
    @Size(max = 64, message = "绑定票据长度不能超过 64 个字符")
    private String bindTicket;

    /** MES 工号 */
    @NotBlank(message = "工号不能为空")
    @Size(max = 32, message = "工号长度不能超过 32 个字符")
    private String userNo;

    /** MES 密码 */
    @NotBlank(message = "密码不能为空")
    @Size(max = 128, message = "密码长度不能超过 128 个字符")
    private String password;
}
