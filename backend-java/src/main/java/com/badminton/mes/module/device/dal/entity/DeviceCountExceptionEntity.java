package com.badminton.mes.module.device.dal.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 设备计数异常持久化实体。
 *
 * <p>当设备不可用、工序未配置、累计计数回退或增量异常跳变时，与原始计数记录配套保存异常事实。
 * 异常处理采用从待处理到已解决或已忽略的单向状态流转，并通过加锁更新防止多人重复处置覆盖审计结论。
 */
@Data
@Entity
@DynamicInsert
@Table(name = "device_count_exception")
public class DeviceCountExceptionEntity {

    /** 异常记录主键。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 触发异常的计数原始记录主键，确保异常可回溯至报文事实。 */
    @Column(name = "count_record_id")
    private Long countRecordId;

    /** 异常发生时使用的接入配置主键。 */
    @Column(name = "access_config_id")
    private Long accessConfigId;

    /** 异常涉及的设备台账主键。 */
    @Column(name = "equipment_id")
    private Long equipmentId;

    /** 机器可识别的异常类型，如设备停用、计数回退或计数跳变。 */
    @Column(name = "exception_type")
    private String exceptionType;

    /** 面向操作人员的异常原因和判定上下文。 */
    @Column(name = "exception_reason")
    private String exceptionReason;

    /** 处理状态；新异常为待处理，后续只能结束为已解决或已忽略。 */
    @Column(name = "processing_status")
    private String processingStatus;

    /** 最终处置异常的操作人主键。 */
    @Column(name = "processed_by")
    private Long processedBy;

    /** 异常完成处置的服务器时间。 */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /** 人工核实、修正或忽略异常的处理结论。 */
    @Column(name = "processing_result")
    private String processingResult;

    /** 数据库生成的异常创建时间。 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 数据库维护的最近更新时间，反映异常处置变更。 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;
}
