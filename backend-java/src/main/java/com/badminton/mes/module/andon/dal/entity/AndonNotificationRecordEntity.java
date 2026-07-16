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
 * 安灯异常通知记录实体，保存事件在不同流程节点向责任主体发送通知的结果。
 *
 * <p>每条记录描述一次通知类型、渠道、接收主体和发送状态，可用于事件详情展示及通知链路审计，
 * 不作为事件是否已处理的流程状态依据。
 */
@Data
@Entity
@DynamicInsert
@Table(name = "andon_notification_record")
public class AndonNotificationRecordEntity {

    /** 通知记录主键，也是同一事件通知历史的稳定排序依据。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** 所属安灯事件主键，用于聚合该事件的全部通知记录。 */
    @Column(name = "event_id") private Long eventId;
    /** 通知业务类型，用于区分发起、转派、升级或超时等触发场景。 */
    @Column(name = "notification_type") private String notificationType;
    /** 实际采用的通知渠道，如站内消息或其他外部通道。 */
    @Column(name = "channel") private String channel;
    /** 通知直接接收用户标识；按角色通知时可为空。 */
    @Column(name = "receiver_user_id") private Long receiverUserId;
    /** 通知接收角色编码，表示面向该角色下可处理成员发送。 */
    @Column(name = "receiver_role_code") private String receiverRoleCode;
    /** 本次发送执行结果，用于区分成功、失败或其他投递状态。 */
    @Column(name = "send_status") private String sendStatus;
    /** 通知正文、发送结果说明或失败原因的持久化内容。 */
    @Column(name = "send_message") private String sendMessage;
    /** 实际完成发送尝试的时间，区别于记录创建时间。 */
    @Column(name = "sent_at") private LocalDateTime sentAt;
    /** 数据库生成的通知记录创建时间。 */
    @Column(name = "create_time", insertable = false, updatable = false) private LocalDateTime createTime;
}
