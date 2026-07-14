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

/** 安灯异常原因实体。 */
@Data
@Entity
@DynamicInsert
@Table(name = "andon_reason")
public class AndonReasonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reason_code")
    private String reasonCode;

    @Column(name = "reason_name")
    private String reasonName;

    @Column(name = "andon_type_id")
    private Long andonTypeId;

    @Column(name = "reason_description")
    private String reasonDescription;

    @Column(name = "enabled_status")
    private Integer enabledStatus;

    @Column(name = "create_by")
    private Long createBy;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    @Column(name = "is_deleted")
    private Boolean deleted;
}
