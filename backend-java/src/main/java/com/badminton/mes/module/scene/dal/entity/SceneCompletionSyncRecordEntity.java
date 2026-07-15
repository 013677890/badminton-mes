package com.badminton.mes.module.scene.dal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;

/** 完工单外部同步审计记录。 @author 刘涵 */
@Data @Entity @DynamicInsert @Table(name="prod_finish_sync_record")
public class SceneCompletionSyncRecordEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="finish_order_id") private Long finishOrderId;
    @Column(name="target_system") private String targetSystem;
    @Column(name="idempotency_key") private String idempotencyKey;
    @Column(name="sync_status") private Integer syncStatus;
    @Column(name="retry_count") private Integer retryCount;
    @Column(name="error_summary") private String errorSummary;
    @Column(name="last_sync_time") private LocalDateTime lastSyncTime;
    @Column(name="create_time",insertable=false,updatable=false) private LocalDateTime createTime;
    @Column(name="update_time",insertable=false) private LocalDateTime updateTime;
    @Column(name="is_deleted") private Boolean deleted;
}
