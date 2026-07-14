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

/** 现场安灯异常实体。 */
@Data
@Entity
@DynamicInsert
@Table(name = "andon_event")
public class AndonEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "event_no") private String eventNo;
    @Column(name = "andon_type_id") private Long andonTypeId;
    @Column(name = "reason_id") private Long reasonId;
    @Column(name = "actual_reason_id") private Long actualReasonId;
    @Column(name = "source_channel") private String sourceChannel;
    @Column(name = "severity") private String severity;
    @Column(name = "workshop_id") private Long workshopId;
    @Column(name = "production_line_id") private Long productionLineId;
    @Column(name = "work_order_id") private Long workOrderId;
    @Column(name = "production_task_id") private Long productionTaskId;
    @Column(name = "process_id") private Long processId;
    @Column(name = "equipment_id") private Long equipmentId;
    @Column(name = "quality_record_id") private Long qualityRecordId;
    @Column(name = "batch_no") private String batchNo;
    @Column(name = "description") private String description;
    @Column(name = "attachment_urls", columnDefinition = "TEXT") private String attachmentUrls;
    @Column(name = "event_status") private String eventStatus;
    @Column(name = "assigned_user_id") private Long assignedUserId;
    @Column(name = "assigned_role_code") private String assignedRoleCode;
    @Column(name = "response_deadline") private LocalDateTime responseDeadline;
    @Column(name = "escalation_deadline") private LocalDateTime escalationDeadline;
    @Column(name = "timeout_status") private String timeoutStatus;
    @Column(name = "light_status") private String lightStatus;
    @Column(name = "light_message") private String lightMessage;
    @Column(name = "processing_result") private String processingResult;
    @Column(name = "impact_minutes") private Integer impactMinutes;
    @Column(name = "affected_quantity") private Integer affectedQuantity;
    @Column(name = "initiated_by") private Long initiatedBy;
    @Column(name = "confirmed_by") private Long confirmedBy;
    @Column(name = "confirmed_at") private LocalDateTime confirmedAt;
    @Column(name = "completed_by") private Long completedBy;
    @Column(name = "completed_at") private LocalDateTime completedAt;
    @Column(name = "closed_by") private Long closedBy;
    @Column(name = "closed_at") private LocalDateTime closedAt;
    @Column(name = "create_time", insertable = false, updatable = false) private LocalDateTime createTime;
    @Column(name = "update_time", insertable = false, updatable = false) private LocalDateTime updateTime;
    @Column(name = "is_deleted") private Boolean deleted;
}
