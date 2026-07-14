package com.badminton.mes.module.wage.dal.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/** 计件规则变更日志实体。 */
@Data
@Entity
@DynamicInsert
@Table(name = "wage_rule_change_log")
public class WageRuleChangeLogEntity {
    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** 规则主键 */
    @Column(name = "rule_id")
    private Long ruleId;
    /** 变更类型 */
    @Column(name = "change_type")
    private String changeType;
    /** 变更前快照 */
    @Column(name = "before_snapshot", columnDefinition = "json")
    private String beforeSnapshot;
    /** 变更后快照 */
    @Column(name = "after_snapshot", columnDefinition = "json")
    private String afterSnapshot;
    /** 变更原因 */
    @Column(name = "change_reason")
    private String changeReason;
    /** 操作人 */
    @Column(name = "operate_by")
    private Long operateBy;
    /** 操作时间 */
    @Column(name = "operate_time", insertable = false, updatable = false)
    private LocalDateTime operateTime;
    /** 创建时间 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;
    /** 更新时间 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;
    /** 逻辑删除 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
