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
 * 安灯异常处理过程日志实体，按动作顺序记录事件状态迁移和责任主体变化。
 *
 * <p>日志是事件当前快照之外的审计轨迹，不承担当前状态判断；同一事件按主键升序读取即可还原处理过程。
 */
@Data
@Entity
@DynamicInsert
@Table(name = "andon_process_log")
public class AndonProcessLogEntity {

    /** 日志主键，同时作为同一事件内处理动作的稳定排序依据。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** 所属安灯事件主键，用于聚合事件的完整处理轨迹。 */
    @Column(name = "event_id") private Long eventId;
    /** 本次操作类型，用于区分确认、转派、升级、完成和关闭等动作。 */
    @Column(name = "action_type") private String actionType;
    /** 动作执行前的事件状态，用于审计状态迁移起点。 */
    @Column(name = "from_status") private String fromStatus;
    /** 动作执行后的事件状态；不发生状态变化的动作可与原状态一致。 */
    @Column(name = "to_status") private String toStatus;
    /** 实际执行本次处理动作的用户标识。 */
    @Column(name = "operator_id") private Long operatorId;
    /** 转派或升级动作指定的目标用户标识。 */
    @Column(name = "target_user_id") private Long targetUserId;
    /** 转派或升级动作指定的目标角色编码。 */
    @Column(name = "target_role_code") private String targetRoleCode;
    /** 操作说明、处理意见或状态变化原因，补充结构化动作字段。 */
    @Column(name = "action_content") private String actionContent;
    /** 数据库生成的日志创建时间，表示动作审计记录的落库时刻。 */
    @Column(name = "create_time", insertable = false, updatable = false) private LocalDateTime createTime;
}
