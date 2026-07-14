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

/** 安灯异常通知记录实体。 */
@Data
@Entity
@DynamicInsert
@Table(name = "andon_notification_record")
public class AndonNotificationRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "event_id") private Long eventId;
    @Column(name = "notification_type") private String notificationType;
    @Column(name = "channel") private String channel;
    @Column(name = "receiver_user_id") private Long receiverUserId;
    @Column(name = "receiver_role_code") private String receiverRoleCode;
    @Column(name = "send_status") private String sendStatus;
    @Column(name = "send_message") private String sendMessage;
    @Column(name = "sent_at") private LocalDateTime sentAt;
    @Column(name = "create_time", insertable = false, updatable = false) private LocalDateTime createTime;
}
