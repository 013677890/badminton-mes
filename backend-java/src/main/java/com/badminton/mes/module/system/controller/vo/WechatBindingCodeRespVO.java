package com.badminton.mes.module.system.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/** 微信绑定小程序码响应。 */
@Data
public class WechatBindingCodeRespVO {

    private String ticket;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;

    /** PNG 图片的纯 Base64 内容，不含 data URL 前缀。 */
    private String codeImageBase64;

    private String status;
}
