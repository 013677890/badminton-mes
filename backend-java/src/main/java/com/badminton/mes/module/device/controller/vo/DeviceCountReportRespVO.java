package com.badminton.mes.module.device.controller.vo;

import lombok.Data;

/** 设备计数上报处理结果。 */
@Data
public class DeviceCountReportRespVO {

    private Long countRecordId;
    private Long incrementCount;
    private String matchStatus;
    private String reportStatus;
    private String exceptionType;
    private String processingMessage;
}
