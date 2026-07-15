package com.badminton.mes.module.device.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 设备计数异常详情响应。
 *
 * <p>提供异常来源、机器可识别类型、面向人员的原因以及人工处置审计信息，便于从异常记录
 * 反查原始计数。处理状态一旦离开待处理态，业务层不允许再次覆盖处置结论。
 */
@Data
public class DeviceCountExceptionRespVO {

    /** 异常记录主键。 */
    private Long id;
    /** 触发异常的计数原始记录主键。 */
    private Long countRecordId;
    /** 异常发生时命中的接入配置主键。 */
    private Long accessConfigId;
    /** 异常涉及的设备台账主键。 */
    private Long equipmentId;
    /** 异常分类编码，如计数回退或异常跳变。 */
    private String exceptionType;
    /** 异常判定原因及上下文。 */
    private String exceptionReason;
    /** 人工处置状态：{@code PENDING} 待处理、{@code RESOLVED} 已解决、{@code IGNORED} 已忽略。 */
    private String processingStatus;
    /** 执行最终处置的用户主键；待处理时为空。 */
    private Long processedBy;

    /** 异常首次完成终态处置的服务器时间；待处理时为空。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processedAt;

    /** 人工核实、修正或忽略异常的结论；待处理时为空。 */
    private String processingResult;

    /** 计数接入流程生成异常记录的服务端时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 异常记录最近一次持久化更新的服务端时间，通常随人工处置刷新。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
