package com.badminton.mes.module.andon.dal.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 现场安灯异常事件实体，保存一次异常从发起到关闭的当前流程快照。
 *
 * <p>事件关联的处理日志和通知记录分别存放在独立表中；本实体聚合异常上下文、当前责任主体、
 * 响应与升级时限、灯态以及最终处置结果，供状态流转和超时扫描使用。
 */
@Data
@Entity
@DynamicInsert
@Table(name = "andon_event")
public class AndonEventEntity {

    /** 事件主键，也是过程日志、通知记录及详情缓存关联事件的稳定标识。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** 面向业务人员展示和检索的事件单号，区别于数据库自增主键。 */
    @Column(name = "event_no") private String eventNo;
    /** 事件所属安灯类型，决定处理模式、默认责任主体和时限规则。 */
    @Column(name = "andon_type_id") private Long andonTypeId;
    /** 发起事件时选择的初步原因，可在处理阶段进一步核实。 */
    @Column(name = "reason_id") private Long reasonId;
    /** 处理完成时确认的实际原因，用于复盘初判与最终结论的差异。 */
    @Column(name = "actual_reason_id") private Long actualReasonId;
    /** 事件入口来源，如人工上报或系统联动，用于区分触发链路。 */
    @Column(name = "source_channel") private String sourceChannel;
    /** 异常严重程度，用于列表筛选和处置优先级判断。 */
    @Column(name = "severity") private String severity;
    /** 异常发生车间标识，限定事件的生产组织上下文。 */
    @Column(name = "workshop_id") private Long workshopId;
    /** 异常发生或配置匹配所依据的产线标识。 */
    @Column(name = "production_line_id") private Long productionLineId;
    /** 异常关联工单标识，用于追溯受影响的生产指令。 */
    @Column(name = "work_order_id") private Long workOrderId;
    /** 异常关联生产任务标识，用于定位更细粒度的执行任务。 */
    @Column(name = "production_task_id") private Long productionTaskId;
    /** 异常关联工序标识，用于定位生产流程中的发生环节。 */
    @Column(name = "process_id") private Long processId;
    /** 异常关联设备标识，表示故障或处置涉及的现场设备。 */
    @Column(name = "equipment_id") private Long equipmentId;
    /** 异常关联质量记录标识，用于质量类事件的来源追溯。 */
    @Column(name = "quality_record_id") private Long qualityRecordId;
    /** 受影响物料或产品批次号，支持按批次追踪异常影响。 */
    @Column(name = "batch_no") private String batchNo;
    /** 发起人填写的异常现象和现场情况说明。 */
    @Column(name = "description") private String description;
    /** 现场图片或文件地址的持久化文本，由上层约定具体序列化格式。 */
    @Column(name = "attachment_urls", columnDefinition = "TEXT") private String attachmentUrls;
    /** 事件当前流程状态，是确认、处理、完成和关闭等动作的状态机依据。 */
    @Column(name = "event_status") private String eventStatus;
    /** 当前明确指派的处理用户；与角色指派共同描述责任主体。 */
    @Column(name = "assigned_user_id") private Long assignedUserId;
    /** 当前指派的处理角色编码，供角色成员进行权限判断和认领处理。 */
    @Column(name = "assigned_role_code") private String assignedRoleCode;
    /** 首次响应截止时间，超时扫描据此识别未及时响应的事件。 */
    @Column(name = "response_deadline") private LocalDateTime responseDeadline;
    /** 升级处置截止时间，达到后事件进入升级通知候选范围。 */
    @Column(name = "escalation_deadline") private LocalDateTime escalationDeadline;
    /** 响应或升级是否超时的派生状态，与主流程状态分开记录。 */
    @Column(name = "timeout_status") private String timeoutStatus;
    /** 现场安灯设备当前灯态，反映事件对灯控链路的期望结果。 */
    @Column(name = "light_status") private String lightStatus;
    /** 最近一次灯控结果或失败原因，便于排查设备联动问题。 */
    @Column(name = "light_message") private String lightMessage;
    /** 事件处理完成时填写的处置结果和恢复情况。 */
    @Column(name = "processing_result") private String processingResult;
    /** 异常造成的生产影响时长，单位为分钟。 */
    @Column(name = "impact_minutes") private Integer impactMinutes;
    /** 经处理确认的受影响产品或物料数量。 */
    @Column(name = "affected_quantity") private Integer affectedQuantity;
    /** 发起事件的用户标识，保留事件来源责任链。 */
    @Column(name = "initiated_by") private Long initiatedBy;
    /** 执行事件确认动作的用户标识。 */
    @Column(name = "confirmed_by") private Long confirmedBy;
    /** 事件被确认受理的时间。 */
    @Column(name = "confirmed_at") private LocalDateTime confirmedAt;
    /** 提交事件处理完成结果的用户标识。 */
    @Column(name = "completed_by") private Long completedBy;
    /** 事件完成处置的时间。 */
    @Column(name = "completed_at") private LocalDateTime completedAt;
    /** 最终关闭事件的用户标识。 */
    @Column(name = "closed_by") private Long closedBy;
    /** 事件完成复核并关闭的时间。 */
    @Column(name = "closed_at") private LocalDateTime closedAt;
    /** 数据库生成的事件创建时间，不由应用插入或更新。 */
    @Column(name = "create_time", insertable = false, updatable = false) private LocalDateTime createTime;
    /** 数据库维护的最近更新时间，不由应用直接覆盖。 */
    @Column(name = "update_time", insertable = false, updatable = false) private LocalDateTime updateTime;
    /** 逻辑删除标记；查询活动事件时必须排除已删除数据。 */
    @Column(name = "is_deleted") private Boolean deleted;
}
