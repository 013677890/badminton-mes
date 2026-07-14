package com.badminton.mes.module.device.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/** 设备联调记录响应。 */
@Data
public class DeviceCommissioningRespVO {

    private Long id;
    private Long accessConfigId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime testTime;

    private Long testerUserId;
    private String communicationResult;
    private String dataFormatResult;
    private String testResult;
    private String issueDescription;
    private String samplePayload;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
