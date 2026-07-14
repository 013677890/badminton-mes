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

/** 设备计数原始记录实体。 */
@Data
@Entity
@DynamicInsert
@Table(name = "device_count_record")
public class DeviceCountRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "access_config_id")
    private Long accessConfigId;

    @Column(name = "equipment_id")
    private Long equipmentId;

    @Column(name = "equipment_code_snapshot")
    private String equipmentCodeSnapshot;

    @Column(name = "collection_point_code_snapshot")
    private String collectionPointCodeSnapshot;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @Column(name = "serial_number")
    private String serialNumber;

    @Column(name = "raw_count")
    private Long rawCount;

    @Column(name = "increment_count")
    private Long incrementCount;

    @Column(name = "runtime_status")
    private String runtimeStatus;

    @Column(name = "fault_status")
    private String faultStatus;

    @Column(name = "production_task_id")
    private Long productionTaskId;

    @Column(name = "process_id")
    private Long processId;

    @Column(name = "match_status")
    private String matchStatus;

    @Column(name = "report_status")
    private String reportStatus;

    @Column(name = "deduplication_key")
    private String deduplicationKey;

    @Column(name = "raw_payload")
    private String rawPayload;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;
}
