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

/** 设备计数异常实体。 */
@Data
@Entity
@DynamicInsert
@Table(name = "device_count_exception")
public class DeviceCountExceptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "count_record_id")
    private Long countRecordId;

    @Column(name = "access_config_id")
    private Long accessConfigId;

    @Column(name = "equipment_id")
    private Long equipmentId;

    @Column(name = "exception_type")
    private String exceptionType;

    @Column(name = "exception_reason")
    private String exceptionReason;

    @Column(name = "processing_status")
    private String processingStatus;

    @Column(name = "processed_by")
    private Long processedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "processing_result")
    private String processingResult;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;
}
