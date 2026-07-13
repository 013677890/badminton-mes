package com.badminton.mes.module.integration.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 设备计数异常池分页行响应。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
public class DeviceCountExceptionRespVO {

    /** 异常主键 */
    private Long id;

    /** 来源系统 */
    private String sourceSystem;

    /** 来源幂等键 */
    private String externalKey;

    /** 设备编码 */
    private String equipmentCode;

    /** 派工单号 */
    private String dispatchNo;

    /** 工序编码 */
    private String processCode;

    /** 设备采集时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectTime;

    /** 原始累计计数值 */
    private Long countValue;

    /** 原始请求 JSON */
    private String requestSnapshot;

    /** 异常类型 */
    private String exceptionType;

    /** 异常错误码 */
    private String errorCode;

    /** 异常原因 */
    private String errorMessage;

    /** 处理状态 */
    private Integer handleStatus;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
