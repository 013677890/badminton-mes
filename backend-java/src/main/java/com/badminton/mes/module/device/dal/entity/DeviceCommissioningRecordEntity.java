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

/** 设备联调记录实体。 */
@Data
@Entity
@DynamicInsert
@Table(name = "device_commissioning_record")
public class DeviceCommissioningRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "access_config_id")
    private Long accessConfigId;

    @Column(name = "test_time")
    private LocalDateTime testTime;

    @Column(name = "tester_user_id")
    private Long testerUserId;

    @Column(name = "communication_result")
    private String communicationResult;

    @Column(name = "data_format_result")
    private String dataFormatResult;

    @Column(name = "test_result")
    private String testResult;

    @Column(name = "issue_description")
    private String issueDescription;

    @Column(name = "sample_payload")
    private String samplePayload;

    @Column(name = "create_by")
    private Long createBy;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;
}
