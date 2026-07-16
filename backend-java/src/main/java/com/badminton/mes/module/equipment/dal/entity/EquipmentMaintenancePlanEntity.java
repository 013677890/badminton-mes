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
 * 设备保养计划持久化实体，逐字段映射数据库表 {@code equip_maintenance_plan}。
 *
 * <p>计划描述设备应按何种周期执行哪些保养内容。最近和下次保养时间会在任务完成时由 Service
 * 重新计算；实体仅保存设备和用户主键，不声明跨表级联，避免删除计划时误删历史任务。
 * {@link DynamicInsert} 使未显式赋值的列继续采用数据库默认值。
 *
 * @author 角色C
 * @date 2026/07/11
 */
@Data
@Entity
@DynamicInsert
@Table(name = "equip_maintenance_plan")
public class EquipmentMaintenancePlanEntity {

    /** 映射主键列 {@code id}；数据库自增生成，供保养任务稳定引用。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 映射 {@code plan_code}；业务唯一计划编码，逻辑删除时会改写以释放唯一键。 */
    @Column(name = "plan_code")
    private String planCode;

    /** 映射 {@code plan_name}；供业务人员识别、展示和关键字检索的计划名称。 */
    @Column(name = "plan_name")
    private String planName;

    /** 映射 {@code equipment_id}；计划绑定的设备台账主键，任务创建时据此固化设备。 */
    @Column(name = "equipment_id")
    private Long equipmentId;

    /** 映射 {@code maintenance_type}；保养类型：ROUTINE、PREVENTIVE 或 SPECIAL。 */
    @Column(name = "maintenance_type")
    private String maintenanceType;

    /** 映射 {@code cycle_days}；保养周期天数，用于从完成时间推算下次执行时间。 */
    @Column(name = "cycle_days")
    private Integer cycleDays;

    /** 映射 {@code maintenance_content}；计划规定的标准作业内容，创建任务时可作为执行依据。 */
    @Column(name = "maintenance_content")
    private String maintenanceContent;

    /** 映射 {@code responsible_user_id}；默认负责人用户主键，可空且不建立用户实体级联。 */
    @Column(name = "responsible_user_id")
    private Long responsibleUserId;

    /** 映射 {@code last_maintenance_time}；最近一次已完成任务的完成时间，尚无历史时可空。 */
    @Column(name = "last_maintenance_time")
    private LocalDateTime lastMaintenanceTime;

    /** 映射 {@code next_maintenance_time}；下一次计划执行时间，供到期筛选和任务安排。 */
    @Column(name = "next_maintenance_time")
    private LocalDateTime nextMaintenanceTime;

    /** 映射 {@code remark}；保存计划适用条件等非结构化补充说明。 */
    @Column(name = "remark")
    private String remark;

    /** 映射 {@code status}；计划启停状态，{@code 1} 启用、{@code 0} 停用。 */
    @Column(name = "status")
    private Integer status;

    /** 映射 {@code create_by}；创建计划的系统用户主键，供审计追溯。 */
    @Column(name = "create_by")
    private Long createBy;

    /** 映射 {@code create_time}；数据库生成的创建时间，应用不参与插入和更新。 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 映射 {@code update_time}；数据库维护的最后更新时间，应用不参与插入和更新。 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    /** 映射 {@code is_deleted}；逻辑删除标记，正常查询必须排除值为 {@code true} 的计划。 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
