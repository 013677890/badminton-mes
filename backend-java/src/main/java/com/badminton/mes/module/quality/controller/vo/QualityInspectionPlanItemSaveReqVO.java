package com.badminton.mes.module.quality.controller.vo;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 检验方案项目明细保存请求。 */
@Data
public class QualityInspectionPlanItemSaveReqVO {

    @NotNull(message = "检验项目不能为空")
    @Positive(message = "检验项目必须为正整数")
    private Long inspectionItemId;

    @Min(value = 0, message = "显示顺序不能为负数")
    private Integer sortOrder;

    @NotNull(message = "抽样数量不能为空")
    @Positive(message = "抽样数量必须为正整数")
    private Integer sampleQuantity;

    private Boolean requiredFlag;

    @Size(max = 128, message = "标准值长度不能超过 128")
    private String standardValue;

    private BigDecimal lowerLimit;

    private BigDecimal upperLimit;

    @Pattern(regexp = "^(RANGE|STANDARD_VALUE|MANUAL)$", message = "判定方式不合法")
    private String judgmentMethod;
}
