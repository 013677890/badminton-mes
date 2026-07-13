package com.badminton.mes.module.scene.dal.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;

/** 生产参数变更日志实体。 @author 刘涵 */
@Data @Entity @DynamicInsert @Table(name = "prod_param_change_log")
public class SceneParameterChangeLogEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "param_id") private Long paramId;
    @Column(name = "before_value") private String beforeValue;
    @Column(name = "after_value") private String afterValue;
    @Column(name = "before_status", columnDefinition = "tinyint unsigned") private Integer beforeStatus;
    @Column(name = "after_status", columnDefinition = "tinyint unsigned") private Integer afterStatus;
    @Column(name = "change_reason") private String changeReason;
    @Column(name = "operator_id") private Long operatorId;
    @Column(name = "operate_time") private LocalDateTime operateTime;
    @Column(name = "create_time", insertable = false, updatable = false) private LocalDateTime createTime;
    @Column(name = "update_time", insertable = false) private LocalDateTime updateTime;
    @Column(name = "is_deleted", columnDefinition = "tinyint unsigned") private Boolean deleted;
}
