package com.badminton.mes.module.scene.dal.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;

/** 产品批次工序履历。 @author 刘涵 */
@Data @Entity @DynamicInsert @Table(name = "prod_batch_process_history")
public class SceneBatchProcessHistoryEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "batch_status_id") private Long batchStatusId;
    @Column(name = "task_id") private Long taskId;
    @Column(name = "dispatch_detail_id") private Long dispatchDetailId;
    @Column(name = "batch_no") private String batchNo;
    @Column(name = "process_id") private Long processId;
    @Column(name = "process_code") private String processCode;
    @Column(name = "process_name") private String processName;
    @Column(name = "action_type", columnDefinition = "tinyint unsigned") private Integer actionType;
    @Column(name = "operator_id") private Long operatorId;
    @Column(name = "action_reason") private String actionReason;
    @Column(name = "operate_time") private LocalDateTime operateTime;
    @Column(name = "create_time", insertable = false, updatable = false) private LocalDateTime createTime;
    @Column(name = "update_time", insertable = false) private LocalDateTime updateTime;
    @Column(name = "is_deleted", columnDefinition = "tinyint unsigned") private Boolean deleted;
}
