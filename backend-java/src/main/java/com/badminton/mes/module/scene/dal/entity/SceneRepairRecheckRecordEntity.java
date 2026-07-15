package com.badminton.mes.module.scene.dal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

/** 返修复检事实记录。 @author 刘涵 */
@Data @Entity @Table(name = "scene_repair_recheck_record")
public class SceneRepairRecheckRecordEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "repair_work_order_id", nullable = false) private Long repairWorkOrderId;
    @Column(name = "result", nullable = false) private String result;
    @Column(name = "quantity", nullable = false) private Integer quantity;
    @Column(name = "inspector_id", nullable = false) private Long inspectorId;
    @Column(name = "created_time", nullable = false) private LocalDateTime createdTime;
}
