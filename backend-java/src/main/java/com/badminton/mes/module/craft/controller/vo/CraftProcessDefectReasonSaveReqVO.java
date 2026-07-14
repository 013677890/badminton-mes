package com.badminton.mes.module.craft.controller.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 工序不良原因保存请求 VO。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
public class CraftProcessDefectReasonSaveReqVO {

    /** 不良原因编码 */
    @NotBlank(message = "不良原因编码不能为空")
    @Size(max = 32, message = "不良原因编码长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "不良原因编码只能包含字母、数字、下划线和连字符")
    private String reasonCode;

    /** 不良原因名称 */
    @NotBlank(message = "不良原因名称不能为空")
    @Size(max = 64, message = "不良原因名称长度不能超过 64")
    private String reasonName;

    /** 状态：1 启用 0 停用 */
    @NotNull(message = "不良原因状态不能为空")
    @Min(value = 0, message = "不良原因状态取值为 0 或 1")
    @Max(value = 1, message = "不良原因状态取值为 0 或 1")
    private Integer status;

    /** 变更原因 */
    @Size(max = 255, message = "变更原因长度不能超过 255")
    private String changeReason;
}
