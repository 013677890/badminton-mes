package com.badminton.mes.module.scene.dal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

/** 返修作业记录。 @author 刘涵 */
@Data @Entity @Table(name = "scene_repair_record")
public class SceneRepairRecordEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "repair_work_order_id", nullable = false) private Long repairWorkOrderId;
    @Column(name = "quantity", nullable = false) private Integer quantity;
    @Column(name = "description", nullable = false) private String description;
    @Column(name = "operator_id", nullable = false) private Long operatorId;
    @Column(name = "created_time", nullable = false) private LocalDateTime createdTime;
}
