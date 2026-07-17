package com.badminton.mes.module.andon.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 安灯异常通知记录响应。
 *
 * <p>记录事件在初始指派、状态变化或升级时面向用户/角色发送通知的渠道、发送状态与结果，
 * 用于审计通知是否按配置执行；当前实现可使用模拟发送状态验证通知链路。
 */
@Data
public class AndonNotificationRecordRespVO {
    /** 通知记录数据库主键。 */
    private Long id;
    /** 通知业务类型，如初始指派、状态通知或升级通知。 */
    private String notificationType;
    /** 发送渠道，取自配置中的 {@code IN_APP}、{@code SMS} 或 {@code WECHAT}。 */
    private String channel;
    /** 接收用户主键；按具体用户通知时填写。 */
    private Long receiverUserId;
    /** 接收角色编码；按责任角色覆盖成员时填写。 */
    private String receiverRoleCode;
    /** 发送执行状态；{@code SIMULATED} 表示已记录模拟发送而非真实外部投递。 */
    private String sendStatus;
    /** 本次通知正文或发送结果说明，便于排查通知内容与执行结果。 */
    private String sendMessage;
    /** 实际执行发送或模拟发送的时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime sentAt;
    /** 通知记录持久化创建时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime createTime;
}
