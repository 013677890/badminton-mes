package com.badminton.mes.module.andon.controller.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 安灯异常原因创建/修改请求。 */
@Data
public class AndonReasonSaveReqVO {

    @NotBlank(message = "异常原因编码不能为空")
    @Size(max = 32, message = "异常原因编码长度不能超过 32")
    @Pattern(regexp = "^(?!__DELETED_).+$", message = "异常原因编码不能使用系统保留前缀")
    private String reasonCode;

    @NotBlank(message = "异常原因名称不能为空")
    @Size(max = 128, message = "异常原因名称长度不能超过 128")
    private String reasonName;

    @NotNull(message = "安灯类型不能为空")
    @Positive(message = "安灯类型必须为正整数")
    private Long andonTypeId;

    @Size(max = 500, message = "原因描述长度不能超过 500")
    private String reasonDescription;

    @Min(value = 0, message = "启用状态只能为 0 或 1")
    @Max(value = 1, message = "启用状态只能为 0 或 1")
    private Integer enabledStatus;
}
