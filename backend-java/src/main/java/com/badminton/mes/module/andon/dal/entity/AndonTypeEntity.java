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

/** 安灯类型实体。 */
@Data
@Entity
@DynamicInsert
@Table(name = "andon_type")
public class AndonTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_code")
    private String typeCode;

    @Column(name = "type_name")
    private String typeName;

    @Column(name = "exception_category")
    private String exceptionCategory;

    @Column(name = "handling_mode")
    private String handlingMode;

    @Column(name = "response_minutes")
    private Integer responseMinutes;

    @Column(name = "responsible_role_code")
    private String responsibleRoleCode;

    @Column(name = "notification_channels")
    private String notificationChannels;

    @Column(name = "light_control_enabled")
    private Boolean lightControlEnabled;

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
