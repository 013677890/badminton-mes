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

/** 设备接入配置实体。 */
@Data
@Entity
@DynamicInsert
@Table(name = "device_access_config")
public class DeviceAccessConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_code")
    private String configCode;

    @Column(name = "config_name")
    private String configName;

    @Column(name = "equipment_id")
    private Long equipmentId;

    @Column(name = "collection_point_code")
    private String collectionPointCode;

    @Column(name = "process_id")
    private Long processId;

    @Column(name = "production_line_id")
    private Long productionLineId;

    @Column(name = "data_source")
    private String dataSource;

    @Column(name = "count_mode")
    private String countMode;

    @Column(name = "spike_threshold")
    private Long spikeThreshold;

    @Column(name = "report_mode")
    private String reportMode;

    @Column(name = "commissioning_status")
    private String commissioningStatus;

    @Column(name = "enabled_status")
    private Integer enabledStatus;

    @Column(name = "last_communication_time")
    private LocalDateTime lastCommunicationTime;

    @Column(name = "remark")
    private String remark;

    @Column(name = "create_by")
    private Long createBy;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    @Column(name = "is_deleted")
    private Boolean deleted;
}
