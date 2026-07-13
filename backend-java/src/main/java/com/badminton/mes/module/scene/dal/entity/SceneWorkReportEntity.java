package com.badminton.mes.module.scene.dal.entity;

import java.math.BigDecimal;
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
 * 现场生产报工实体。
 *
 * @author Codex
 * @date 2026/07/13
 */
@Data
@Entity
@DynamicInsert
@Table(name = "scene_work_report")
public class SceneWorkReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_no")
    private String reportNo;

    @Column(name = "source_type")
    private Integer sourceType;

    @Column(name = "source_record_id")
    private Long sourceRecordId;

    @Column(name = "production_task_id")
    private Long productionTaskId;

    @Column(name = "dispatch_order_id")
    private Long dispatchOrderId;

    @Column(name = "work_order_id")
    private Long workOrderId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "process_id")
    private Long processId;

    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "qualified_quantity")
    private BigDecimal qualifiedQuantity;

    @Column(name = "defect_quantity")
    private BigDecimal defectQuantity;

    @Column(name = "report_time")
    private LocalDateTime reportTime;

    @Column(name = "audit_status")
    private Integer auditStatus;

    @Column(name = "audit_by")
    private Long auditBy;

    @Column(name = "audit_time")
    private LocalDateTime auditTime;

    @Column(name = "create_by")
    private Long createBy;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    @Column(name = "is_deleted")
    private Boolean deleted;
}
