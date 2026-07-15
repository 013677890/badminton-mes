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

/**
 * 安灯异常原因主数据实体，维护某一安灯类型下可供事件申报和结案确认选择的原因。
 *
 * <p>原因可同时出现在事件的初步原因和实际原因字段中；已进入事件历史的原因受引用保护，
 * 停用仅影响后续选择，不改变既有事件记录。
 */
@Data
@Entity
@DynamicInsert
@Table(name = "andon_reason")
public class AndonReasonEntity {

    /** 原因主键，供事件初步原因和实际原因字段引用。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 原因业务编码，用于稳定识别和唯一性校验。 */
    @Column(name = "reason_code")
    private String reasonCode;

    /** 面向现场人员展示的原因名称。 */
    @Column(name = "reason_name")
    private String reasonName;

    /** 原因所属安灯类型，限定其可用于哪些类型的事件。 */
    @Column(name = "andon_type_id")
    private Long andonTypeId;

    /** 原因适用场景、判定口径或补充说明。 */
    @Column(name = "reason_description")
    private String reasonDescription;

    /** 原因启停状态；停用原因保留历史引用但不应继续用于新事件。 */
    @Column(name = "enabled_status")
    private Integer enabledStatus;

    /** 创建原因主数据的用户标识。 */
    @Column(name = "create_by")
    private Long createBy;

    /** 数据库生成的原因创建时间。 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 数据库维护的原因最近更新时间。 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记；已删除原因不参与常规查询和编码活动唯一性判断。 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
