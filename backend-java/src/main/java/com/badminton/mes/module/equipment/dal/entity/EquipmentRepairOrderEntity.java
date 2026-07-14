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
 * 设备报修任务实体，对应表 equip_repair_order。
 *
 * @author 角色C
 * @date 2026/07/10
 */
@Data
@Entity
@DynamicInsert
@Table(name = "equip_repair_order")
public class EquipmentRepairOrderEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 报修单号，唯一 */
    @Column(name = "repair_no")
    private String repairNo;

    /** 设备台账 id */
    @Column(name = "equipment_id")
    private Long equipmentId;

    /** 故障原理 id */
    @Column(name = "fault_principle_id")
    private Long faultPrincipleId;

    /** 故障描述 */
    @Column(name = "fault_description")
    private String faultDescription;

    /** 报修时间 */
    @Column(name = "report_time")
    private LocalDateTime reportTime;

    /** 报修人用户 id */
    @Column(name = "report_user_id")
    private Long reportUserId;

    /** 维修人用户 id */
    @Column(name = "repair_user_id")
    private Long repairUserId;

    /** 维修开始时间 */
    @Column(name = "repair_start_time")
    private LocalDateTime repairStartTime;

    /** 维修结束时间 */
    @Column(name = "repair_end_time")
    private LocalDateTime repairEndTime;

    /** 维修结果 */
    @Column(name = "repair_result")
    private String repairResult;

    /** 报修状态：REPORTED/ASSIGNED/REPAIRING/FINISHED/CANCELLED */
    @Column(name = "repair_status")
    private String repairStatus;

    /** 备注说明 */
    @Column(name = "remark")
    private String remark;

    /** 创建人用户 id */
    @Column(name = "create_by")
    private Long createBy;

    /** 创建时间 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(name = "update_time", insertable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
