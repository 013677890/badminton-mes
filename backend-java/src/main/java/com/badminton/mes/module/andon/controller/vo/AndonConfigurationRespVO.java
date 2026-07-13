package com.badminton.mes.module.andon.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/** 安灯异常处理配置响应。 */
@Data
public class AndonConfigurationRespVO {

    private Long id;
    private Long andonTypeId;
    private String andonTypeCode;
    private String andonTypeName;
    private Long productionLineId;
    private Long handlerUserId;
    private String handlerRoleCode;
    private Long escalationUserId;
    private String escalationRoleCode;
    private Integer responseMinutes;
    private Integer escalationMinutes;
    private String notificationChannels;
    private Integer enabledStatus;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
