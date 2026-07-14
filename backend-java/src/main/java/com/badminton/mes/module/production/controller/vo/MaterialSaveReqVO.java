package com.badminton.mes.module.production.controller.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 物料主档创建与修改请求。 */
@Data
public class MaterialSaveReqVO {
    /** 物料编码 */
    @NotBlank(message = "物料编码不能为空")
    @Size(max = 32, message = "物料编码长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "物料编码只能包含字母、数字、下划线和连字符")
    private String materialCode;
    /** 物料名称 */
    @NotBlank(message = "物料名称不能为空")
    @Size(max = 128, message = "物料名称长度不能超过 128")
    private String materialName;
    /** 规格型号 */
    @Size(max = 128, message = "规格型号长度不能超过 128")
    private String spec;
    /** 物料类型 */
    @NotNull(message = "物料类型不能为空")
    private Integer materialType;
    /** 计量单位主键 */
    @NotNull(message = "计量单位不能为空")
    @Positive(message = "计量单位 id 必须为正数")
    private Long unitId;
    /** 是否关键物料 */
    @NotNull(message = "请设置是否关键物料")
    private Boolean keyMaterial;
    /** 启停状态 */
    @NotNull(message = "物料状态不能为空")
    @Min(value = 0, message = "物料状态不合法")
    @Max(value = 1, message = "物料状态不合法")
    private Integer status;
}
