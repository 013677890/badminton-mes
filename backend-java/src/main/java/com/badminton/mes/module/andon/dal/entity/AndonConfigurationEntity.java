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
 * 安灯异常处理配置实体，定义某类安灯事件在指定产线范围内的责任主体、响应时限和通知方式。
 *
 * <p>配置既保留业务产线标识，也保存用于唯一性与兜底匹配的范围产线标识；事件发起时据此选择
 * 产线级规则或全局规则，并生成当前指派和升级时限。
 */
@Data
@Entity
@DynamicInsert
@Table(name = "andon_configuration")
public class AndonConfigurationEntity {

    /** 配置主键，供维护、加锁更新和详情缓存使用。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 配置适用的安灯类型，同一范围内用于匹配对应类型的事件。 */
    @Column(name = "andon_type_id")
    private Long andonTypeId;

    /** 配置直接关联的业务产线；事件发起时优先按该产线匹配启用规则。 */
    @Column(name = "production_line_id")
    private Long productionLineId;

    /** 规范化后的配置范围产线，用于表达产线级或全局范围并参与范围唯一性判断。 */
    @Column(name = "scope_line_id")
    private Long scopeLineId;

    /** 默认处理用户标识，与处理角色共同构成事件初始责任主体。 */
    @Column(name = "handler_user_id")
    private Long handlerUserId;

    /** 默认处理角色编码，供该角色成员接收通知并获得处理权限。 */
    @Column(name = "handler_role_code")
    private String handlerRoleCode;

    /** 超过响应阶段后接收升级处理的用户标识。 */
    @Column(name = "escalation_user_id")
    private Long escalationUserId;

    /** 超过响应阶段后接收升级处理的角色编码。 */
    @Column(name = "escalation_role_code")
    private String escalationRoleCode;

    /** 从事件发起到要求首次响应的分钟数，用于计算响应截止时间。 */
    @Column(name = "response_minutes")
    private Integer responseMinutes;

    /** 从规则约定起点到触发升级的分钟数，用于计算升级截止时间。 */
    @Column(name = "escalation_minutes")
    private Integer escalationMinutes;

    /** 该范围事件需要采用的通知渠道集合，由业务层按约定格式解析。 */
    @Column(name = "notification_channels")
    private String notificationChannels;

    /** 配置启停状态；只有启用且未删除的规则参与事件配置匹配。 */
    @Column(name = "enabled_status")
    private Integer enabledStatus;

    /** 配置用途、适用边界或维护说明。 */
    @Column(name = "remark")
    private String remark;

    /** 创建配置的用户标识，用于主数据维护审计。 */
    @Column(name = "create_by")
    private Long createBy;

    /** 数据库生成的配置创建时间。 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 数据库维护的配置最近更新时间。 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记；已删除配置不参与范围唯一性和事件规则匹配。 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
