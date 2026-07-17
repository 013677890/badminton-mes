package com.badminton.mes.module.system.controller.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 微信扫码确认绑定请求。 */
@Data
public class MiniAppBindByCodeReqVO {

    /** 小程序码携带的一次性票据。 */
    @NotBlank(message = "绑定票据不能为空")
    @Pattern(regexp = "^[0-9a-f]{32}$", message = "绑定票据格式不正确")
    private String ticket;

    /** 扫码微信通过 wx.login 获取的临时 code。 */
    @NotBlank(message = "微信登录凭证不能为空")
    @Size(max = 128, message = "微信登录凭证长度不能超过 128 个字符")
    private String code;
}
