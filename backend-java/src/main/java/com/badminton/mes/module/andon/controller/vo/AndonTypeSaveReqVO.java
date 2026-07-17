package com.badminton.mes.module.andon.controller.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 安灯类型创建/修改请求。
 *
 * <p>异常类别用于业务归类，处理方式决定事件采用自动闭环、自行处理或协助指派流程。
 * 协助模式必须配套默认响应时限、责任角色和通知渠道，产线/全局处理配置可在事件创建时覆盖这些默认值。
 */
@Data
public class AndonTypeSaveReqVO {

    /** 类型业务编码；必须唯一，且不得使用软删除流程保留的 {@code __DELETED_} 前缀。 */
    @NotBlank(message = "安灯类型编码不能为空")
    @Size(max = 32, message = "安灯类型编码长度不能超过 32")
    @Pattern(regexp = "^(?!__DELETED_).+$", message = "安灯类型编码不能使用系统保留前缀")
    private String typeCode;

    /** 类型显示名称，应准确概括现场异常及其处理场景。 */
    @NotBlank(message = "安灯类型名称不能为空")
    @Size(max = 128, message = "安灯类型名称长度不能超过 128")
    private String typeName;

    /** 异常类别：生产、设备、质量、物料或非生产异常，用于统计与筛选。 */
    @NotBlank(message = "异常类别不能为空")
    @Pattern(regexp = "^(PRODUCTION|EQUIPMENT|QUALITY|MATERIAL|NON_PRODUCTION)$", message = "异常类别不合法")
    private String exceptionCategory;

    /** 处理方式：无需处理时自动闭环，自行处理时指派发起人，协助处理时按配置或默认规则指派。 */
    @NotBlank(message = "处理方式不能为空")
    @Pattern(regexp = "^(NO_ACTION|SELF_HANDLE|ASSISTANCE)$", message = "处理方式不合法")
    private String handlingMode;

    /** 默认响应时限，单位为分钟；协助模式必填，供未匹配专用处理配置时使用。 */
    @Min(value = 1, message = "响应时限最少为 1 分钟")
    @Max(value = 10080, message = "响应时限不能超过 10080 分钟")
    private Integer responseMinutes;

    /** 默认责任角色编码；协助模式必填，作为无专用配置时的初始角色指派。 */
    @Size(max = 32, message = "责任角色编码长度不能超过 32")
    private String responsibleRoleCode;

    /** 默认通知渠道，按需用逗号组合 {@code IN_APP}、{@code SMS} 和 {@code WECHAT}。 */
    @Size(max = 128, message = "通知渠道长度不能超过 128")
    @Pattern(regexp = "^(IN_APP|SMS|WECHAT)(,(IN_APP|SMS|WECHAT))*$", message = "通知渠道格式不合法")
    private String notificationChannels;

    /** 是否启用现场灯控；开启后事件发起尝试开灯，关闭事件时同步关灯。 */
    private Boolean lightControlEnabled;

    /** 启用状态：{@code 1} 可用于新事件，{@code 0} 停止新增使用。 */
    @Min(value = 0, message = "启用状态只能为 0 或 1")
    @Max(value = 1, message = "启用状态只能为 0 或 1")
    private Integer enabledStatus;

    /** 类型使用范围、管理要求或其他维护说明。 */
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
