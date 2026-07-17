package com.badminton.mes.module.scene.dal.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;

/** 任务操作日志实体。 @author 刘涵 */
@Data @Entity @DynamicInsert @Table(name = "prod_task_operate_log")
public class SceneTaskOperateLogEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "task_id") private Long taskId;
    @Column(name = "operate_type", columnDefinition = "tinyint unsigned") private Integer operateType;
    @Column(name = "from_status", columnDefinition = "tinyint unsigned") private Integer fromStatus;
    @Column(name = "to_status", columnDefinition = "tinyint unsigned") private Integer toStatus;
    private String reason;
    @Column(name = "terminal_type", columnDefinition = "tinyint unsigned") private Integer terminalType;
    @Column(name = "operator_id") private Long operatorId;
    @Column(name = "operate_time") private LocalDateTime operateTime;
    @Column(name = "create_time", insertable = false, updatable = false) private LocalDateTime createTime;
    @Column(name = "update_time", insertable = false) private LocalDateTime updateTime;
    @Column(name = "is_deleted", columnDefinition = "tinyint unsigned") private Boolean deleted;
}
