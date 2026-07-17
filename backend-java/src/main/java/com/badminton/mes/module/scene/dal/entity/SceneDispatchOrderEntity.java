package com.badminton.mes.module.scene.dal.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;

/** 工序派工主表实体。 @author 刘涵 */
@Data @Entity @DynamicInsert @Table(name = "prod_process_dispatch")
public class SceneDispatchOrderEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "dispatch_no") private String dispatchNo;
    @Column(name = "task_id") private Long taskId;
    @Column(name = "routing_id") private Long routingId;
    @Column(name = "routing_code") private String routingCode;
    @Column(name = "routing_version") private String routingVersion;
    @Column(name = "dispatch_status", columnDefinition = "tinyint unsigned") private Integer dispatchStatus;
    @Column(name = "create_by") private Long createBy;
    @Column(name = "create_time", insertable = false, updatable = false) private LocalDateTime createTime;
    @Column(name = "update_time", insertable = false) private LocalDateTime updateTime;
    @Column(name = "is_deleted", columnDefinition = "tinyint unsigned") private Boolean deleted;
}
