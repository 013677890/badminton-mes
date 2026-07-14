package com.badminton.mes.module.integration.controller.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 外部计量单位写入请求。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
@Data
public class UnitWriteReqVO {

    /** 来源系统编码 */
    @NotBlank(message = "来源系统不能为空")
    @Size(max = 32, message = "来源系统长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "来源系统只能包含字母、数字、下划线和连字符")
    private String sourceSystem;

    /** 单位编码 */
    @NotBlank(message = "单位编码不能为空")
    @Size(max = 32, message = "单位编码长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "单位编码只能包含字母、数字、下划线和连字符")
    private String unitCode;

    /** 单位名称 */
    @NotBlank(message = "单位名称不能为空")
    @Size(max = 64, message = "单位名称长度不能超过 64")
    private String unitName;

    /** 数量小数精度 */
    @NotNull(message = "小数精度不能为空")
    @Min(value = 0, message = "小数精度不能小于 0")
    @Max(value = 6, message = "小数精度不能大于 6")
    private Integer decimalPrecision;

    /** 状态：1 启用 0 停用 */
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态只能为 0 或 1")
    @Max(value = 1, message = "状态只能为 0 或 1")
    private Integer status;
}
