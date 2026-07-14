package com.badminton.mes.module.andon.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/** 安灯类型响应。 */
@Data
public class AndonTypeRespVO {

    private Long id;
    private String typeCode;
    private String typeName;
    private String exceptionCategory;
    private String handlingMode;
    private Integer responseMinutes;
    private String responsibleRoleCode;
    private String notificationChannels;
    private Boolean lightControlEnabled;
    private Integer enabledStatus;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
