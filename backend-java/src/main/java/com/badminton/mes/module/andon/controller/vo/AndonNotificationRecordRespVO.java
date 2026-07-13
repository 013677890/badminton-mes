package com.badminton.mes.module.andon.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/** 安灯异常通知记录响应。 */
@Data
public class AndonNotificationRecordRespVO {
    private Long id;
    private String notificationType;
    private String channel;
    private Long receiverUserId;
    private String receiverRoleCode;
    private String sendStatus;
    private String sendMessage;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime sentAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime createTime;
}
