package com.badminton.mes.module.andon.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 安灯异常处理过程日志响应。
 *
 * <p>每条日志固化一次事件动作前后的状态、操作者、目标指派和说明，用于还原状态迁移、
 * 转派及升级责任链；不改变状态的转派/升级动作中，前后状态可以相同。
 */
@Data
public class AndonProcessLogRespVO {
    /** 过程日志数据库主键，也是同一事件内的顺序依据。 */
    private Long id;
    /** 动作类型，如发起、确认、开始处理、转派、完成、升级或关闭。 */
    private String actionType;
    /** 动作执行前的事件状态；发起事件时可为空。 */
    private String fromStatus;
    /** 动作执行后的事件状态；仅改派责任主体时可与原状态相同。 */
    private String toStatus;
    /** 执行动作的用户主键；自动超时动作使用系统操作者标识。 */
    private Long operatorId;
    /** 动作完成后指向的具体处理用户主键。 */
    private Long targetUserId;
    /** 动作完成后指向的处理角色编码。 */
    private String targetRoleCode;
    /** 操作说明、转派理由、状态说明或系统超时处理结果。 */
    private String actionContent;
    /** 动作日志创建时间，按发生顺序用于还原事件处理时间线。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
