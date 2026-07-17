package com.badminton.mes.module.system.controller.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 微信小程序登录请求。
 *
 * @author Codex
 * @date 2026/07/15
 */
@Data
public class MiniAppLoginReqVO {

    /** wx.login 返回的临时 code */
    @NotBlank(message = "微信登录 code 不能为空")
    @Size(max = 256, message = "微信登录 code 长度不能超过 256 个字符")
    private String code;
}
