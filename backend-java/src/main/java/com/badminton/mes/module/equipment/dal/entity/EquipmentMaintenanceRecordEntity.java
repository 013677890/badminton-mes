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
 * 设备保养记录持久化实体，逐字段映射数据库表 {@code equip_maintenance_record}。
 *
 * <p>一条记录既代表可执行的保养任务，也保存完成后的历史凭据。任务开始时会快照设备原状态，
 * 结束时据此恢复设备；已完成或已取消记录不可再修改，以保证审计信息稳定。计划、设备和用户
 * 均以主键关联而不建立 JPA 级联，{@link DynamicInsert} 使未赋值列沿用数据库默认值。
 *
 * @author 角色C
 * @date 2026/07/11
 */
@Data
@Entity
@DynamicInsert
@Table(name = "equip_maintenance_record")
public class EquipmentMaintenanceRecordEntity {

    /** 映射主键列 {@code id}；数据库自增生成，是保养状态流转的稳定标识。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 映射 {@code record_no}；业务唯一任务编号，逻辑删除时会改写以释放唯一键。 */
    @Column(name = "record_no")
    private String recordNo;

    /** 映射 {@code plan_id}；产生该任务的保养计划主键，用于历史归属和周期回写。 */
    @Column(name = "plan_id")
    private Long planId;

    /** 映射 {@code equipment_id}；实际保养设备的台账主键，创建任务时从计划固化。 */
    @Column(name = "equipment_id")
    private Long equipmentId;

    /** 映射 {@code previous_equipment_status}；进入保养前的设备状态，供结束任务时安全恢复。 */
    @Column(name = "previous_equipment_status")
    private String previousEquipmentStatus;

    /** 映射 {@code scheduled_time}；计划执行时间，用于任务排程和时间范围筛选。 */
    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;

    /** 映射 {@code start_time}；任务真正进入执行中的时间。 */
    @Column(name = "start_time")
    private LocalDateTime startTime;

    /** 映射 {@code finish_time}；任务完成时间，用于更新计划周期和统计执行耗时。 */
    @Column(name = "finish_time")
    private LocalDateTime finishTime;

    /** 映射 {@code executor_user_id}；实际执行人用户主键，未开始或未指定时可空。 */
    @Column(name = "executor_user_id")
    private Long executorUserId;

    /** 映射 {@code maintenance_content}；本次任务实际执行内容，也是历史审计凭据。 */
    @Column(name = "maintenance_content")
    private String maintenanceContent;

    /** 映射 {@code maintenance_result}；保养结论 NORMAL 或 ABNORMAL，未完成时为空。 */
    @Column(name = "maintenance_result")
    private String maintenanceResult;

    /** 映射 {@code record_status}；状态机值：PENDING、IN_PROGRESS、COMPLETED 或 CANCELLED。 */
    @Column(name = "record_status")
    private String recordStatus;

    /** 映射 {@code abnormal_description}；结果异常时记录现象、原因或后续处理建议。 */
    @Column(name = "abnormal_description")
    private String abnormalDescription;

    /** 映射 {@code remark}；保存任务执行限制等非结构化补充说明。 */
    @Column(name = "remark")
    private String remark;

    /** 映射 {@code create_by}；创建任务的系统用户主键，供审计追溯。 */
    @Column(name = "create_by")
    private Long createBy;

    /** 映射 {@code create_time}；数据库生成的创建时间，应用不参与插入和更新。 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 映射 {@code update_time}；数据库维护的最后更新时间，应用不参与插入和更新。 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    /** 映射 {@code is_deleted}；逻辑删除标记，仅待处理任务允许置为 {@code true}。 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
