package com.badminton.mes.module.device.controller.vo;

import java.time.LocalDateTime;

import com.badminton.mes.common.core.PageParam;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 设备计数异常分页请求。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceCountExceptionPageReqVO extends PageParam {

    @Positive(message = "设备接入配置必须为正整数")
    private Long accessConfigId;

    @Positive(message = "设备台账必须为正整数")
    private Long equipmentId;

    @Pattern(regexp = "^(PENDING|RESOLVED|IGNORED)$", message = "异常处理状态不合法")
    private String processingStatus;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createStartTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createEndTime;
}
