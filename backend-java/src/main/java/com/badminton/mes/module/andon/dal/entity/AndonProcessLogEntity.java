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

/** 安灯异常处理过程实体。 */
@Data
@Entity
@DynamicInsert
@Table(name = "andon_process_log")
public class AndonProcessLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "event_id") private Long eventId;
    @Column(name = "action_type") private String actionType;
    @Column(name = "from_status") private String fromStatus;
    @Column(name = "to_status") private String toStatus;
    @Column(name = "operator_id") private Long operatorId;
    @Column(name = "target_user_id") private Long targetUserId;
    @Column(name = "target_role_code") private String targetRoleCode;
    @Column(name = "action_content") private String actionContent;
    @Column(name = "create_time", insertable = false, updatable = false) private LocalDateTime createTime;
}
