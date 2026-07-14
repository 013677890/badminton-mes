package com.badminton.mes.module.equipment.dal.entity;

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
 * 设备保养记录实体，对应表 equip_maintenance_record。
 *
 * @author 角色C
 * @date 2026/07/11
 */
@Data
@Entity
@DynamicInsert
@Table(name = "equip_maintenance_record")
public class EquipmentMaintenanceRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_no")
    private String recordNo;

    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "equipment_id")
    private Long equipmentId;

    @Column(name = "previous_equipment_status")
    private String previousEquipmentStatus;

    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "finish_time")
    private LocalDateTime finishTime;

    @Column(name = "executor_user_id")
    private Long executorUserId;

    @Column(name = "maintenance_content")
    private String maintenanceContent;

    @Column(name = "maintenance_result")
    private String maintenanceResult;

    @Column(name = "record_status")
    private String recordStatus;

    @Column(name = "abnormal_description")
    private String abnormalDescription;

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
