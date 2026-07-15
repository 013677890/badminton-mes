package com.badminton.mes.module.andon.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 安灯异常处理配置详情响应。
 *
 * <p>配置以安灯类型和产线为作用域，定义协助事件的初始指派、自动升级目标、两级时限及通知渠道；
 * 事件创建时优先使用产线配置，未匹配时再使用全局配置或类型默认规则。
 */
@Data
public class AndonConfigurationRespVO {

    /** 配置数据库主键。 */
    private Long id;
    /** 适用的安灯类型主键。 */
    private Long andonTypeId;
    /** 安灯类型业务编码，便于客户端识别配置所属类型。 */
    private String andonTypeCode;
    /** 安灯类型显示名称。 */
    private String andonTypeName;
    /** 作用产线主键；为空表示该类型的全局兜底配置。 */
    private Long productionLineId;
    /** 初始指派的具体处理用户主键，可与处理角色同时配置。 */
    private Long handlerUserId;
    /** 初始指派的处理角色编码，允许角色成员承接事件。 */
    private String handlerRoleCode;
    /** 超过升级时限后改派的具体用户主键。 */
    private Long escalationUserId;
    /** 超过升级时限后改派的责任角色编码。 */
    private String escalationRoleCode;
    /** 从事件发起起计算的响应时限，单位为分钟，用于生成响应截止时间。 */
    private Integer responseMinutes;
    /** 从事件发起起计算的升级时限，单位为分钟，配置时必须晚于响应时限。 */
    private Integer escalationMinutes;
    /** 逗号分隔的通知渠道，可包含应用内、短信和微信通知。 */
    private String notificationChannels;
    /** 启用状态：{@code 1} 参与事件规则匹配，{@code 0} 不再用于新事件。 */
    private Integer enabledStatus;
    /** 配置用途、责任范围或特殊约定的补充说明。 */
    private String remark;

    /** 配置记录创建时间，按 {@code yyyy-MM-dd HH:mm:ss} 输出。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 配置责任主体、时限、渠道或启用状态最后修改时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
