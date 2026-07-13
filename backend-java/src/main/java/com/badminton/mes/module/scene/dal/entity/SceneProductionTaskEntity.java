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
 * 现场生产任务实体。
 *
 * @author Codex
 * @date 2026/07/13
 */
@Data
@Entity
@DynamicInsert
@Table(name = "scene_production_task")
public class SceneProductionTaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_no")
    private String taskNo;

    @Column(name = "dispatch_order_id")
    private Long dispatchOrderId;

    @Column(name = "work_order_id")
    private Long workOrderId;

    @Column(name = "routing_id")
    private Long routingId;

    @Column(name = "line_id")
    private Long lineId;

    @Column(name = "shift_id")
    private Long shiftId;

    @Column(name = "plan_quantity")
    private Integer planQuantity;

    @Column(name = "task_status")
    private Integer taskStatus;

    @Column(name = "qualified_quantity")
    private BigDecimal qualifiedQuantity;

    @Column(name = "defect_quantity")
    private BigDecimal defectQuantity;

    @Column(name = "create_by")
    private Long createBy;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    @Column(name = "is_deleted")
    private Boolean deleted;
}
