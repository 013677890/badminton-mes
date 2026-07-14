package com.badminton.mes.module.andon.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/** 安灯异常处理过程响应。 */
@Data
public class AndonProcessLogRespVO {
    private Long id;
    private String actionType;
    private String fromStatus;
    private String toStatus;
    private Long operatorId;
    private Long targetUserId;
    private String targetRoleCode;
    private String actionContent;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
