package com.badminton.mes.module.integration.dal.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 设备计数异常池实体，对应 integration_device_count_exception。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
@Entity
@DynamicInsert
@Table(name = "integration_device_count_exception")
public class IntegrationDeviceCountExceptionEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 来源系统 */
    @Column(name = "source_system")
    private String sourceSystem;

    /** 来源系统内幂等键 */
    @Column(name = "external_key")
    private String externalKey;

    /** 设备编码 */
    @Column(name = "equipment_code")
    private String equipmentCode;

    /** 匹配派工单主键，可空 */
    @Column(name = "dispatch_order_id")
    private Long dispatchOrderId;

    /** 上报派工单号 */
    @Column(name = "dispatch_no")
    private String dispatchNo;

    /** 匹配工序主键，可空 */
    @Column(name = "process_id")
    private Long processId;

    /** 上报工序编码 */
    @Column(name = "process_code")
    private String processCode;

    /** 设备采集时间 */
    @Column(name = "collect_time")
    private LocalDateTime collectTime;

    /** 原始累计计数值 */
    @Column(name = "count_value")
    private Long countValue;

    /** 原始请求 JSON */
    @Column(name = "request_snapshot", columnDefinition = "json")
    private String requestSnapshot;

    /** 最后一次修正重试请求快照 */
    @Column(name = "retry_request_snapshot", columnDefinition = "json")
    private String retryRequestSnapshot;

    /** 异常类型 */
    @Column(name = "exception_type")
    private String exceptionType;

    /** 异常错误码 */
    @Column(name = "error_code")
    private String errorCode;

    /** 异常原因 */
    @Column(name = "error_message")
    private String errorMessage;

    /** 处理状态：0 待处理 1 已处理 2 已忽略 */
    @Column(name = "handle_status")
    private Integer handleStatus;

    /** 处理人 */
    @Column(name = "handle_by")
    private Long handleBy;

    /** 处理时间 */
    @Column(name = "handle_time")
    private LocalDateTime handleTime;

    /** 处理说明 */
    @Column(name = "handle_remark")
    private String handleRemark;

    /** 修正后写入日志主键 */
    @Column(name = "retry_log_id")
    private Long retryLogId;

    /** 修正后设备计数记录主键 */
    @Column(name = "retry_record_id")
    private Long retryRecordId;

    /** 调用用户 */
    @Column(name = "create_by")
    private Long createBy;

    /** 创建时间 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
