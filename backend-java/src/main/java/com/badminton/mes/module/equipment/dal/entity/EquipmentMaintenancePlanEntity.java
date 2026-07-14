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
 * 设备保养计划实体，对应表 equip_maintenance_plan。
 *
 * @author 角色C
 * @date 2026/07/11
 */
@Data
@Entity
@DynamicInsert
@Table(name = "equip_maintenance_plan")
public class EquipmentMaintenancePlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_code")
    private String planCode;

    @Column(name = "plan_name")
    private String planName;

    @Column(name = "equipment_id")
    private Long equipmentId;

    @Column(name = "maintenance_type")
    private String maintenanceType;

    @Column(name = "cycle_days")
    private Integer cycleDays;

    @Column(name = "maintenance_content")
    private String maintenanceContent;

    @Column(name = "responsible_user_id")
    private Long responsibleUserId;

    @Column(name = "last_maintenance_time")
    private LocalDateTime lastMaintenanceTime;

    @Column(name = "next_maintenance_time")
    private LocalDateTime nextMaintenanceTime;

    @Column(name = "remark")
    private String remark;

    @Column(name = "status")
    private Integer status;

    @Column(name = "create_by")
    private Long createBy;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    @Column(name = "is_deleted")
    private Boolean deleted;
}
