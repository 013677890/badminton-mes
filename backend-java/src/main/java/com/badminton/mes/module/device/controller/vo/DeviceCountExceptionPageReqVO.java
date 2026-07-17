package com.badminton.mes.module.device.controller.vo;

import java.time.LocalDateTime;

import com.badminton.mes.common.core.PageParam;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备计数异常分页筛选条件。
 *
 * <p>支持按异常来源配置、设备、人工处理状态和异常记录创建时间组合过滤。起止时间均包含边界值，
 * 未提供的条件不参与筛选；两个时间字段独立可选，当前不额外校验起止先后关系，分页结果优先返回
 * 最近创建的异常。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceCountExceptionPageReqVO extends PageParam {

    /** 触发异常的设备接入配置主键；提供时必须为正整数。 */
    @Positive(message = "设备接入配置必须为正整数")
    private Long accessConfigId;

    /** 异常涉及的设备台账主键；提供时必须为正整数。 */
    @Positive(message = "设备台账必须为正整数")
    private Long equipmentId;

    /** 人工处理状态：待处理、已解决或已忽略。 */
    @Pattern(regexp = "^(PENDING|RESOLVED|IGNORED)$", message = "异常处理状态不合法")
    private String processingStatus;

    /** 异常记录创建时间下界，查询包含与该时间相等的记录。 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createStartTime;

    /** 异常记录创建时间上界，查询包含与该时间相等的记录。 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createEndTime;
}
