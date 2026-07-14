package com.badminton.mes.module.scene.dal.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/** 现场生产任务实体及上游快照，对应 B 组现场任务表。 */
@Data
@Entity
@DynamicInsert
@Table(name = "prod_task")
public class SceneProductionTaskEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "task_no") private String taskNo;
    @Column(name = "source_type", columnDefinition = "tinyint unsigned") private Integer sourceType;
    @Column(name = "work_order_id") private Long workOrderId;
    @Column(name = "work_order_no") private String workOrderNo;
    @Column(name = "product_id") private Long productId;
    @Column(name = "product_code") private String productCode;
    @Column(name = "product_name") private String productName;
    @Column(name = "batch_no") private String batchNo;
    @Column(name = "routing_id") private Long routingId;
    @Column(name = "routing_code") private String routingCode;
    @Column(name = "routing_version") private String routingVersion;
    @Column(name = "workshop_id") private Long workshopId;
    @Column(name = "workshop_name") private String workshopName;
    @Column(name = "line_id") private Long lineId;
    @Column(name = "line_name") private String lineName;
    @Column(name = "shift_id") private Long shiftId;
    @Column(name = "plan_date") private LocalDate planDate;
    @Column(name = "plan_quantity") private Integer planQuantity;
    @Column(name = "input_quantity") private Integer inputQuantity;
    @Column(name = "good_quantity") private Integer goodQuantity;
    @Column(name = "defect_quantity") private Integer defectQuantity;
    @Column(name = "rework_quantity") private Integer reworkQuantity;
    @Column(name = "finish_quantity") private Integer finishQuantity;
    @Column(name = "plan_start_time") private LocalDateTime planStartTime;
    @Column(name = "plan_end_time") private LocalDateTime planEndTime;
    @Column(name = "actual_start_time") private LocalDateTime actualStartTime;
    @Column(name = "actual_end_time") private LocalDateTime actualEndTime;
    @Column(name = "task_status", columnDefinition = "tinyint unsigned") private Integer taskStatus;
    @Column(name = "pause_reason") private String pauseReason;
    @Column(name = "audit_by") private Long auditBy;
    @Column(name = "audit_time") private LocalDateTime auditTime;
    @Column(name = "create_by") private Long createBy;
    @Column(name = "create_time", insertable = false, updatable = false) private LocalDateTime createTime;
    @Column(name = "update_time", insertable = false) private LocalDateTime updateTime;
    @Column(name = "is_deleted", columnDefinition = "tinyint unsigned") private Boolean deleted;
}
