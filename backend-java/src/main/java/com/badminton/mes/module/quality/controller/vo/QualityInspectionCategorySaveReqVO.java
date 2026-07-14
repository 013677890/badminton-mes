package com.badminton.mes.module.quality.controller.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 检验分类创建/修改请求。 */
@Data
public class QualityInspectionCategorySaveReqVO {

    @NotBlank(message = "检验分类编码不能为空")
    @Size(max = 32, message = "检验分类编码长度不能超过 32")
    @Pattern(regexp = "^(?!__DELETED_).+$", message = "检验分类编码不能使用系统保留前缀")
    private String categoryCode;

    @NotBlank(message = "检验分类名称不能为空")
    @Size(max = 128, message = "检验分类名称长度不能超过 128")
    private String categoryName;

    @Min(value = 0, message = "启用状态只能为 0 或 1")
    @Max(value = 1, message = "启用状态只能为 0 或 1")
    private Integer enabledStatus;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
