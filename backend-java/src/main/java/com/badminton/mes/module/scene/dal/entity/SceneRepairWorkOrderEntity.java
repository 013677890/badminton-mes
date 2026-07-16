package com.badminton.mes.module.scene.dal.entity;

import com.badminton.mes.module.scene.enums.SceneRepairStatusEnum;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

/** 返修工单。 @author 刘涵 */
@Data @Entity @Table(name = "scene_repair_work_order")
public class SceneRepairWorkOrderEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "repair_no", nullable = false, unique = true) private String repairNo;
    @Column(name = "source_report_id", nullable = false, unique = true) private Long sourceReportId;
    @Column(name = "task_id") private Long taskId;
    @Column(name = "batch_no", nullable = false) private String batchNo;
    @Column(name = "defect_quantity", nullable = false) private Integer defectQuantity;
    @Column(name = "repair_quantity", nullable = false) private Integer repairQuantity;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false) private SceneRepairStatusEnum status;
    @Column(name = "reason") private String reason;
    @Column(name = "assignee_id") private Long assigneeId;
    @Column(name = "recheck_result") private String recheckResult;
    @Column(name = "recheck_quantity") private Integer recheckQuantity;
    @Column(name = "created_by", nullable = false) private Long createdBy;
    @Column(name = "created_time", nullable = false) private LocalDateTime createdTime;
    @Column(name = "updated_time", nullable = false) private LocalDateTime updatedTime;
    @Column(name = "is_deleted", nullable = false) private Boolean deleted;
}
