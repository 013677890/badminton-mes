package com.badminton.mes.module.andon.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/** 安灯异常原因响应。 */
@Data
public class AndonReasonRespVO {

    private Long id;
    private String reasonCode;
    private String reasonName;
    private Long andonTypeId;
    private String andonTypeCode;
    private String andonTypeName;
    private String reasonDescription;
    private Integer enabledStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
