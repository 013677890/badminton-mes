package com.badminton.mes.module.quality.controller.vo;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 检验项目创建/修改请求。 */
@Data
public class QualityInspectionItemSaveReqVO {

    @NotBlank(message = "检验项目编码不能为空")
    @Size(max = 32, message = "检验项目编码长度不能超过 32")
    @Pattern(regexp = "^(?!__DELETED_).+$", message = "检验项目编码不能使用系统保留前缀")
    private String itemCode;

    @NotBlank(message = "检验项目名称不能为空")
    @Size(max = 128, message = "检验项目名称长度不能超过 128")
    private String itemName;

    @NotNull(message = "检验分类不能为空")
    @Positive(message = "检验分类必须为正整数")
    private Long categoryId;

    @NotBlank(message = "值类型不能为空")
    @Pattern(regexp = "^(NUMERIC|TEXT|BOOLEAN)$", message = "值类型必须为 NUMERIC、TEXT 或 BOOLEAN")
    private String valueType;

    @Size(max = 32, message = "计量单位长度不能超过 32")
    private String unit;

    @Size(max = 128, message = "标准值长度不能超过 128")
    private String standardValue;

    @DecimalMin(value = "-999999999999.999999", message = "数值下限超出允许范围")
    @DecimalMax(value = "999999999999.999999", message = "数值下限超出允许范围")
    @Digits(integer = 12, fraction = 6, message = "数值下限最多 12 位整数和 6 位小数")
    private BigDecimal lowerLimit;

    @DecimalMin(value = "-999999999999.999999", message = "数值上限超出允许范围")
    @DecimalMax(value = "999999999999.999999", message = "数值上限超出允许范围")
    @Digits(integer = 12, fraction = 6, message = "数值上限最多 12 位整数和 6 位小数")
    private BigDecimal upperLimit;

    @NotBlank(message = "判定方式不能为空")
    @Pattern(regexp = "^(RANGE|STANDARD_VALUE|MANUAL)$",
             message = "判定方式必须为 RANGE、STANDARD_VALUE 或 MANUAL")
    private String judgmentMethod;

    @Size(max = 255, message = "检验方法长度不能超过 255")
    private String inspectionMethod;

    private Boolean requiredFlag;

    @Min(value = 0, message = "启用状态只能为 0 或 1")
    @Max(value = 1, message = "启用状态只能为 0 或 1")
    private Integer enabledStatus;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
