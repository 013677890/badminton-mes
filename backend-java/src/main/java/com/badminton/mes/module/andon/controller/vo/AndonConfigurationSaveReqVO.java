package com.badminton.mes.module.andon.controller.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 安灯异常处理配置创建/修改请求。 */
@Data
public class AndonConfigurationSaveReqVO {

    @NotNull(message = "安灯类型不能为空")
    @Positive(message = "安灯类型必须为正整数")
    private Long andonTypeId;

    @Positive(message = "产线必须为正整数")
    private Long productionLineId;

    @Positive(message = "处理人必须为正整数")
    private Long handlerUserId;

    @Size(max = 32, message = "处理角色编码长度不能超过 32")
    private String handlerRoleCode;

    @Positive(message = "升级处理人必须为正整数")
    private Long escalationUserId;

    @Size(max = 32, message = "升级角色编码长度不能超过 32")
    private String escalationRoleCode;

    @NotNull(message = "响应时限不能为空")
    @Min(value = 1, message = "响应时限最少为 1 分钟")
    @Max(value = 10080, message = "响应时限不能超过 10080 分钟")
    private Integer responseMinutes;

    @Min(value = 1, message = "升级时限最少为 1 分钟")
    @Max(value = 10080, message = "升级时限不能超过 10080 分钟")
    private Integer escalationMinutes;

    @NotBlank(message = "通知渠道不能为空")
    @Size(max = 128, message = "通知渠道长度不能超过 128")
    @Pattern(regexp = "^(IN_APP|SMS|WECHAT)(,(IN_APP|SMS|WECHAT))*$", message = "通知渠道格式不合法")
    private String notificationChannels;

    @Min(value = 0, message = "启用状态只能为 0 或 1")
    @Max(value = 1, message = "启用状态只能为 0 或 1")
    private Integer enabledStatus;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
