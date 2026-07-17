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
 * 安灯类型主数据实体，定义一类现场异常的分类、处理模式、默认响应规则和灯控能力。
 *
 * <p>类型是原因、处理配置和事件的上层归类；类型规则提供默认值，具体产线配置可进一步确定
 * 实际处理主体、升级时限和通知范围。
 */
@Data
@Entity
@DynamicInsert
@Table(name = "andon_type")
public class AndonTypeEntity {

    /** 类型主键，被原因、处理配置和事件作为分类标识引用。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 类型业务编码，用于跨页面和流程稳定识别该异常类型。 */
    @Column(name = "type_code")
    private String typeCode;

    /** 面向现场人员展示的安灯类型名称。 */
    @Column(name = "type_name")
    private String typeName;

    /** 异常所属业务类别，用于区分设备、质量或其他异常域。 */
    @Column(name = "exception_category")
    private String exceptionCategory;

    /** 类型采用的处置方式，决定是否需要协助处理及完整责任规则。 */
    @Column(name = "handling_mode")
    private String handlingMode;

    /** 类型级默认响应分钟数，在规则校验和事件时限计算中使用。 */
    @Column(name = "response_minutes")
    private Integer responseMinutes;

    /** 类型级默认责任角色编码，用于未被更具体配置覆盖的处理职责。 */
    @Column(name = "responsible_role_code")
    private String responsibleRoleCode;

    /** 类型级默认通知渠道集合，由业务层按约定格式解析。 */
    @Column(name = "notification_channels")
    private String notificationChannels;

    /** 是否启用现场灯控联动；关闭时事件流程仍可独立运行。 */
    @Column(name = "light_control_enabled")
    private Boolean lightControlEnabled;

    /** 类型启停状态；停用后不应继续用于新建事件或活动配置。 */
    @Column(name = "enabled_status")
    private Integer enabledStatus;

    /** 类型用途、处置边界或维护说明。 */
    @Column(name = "remark")
    private String remark;

    /** 创建类型主数据的用户标识。 */
    @Column(name = "create_by")
    private Long createBy;

    /** 数据库生成的类型创建时间。 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 数据库维护的类型最近更新时间。 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记；删除前需确认不存在原因、配置和事件引用。 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
