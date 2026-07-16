package com.badminton.mes.module.device.controller.vo;

import java.time.LocalDateTime;

import com.badminton.mes.common.core.PageParam;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备联调记录分页筛选条件。
 *
 * <p>支持按接入配置、综合联调结论和实际联调业务时间组合过滤。起止时间均包含边界值，两个时间
 * 字段独立可选且当前不额外校验起止先后关系；查询结果按联调时间优先倒序展示，用于回溯历次测试。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceCommissioningPageReqVO extends PageParam {

    /** 被测试的设备接入配置主键；提供时必须为正整数。 */
    @Positive(message = "设备接入配置必须为正整数")
    private Long accessConfigId;

    /** 综合联调结论：通过或失败。 */
    @Pattern(regexp = "^(PASSED|FAILED)$", message = "联调结果必须为 PASSED 或 FAILED")
    private String testResult;

    /** 实际联调业务时间下界，查询包含与该时间相等的记录。 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime testStartTime;

    /** 实际联调业务时间上界，查询包含与该时间相等的记录。 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime testEndTime;
}
