package com.badminton.mes.module.system.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/** 扫码绑定目标账号的脱敏预览。 */
@Data
public class WechatBindingCodePreviewRespVO {

    private String userName;

    private String maskedUserNo;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;

    private String status;
}
