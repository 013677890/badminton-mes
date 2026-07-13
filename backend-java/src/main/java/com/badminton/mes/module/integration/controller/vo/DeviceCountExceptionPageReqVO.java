package com.badminton.mes.module.integration.controller.vo;

import java.time.LocalDateTime;

import com.badminton.mes.common.core.PageParam;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备计数异常池分页查询参数。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceCountExceptionPageReqVO extends PageParam {

    /** 来源系统 */
    @Size(max = 32, message = "来源系统长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "来源系统包含不支持的字符")
    private String sourceSystem;

    /** 设备编码 */
    @Size(max = 32, message = "设备编码长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "设备编码包含不支持的字符")
    private String equipmentCode;

    /** 异常类型 */
    @Pattern(regexp = "^(DISPATCH_NOT_FOUND|DISPATCH_STATUS_INVALID|PROCESS_NOT_FOUND|"
            + "COUNT_NON_POSITIVE|COUNT_ROLLBACK)$",
            message = "异常类型不合法")
    private String exceptionType;

    /** 处理状态：0 待处理 1 已处理 2 已忽略 */
    @Min(value = 0, message = "处理状态最小值为 0")
    @Max(value = 2, message = "处理状态最大值为 2")
    private Integer handleStatus;

    /** 异常创建起始时间，包含 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 异常创建结束时间，包含 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
