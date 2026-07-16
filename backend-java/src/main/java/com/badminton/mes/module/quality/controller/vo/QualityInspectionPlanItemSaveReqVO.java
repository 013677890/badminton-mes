package com.badminton.mes.module.quality.controller.vo;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 检验方案中的单个项目规则保存请求。
 *
 * <p>项目主键与抽样数量为必填项；其余规则用于覆盖检验项目基础资料，并在方案保存时固化为方案项快照。
 */
@Data
public class QualityInspectionPlanItemSaveReqVO {

    /** 被纳入方案的检验项目主键；不能为空且必须为正整数，同一方案内不可重复。 */
    @NotNull(message = "检验项目不能为空")
    @Positive(message = "检验项目必须为正整数")
    private Long inspectionItemId;

    /** 方案内显示与执行顺序；最小为 0，未填写时由服务层采用默认顺序。 */
    @Min(value = 0, message = "显示顺序不能为负数")
    private Integer sortOrder;

    /** 该项目每次检验的计划抽样数量；不能为空且必须大于 0。 */
    @NotNull(message = "抽样数量不能为空")
    @Positive(message = "抽样数量必须为正整数")
    private Integer sampleQuantity;

    /** 是否为必检项目；为空时继承检验项目基础资料中的必检设置。 */
    private Boolean requiredFlag;

    /** 标准目标值覆盖项，最长 128 个字符；用于 STANDARD_VALUE 判定。 */
    @Size(max = 128, message = "标准值长度不能超过 128")
    private String standardValue;

    /** 数值合格区间下限覆盖项；RANGE 判定时必须与上限同时有效。 */
    private BigDecimal lowerLimit;

    /** 数值合格区间上限覆盖项；RANGE 判定时不得小于下限。 */
    private BigDecimal upperLimit;

    /** 判定方式覆盖项：RANGE 区间、STANDARD_VALUE 标准值、MANUAL 人工判定。 */
    @Pattern(regexp = "^(RANGE|STANDARD_VALUE|MANUAL)$", message = "判定方式不合法")
    private String judgmentMethod;
}
