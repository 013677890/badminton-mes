package com.badminton.mes.module.scene.dal.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;

/** 工序派工明细和现场工艺快照。 @author 刘涵 */
@Data @Entity @DynamicInsert @Table(name = "prod_process_dispatch_detail")
public class SceneDispatchDetailEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "dispatch_id") private Long dispatchId;
    @Column(name = "task_id") private Long taskId;
    @Column(name = "process_id") private Long processId;
    @Column(name = "process_code") private String processCode;
    @Column(name = "process_name") private String processName;
    private Integer seq;
    @Column(name = "is_key", columnDefinition = "tinyint unsigned") private Boolean keyProcess;
    @Column(name = "is_inspect", columnDefinition = "tinyint unsigned") private Boolean inspect;
    @Column(name = "is_scan", columnDefinition = "tinyint unsigned") private Boolean scanRequired;
    @Column(name = "sop_id") private Long sopId;
    @Column(name = "sop_code") private String sopCode;
    @Column(name = "sop_name") private String sopName;
    @Column(name = "sop_version") private String sopVersion;
    @Column(name = "station_id") private Long stationId;
    @Column(name = "user_id") private Long userId;
    @Column(name = "equipment_id") private Long equipmentId;
    @Column(name = "plan_quantity") private Integer planQuantity;
    @Column(name = "good_quantity") private Integer goodQuantity;
    @Column(name = "defect_quantity") private Integer defectQuantity;
    @Column(name = "detail_status", columnDefinition = "tinyint unsigned") private Integer detailStatus;
    @Column(name = "is_paused", columnDefinition = "tinyint unsigned") private Boolean paused;
    @Column(name = "pause_reason") private String pauseReason;
    @Column(name = "actual_start_time") private LocalDateTime actualStartTime;
    @Column(name = "actual_end_time") private LocalDateTime actualEndTime;
    @Column(name = "create_time", insertable = false, updatable = false) private LocalDateTime createTime;
    @Column(name = "update_time", insertable = false) private LocalDateTime updateTime;
    @Column(name = "is_deleted", columnDefinition = "tinyint unsigned") private Boolean deleted;
}
