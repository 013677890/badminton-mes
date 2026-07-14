package com.badminton.mes.module.device.controller.vo;

import java.time.LocalDateTime;

import com.badminton.mes.common.core.PageParam;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 设备计数记录分页请求。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceCountRecordPageReqVO extends PageParam {

    @Positive(message = "设备接入配置必须为正整数")
    private Long accessConfigId;

    @Positive(message = "设备台账必须为正整数")
    private Long equipmentId;

    @Pattern(regexp = "^(PENDING|MATCHED|EXCEPTION)$", message = "匹配状态不合法")
    private String matchStatus;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectedStartTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectedEndTime;
}
