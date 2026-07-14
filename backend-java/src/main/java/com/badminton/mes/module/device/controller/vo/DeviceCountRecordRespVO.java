package com.badminton.mes.module.device.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/** 设备计数记录响应。 */
@Data
public class DeviceCountRecordRespVO {

    private Long id;
    private Long accessConfigId;
    private Long equipmentId;
    private String equipmentCode;
    private String collectionPointCode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectedAt;

    private String serialNumber;
    private Long rawCount;
    private Long incrementCount;
    private String runtimeStatus;
    private String faultStatus;
    private Long productionTaskId;
    private Long processId;
    private String matchStatus;
    private String reportStatus;
    private String rawPayload;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
