package com.badminton.mes.module.andon.controller.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 安灯异常处理配置创建/修改请求。
 *
 * <p>同一安灯类型在同一产线作用域仅允许一条配置；未指定产线表示全局兜底规则。
 * 初始处理主体至少配置用户或角色之一；配置升级时限时必须同时配置升级用户/角色之一，且升级时限晚于响应时限。
 */
@Data
public class AndonConfigurationSaveReqVO {

    /** 规则适用的安灯类型主键；类型必须存在，且更新时不可变更原配置所属类型。 */
    @NotNull(message = "安灯类型不能为空")
    @Positive(message = "安灯类型必须为正整数")
    private Long andonTypeId;

    /** 规则作用的产线主键；为空表示对该类型提供全局兜底配置。 */
    @Positive(message = "产线必须为正整数")
    private Long productionLineId;

    /** 事件创建后优先指派的具体用户主键；如填写，用户必须处于启用状态。 */
    @Positive(message = "处理人必须为正整数")
    private Long handlerUserId;

    /** 事件创建后指派的处理角色编码；与处理用户至少填写一项，角色必须有效。 */
    @Size(max = 32, message = "处理角色编码长度不能超过 32")
    private String handlerRoleCode;

    /** 自动或人工升级时采用的具体用户主键；须与升级时限联动配置。 */
    @Positive(message = "升级处理人必须为正整数")
    private Long escalationUserId;

    /** 自动或人工升级时采用的责任角色编码；可与升级用户共同配置。 */
    @Size(max = 32, message = "升级角色编码长度不能超过 32")
    private String escalationRoleCode;

    /** 响应时限，单位为分钟；从事件发起时间计算待确认事件的响应截止时间。 */
    @NotNull(message = "响应时限不能为空")
    @Min(value = 1, message = "响应时限最少为 1 分钟")
    @Max(value = 10080, message = "响应时限不能超过 10080 分钟")
    private Integer responseMinutes;

    /** 升级时限，单位为分钟；从事件发起时间计算，必须大于响应时限并配套升级主体。 */
    @Min(value = 1, message = "升级时限最少为 1 分钟")
    @Max(value = 10080, message = "升级时限不能超过 10080 分钟")
    private Integer escalationMinutes;

    /** 逗号分隔的通知渠道，按需组合 {@code IN_APP}、{@code SMS} 和 {@code WECHAT}。 */
    @NotBlank(message = "通知渠道不能为空")
    @Size(max = 128, message = "通知渠道长度不能超过 128")
    @Pattern(regexp = "^(IN_APP|SMS|WECHAT)(,(IN_APP|SMS|WECHAT))*$", message = "通知渠道格式不合法")
    private String notificationChannels;

    /** 启用状态：{@code 1} 参与事件配置匹配，{@code 0} 暂停用于新事件。 */
    @Min(value = 0, message = "启用状态只能为 0 或 1")
    @Max(value = 1, message = "启用状态只能为 0 或 1")
    private Integer enabledStatus;

    /** 规则用途、适用班组或其他维护说明。 */
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
