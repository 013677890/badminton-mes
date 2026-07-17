package com.badminton.mes.module.integration.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 设备计数异常池分页行响应。
 *
 * <p>同时返回首次失败证据、最新重试快照和处理结果引用，供异常池页面判断当前状态并追溯
 * 修正后的计数记录。内部设备/派工主键不直接暴露，页面使用业务编码和结果主键定位。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
public class DeviceCountExceptionRespVO {

    /** 异常池主键。 */
    private Long id;

    /** 计数来源系统。 */
    private String sourceSystem;

    /** 来源系统幂等键。 */
    private String externalKey;

    /** 发生异常的设备编码。 */
    private String equipmentCode;

    /** 上报派工单号。 */
    private String dispatchNo;

    /** 上报工序编码。 */
    private String processCode;

    /** 设备采集业务时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectTime;

    /** 导致异常的原始累计计数值。 */
    private Long countValue;

    /** 首次失败请求 JSON 快照。 */
    private String requestSnapshot;

    /** 最后一次修正重试请求 JSON 快照。 */
    private String retryRequestSnapshot;

    /** 机器可识别的异常类型。 */
    private String exceptionType;

    /** 异常错误码 */
    private String errorCode;

    /** 异常原因 */
    private String errorMessage;

    /** 处理状态：0 待处理、1 已处理、2 已忽略。 */
    private Integer handleStatus;

    /** 处理人 */
    private Long handleBy;

    /** 处理时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;

    /** 处理说明 */
    private String handleRemark;

    /** 修正重试后的接口日志主键。 */
    private Long retryLogId;

    /** 修正重试成功生成的计数记录主键。 */
    private Long retryRecordId;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
