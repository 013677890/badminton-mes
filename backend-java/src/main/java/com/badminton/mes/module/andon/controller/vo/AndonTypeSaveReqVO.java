package com.badminton.mes.module.andon.controller.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 安灯类型创建/修改请求。 */
@Data
public class AndonTypeSaveReqVO {

    @NotBlank(message = "安灯类型编码不能为空")
    @Size(max = 32, message = "安灯类型编码长度不能超过 32")
    @Pattern(regexp = "^(?!__DELETED_).+$", message = "安灯类型编码不能使用系统保留前缀")
    private String typeCode;

    @NotBlank(message = "安灯类型名称不能为空")
    @Size(max = 128, message = "安灯类型名称长度不能超过 128")
    private String typeName;

    @NotBlank(message = "异常类别不能为空")
    @Pattern(regexp = "^(PRODUCTION|EQUIPMENT|QUALITY|MATERIAL|NON_PRODUCTION)$", message = "异常类别不合法")
    private String exceptionCategory;

    @NotBlank(message = "处理方式不能为空")
    @Pattern(regexp = "^(NO_ACTION|SELF_HANDLE|ASSISTANCE)$", message = "处理方式不合法")
    private String handlingMode;

    @Min(value = 1, message = "响应时限最少为 1 分钟")
    @Max(value = 10080, message = "响应时限不能超过 10080 分钟")
    private Integer responseMinutes;

    @Size(max = 32, message = "责任角色编码长度不能超过 32")
    private String responsibleRoleCode;

    @Size(max = 128, message = "通知渠道长度不能超过 128")
    @Pattern(regexp = "^(IN_APP|SMS|WECHAT)(,(IN_APP|SMS|WECHAT))*$", message = "通知渠道格式不合法")
    private String notificationChannels;

    private Boolean lightControlEnabled;

    @Min(value = 0, message = "启用状态只能为 0 或 1")
    @Max(value = 1, message = "启用状态只能为 0 或 1")
    private Integer enabledStatus;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
