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
 * 现场工序任务实体，保存派工下发时的工艺快照。
 *
 * @author Codex
 * @date 2026/07/13
 */
@Data
@Entity
@DynamicInsert
@Table(name = "scene_process_task")
public class SceneProcessTaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "production_task_id")
    private Long productionTaskId;

    @Column(name = "route_detail_id")
    private Long routeDetailId;

    @Column(name = "process_id")
    private Long processId;

    @Column(name = "sequence_no")
    private Integer sequenceNo;

    @Column(name = "station_id")
    private Long stationId;

    @Column(name = "equipment_category_id")
    private Long equipmentCategoryId;

    @Column(name = "sop_id")
    private Long sopId;

    @Column(name = "is_inspect")
    private Boolean inspect;

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
