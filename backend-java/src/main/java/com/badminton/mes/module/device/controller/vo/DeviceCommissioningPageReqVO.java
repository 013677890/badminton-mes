package com.badminton.mes.module.device.controller.vo;

import java.time.LocalDateTime;

import com.badminton.mes.common.core.PageParam;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 设备联调记录分页请求。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceCommissioningPageReqVO extends PageParam {

    @Positive(message = "设备接入配置必须为正整数")
    private Long accessConfigId;

    @Pattern(regexp = "^(PASSED|FAILED)$", message = "联调结果必须为 PASSED 或 FAILED")
    private String testResult;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime testStartTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime testEndTime;
}
