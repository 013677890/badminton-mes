package com.badminton.mes.module.scene.dal.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;

/** 产品批次当前生产状态。 @author 刘涵 */
@Data @Entity @DynamicInsert @Table(name = "prod_batch_status")
public class SceneBatchStatusEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "batch_no") private String batchNo;
    @Column(name = "task_id") private Long taskId;
    @Column(name = "product_id") private Long productId;
    @Column(name = "current_process_id") private Long currentProcessId;
    @Column(name = "current_process_name") private String currentProcessName;
    @Column(name = "batch_status", columnDefinition = "tinyint unsigned") private Integer batchStatus;
    @Column(name = "is_abnormal", columnDefinition = "tinyint unsigned") private Boolean abnormal;
    @Column(name = "create_time", insertable = false, updatable = false) private LocalDateTime createTime;
    @Column(name = "update_time", insertable = false) private LocalDateTime updateTime;
    @Column(name = "is_deleted", columnDefinition = "tinyint unsigned") private Boolean deleted;
}
