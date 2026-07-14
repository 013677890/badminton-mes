package com.badminton.mes.module.device.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/** 设备接入配置响应。 */
@Data
public class DeviceAccessConfigRespVO {

    private Long id;
    private String configCode;
    private String configName;
    private Long equipmentId;
    private String collectionPointCode;
    private Long processId;
    private Long productionLineId;
    private String dataSource;
    private String countMode;
    private Long spikeThreshold;
    private String reportMode;
    private String commissioningStatus;
    private Integer enabledStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastCommunicationTime;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
