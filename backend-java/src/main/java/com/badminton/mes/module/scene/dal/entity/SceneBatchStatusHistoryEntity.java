package com.badminton.mes.module.scene.dal.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;

/** 产品批次状态履历。 @author 刘涵 */
@Data @Entity @DynamicInsert @Table(name = "prod_batch_status_history")
public class SceneBatchStatusHistoryEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "batch_status_id") private Long batchStatusId;
    @Column(name = "task_id") private Long taskId;
    @Column(name = "batch_no") private String batchNo;
    @Column(name = "from_status", columnDefinition = "tinyint unsigned") private Integer fromStatus;
    @Column(name = "to_status", columnDefinition = "tinyint unsigned") private Integer toStatus;
    @Column(name = "process_id") private Long processId;
    @Column(name = "change_reason") private String changeReason;
    @Column(name = "operator_id") private Long operatorId;
    @Column(name = "operate_time") private LocalDateTime operateTime;
    @Column(name = "create_time", insertable = false, updatable = false) private LocalDateTime createTime;
    @Column(name = "update_time", insertable = false) private LocalDateTime updateTime;
    @Column(name = "is_deleted", columnDefinition = "tinyint unsigned") private Boolean deleted;
}
