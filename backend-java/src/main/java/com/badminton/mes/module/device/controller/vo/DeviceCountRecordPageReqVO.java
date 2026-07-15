package com.badminton.mes.module.device.controller.vo;

import java.time.LocalDateTime;

import com.badminton.mes.common.core.PageParam;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备计数原始记录分页筛选条件。
 *
 * <p>支持按接入配置、设备、生产任务匹配状态和设备采集时间组合过滤。时间范围以设备报文声明的
 * 采集业务时间为准，起止值均包含在结果中，不使用服务端记录入库时间代替；两个时间字段独立可选，
 * 当前不额外校验起止先后关系。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceCountRecordPageReqVO extends PageParam {

    /** 接收报文时命中的设备接入配置主键；提供时必须为正整数。 */
    @Positive(message = "设备接入配置必须为正整数")
    private Long accessConfigId;

    /** 接收报文时绑定的设备台账主键；提供时必须为正整数。 */
    @Positive(message = "设备台账必须为正整数")
    private Long equipmentId;

    /** 生产任务匹配状态：待匹配、已匹配或异常。 */
    @Pattern(regexp = "^(PENDING|MATCHED|EXCEPTION)$", message = "匹配状态不合法")
    private String matchStatus;

    /** 设备采集时间下界，查询包含与该时间相等的记录。 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectedStartTime;

    /** 设备采集时间上界，查询包含与该时间相等的记录。 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectedEndTime;
}
