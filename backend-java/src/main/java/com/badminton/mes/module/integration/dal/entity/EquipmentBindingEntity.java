package com.badminton.mes.module.integration.dal.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 设备报工绑定配置实体。
 *
 * @author Codex
 * @date 2026/07/13
 */
@Data
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "integration_equipment_binding")
public class EquipmentBindingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "equipment_code")
    private String equipmentCode;

    @Column(name = "line_id")
    private Long lineId;

    @Column(name = "process_id")
    private Long processId;

    @Column(name = "default_employee_id")
    private Long defaultEmployeeId;

    @Column(name = "is_auto_report")
    private Boolean autoReport;

    @Column(name = "max_increment")
    private Long maxIncrement;

    @Column(name = "status")
    private Integer status;

    @Column(name = "create_by")
    private Long createBy;

    @Column(name = "update_by")
    private Long updateBy;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    @Column(name = "is_deleted")
    private Boolean deleted;
}
