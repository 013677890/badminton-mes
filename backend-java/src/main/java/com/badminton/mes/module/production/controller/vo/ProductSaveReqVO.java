package com.badminton.mes.module.production.controller.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 产品主档创建与修改请求。 */
@Data
public class ProductSaveReqVO {
    /** 产品编码 */
    @NotBlank(message = "产品编码不能为空")
    @Size(max = 32, message = "产品编码长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "产品编码只能包含字母、数字、下划线和连字符")
    private String productCode;
    /** 产品名称 */
    @NotBlank(message = "产品名称不能为空")
    @Size(max = 128, message = "产品名称长度不能超过 128")
    private String productName;
    /** 规格型号 */
    @Size(max = 128, message = "规格型号长度不能超过 128")
    private String spec;
    /** 产品类型：1 成品 2 半成品 */
    @NotNull(message = "产品类型不能为空")
    private Integer productType;
    /** 产品等级 */
    @Size(max = 32, message = "产品等级长度不能超过 32")
    private String grade;
    /** 计量单位主键 */
    @NotNull(message = "计量单位不能为空")
    @Positive(message = "计量单位 id 必须为正数")
    private Long unitId;
    /** 状态：1 启用 0 停用 */
    @NotNull(message = "产品状态不能为空")
    @Min(value = 0, message = "产品状态不合法")
    @Max(value = 1, message = "产品状态不合法")
    private Integer status;
}
