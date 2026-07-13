package com.badminton.mes.module.andon.dal.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/** 安灯异常处理配置实体。 */
@Data
@Entity
@DynamicInsert
@Table(name = "andon_configuration")
public class AndonConfigurationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "andon_type_id")
    private Long andonTypeId;

    @Column(name = "production_line_id")
    private Long productionLineId;

    @Column(name = "scope_line_id")
    private Long scopeLineId;

    @Column(name = "handler_user_id")
    private Long handlerUserId;

    @Column(name = "handler_role_code")
    private String handlerRoleCode;

    @Column(name = "escalation_user_id")
    private Long escalationUserId;

    @Column(name = "escalation_role_code")
    private String escalationRoleCode;

    @Column(name = "response_minutes")
    private Integer responseMinutes;

    @Column(name = "escalation_minutes")
    private Integer escalationMinutes;

    @Column(name = "notification_channels")
    private String notificationChannels;

    @Column(name = "enabled_status")
    private Integer enabledStatus;

    @Column(name = "remark")
    private String remark;

    @Column(name = "create_by")
    private Long createBy;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    @Column(name = "is_deleted")
    private Boolean deleted;
}
