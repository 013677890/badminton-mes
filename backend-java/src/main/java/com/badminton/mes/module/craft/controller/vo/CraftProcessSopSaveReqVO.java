package com.badminton.mes.module.craft.controller.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 工序 SOP 保存请求 VO。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
public class CraftProcessSopSaveReqVO {

    /** SOP 编码 */
    @NotBlank(message = "SOP 编码不能为空")
    @Size(max = 32, message = "SOP 编码长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "SOP 编码只能包含字母、数字、下划线和连字符")
    private String sopCode;

    /** SOP 名称 */
    @NotBlank(message = "SOP 名称不能为空")
    @Size(max = 64, message = "SOP 名称长度不能超过 64")
    private String sopName;

    /** SOP 版本 */
    @NotBlank(message = "SOP 版本不能为空")
    @Size(max = 32, message = "SOP 版本长度不能超过 32")
    private String sopVersion;

    /** SOP 文件地址 */
    @NotBlank(message = "SOP 文件地址不能为空")
    @Size(max = 512, message = "SOP 文件地址长度不能超过 512")
    private String fileUrl;

    /** 状态：1 启用 0 停用 */
    @NotNull(message = "SOP 状态不能为空")
    @Min(value = 0, message = "SOP 状态取值为 0 或 1")
    @Max(value = 1, message = "SOP 状态取值为 0 或 1")
    private Integer status;

    /** 变更原因 */
    @Size(max = 255, message = "变更原因长度不能超过 255")
    private String changeReason;
}
