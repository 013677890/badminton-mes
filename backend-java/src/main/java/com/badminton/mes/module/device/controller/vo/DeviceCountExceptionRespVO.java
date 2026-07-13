package com.badminton.mes.module.device.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/** 设备计数异常响应。 */
@Data
public class DeviceCountExceptionRespVO {

    private Long id;
    private Long countRecordId;
    private Long accessConfigId;
    private Long equipmentId;
    private String exceptionType;
    private String exceptionReason;
    private String processingStatus;
    private Long processedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processedAt;

    private String processingResult;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
