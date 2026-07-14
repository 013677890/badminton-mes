package com.badminton.mes.module.device.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 设备计数上报请求。 */
@Data
public class DeviceCountReportReqVO {

    @NotBlank(message = "接入配置编码不能为空")
    @Size(max = 32, message = "接入配置编码长度不能超过 32")
    private String configCode;

    @NotBlank(message = "设备编码不能为空")
    @Size(max = 32, message = "设备编码长度不能超过 32")
    private String equipmentCode;

    @NotNull(message = "采集时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectedAt;

    @NotBlank(message = "设备端流水号不能为空")
    @Size(max = 64, message = "设备端流水号长度不能超过 64")
    private String serialNumber;

    @NotNull(message = "计数值不能为空")
    @PositiveOrZero(message = "计数值不能为负数")
    private Long countValue;

    @Pattern(regexp = "^(IDLE|RUNNING|STOPPED)$", message = "设备上报运行状态必须为 IDLE、RUNNING 或 STOPPED")
    private String runtimeStatus;

    @Size(max = 64, message = "故障状态长度不能超过 64")
    private String faultStatus;

    @Size(max = 5000, message = "原始报文长度不能超过 5000")
    private String rawPayload;
}
