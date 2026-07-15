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

/**
 * 检验项目创建或修改请求。
 *
 * <p>除注解声明的单字段边界外，服务层还校验组合规则：NUMERIC 必须提供单位及有序上下限，非数值类型
 * 不得携带上下限；RANGE 仅适用于数值类型，STANDARD_VALUE 必须提供标准值。
 */
@Data
public class QualityInspectionItemSaveReqVO {

    /** 项目业务编码；不能为空、最长 32 个字符，且不得使用系统逻辑删除保留前缀。 */
    @NotBlank(message = "检验项目编码不能为空")
    @Size(max = 32, message = "检验项目编码长度不能超过 32")
    @Pattern(regexp = "^(?!__DELETED_).+$", message = "检验项目编码不能使用系统保留前缀")
    private String itemCode;

    /** 项目名称；不能为空且最长 128 个字符。 */
    @NotBlank(message = "检验项目名称不能为空")
    @Size(max = 128, message = "检验项目名称长度不能超过 128")
    private String itemName;

    /** 所属检验分类主键；不能为空、必须为正整数，且分类必须处于启用状态。 */
    @NotNull(message = "检验分类不能为空")
    @Positive(message = "检验分类必须为正整数")
    private Long categoryId;

    /** 采集值类型：NUMERIC 数值、TEXT 文本、BOOLEAN 布尔。 */
    @NotBlank(message = "值类型不能为空")
    @Pattern(regexp = "^(NUMERIC|TEXT|BOOLEAN)$", message = "值类型必须为 NUMERIC、TEXT 或 BOOLEAN")
    private String valueType;

    /** 数值型项目的计量单位，最长 32 个字符；NUMERIC 类型必须填写。 */
    @Size(max = 32, message = "计量单位长度不能超过 32")
    private String unit;

    /** 标准目标值，最长 128 个字符；STANDARD_VALUE 判定方式必须填写。 */
    @Size(max = 128, message = "标准值长度不能超过 128")
    private String standardValue;

    /** 数值合格区间下限；限 12 位整数和 6 位小数，最小为 -999999999999.999999。 */
    @DecimalMin(value = "-999999999999.999999", message = "数值下限超出允许范围")
    @DecimalMax(value = "999999999999.999999", message = "数值下限超出允许范围")
    @Digits(integer = 12, fraction = 6, message = "数值下限最多 12 位整数和 6 位小数")
    private BigDecimal lowerLimit;

    /** 数值合格区间上限；限 12 位整数和 6 位小数，最大为 999999999999.999999，且不得小于下限。 */
    @DecimalMin(value = "-999999999999.999999", message = "数值上限超出允许范围")
    @DecimalMax(value = "999999999999.999999", message = "数值上限超出允许范围")
    @Digits(integer = 12, fraction = 6, message = "数值上限最多 12 位整数和 6 位小数")
    private BigDecimal upperLimit;

    /** 判定方式：RANGE 区间判定、STANDARD_VALUE 标准值比对、MANUAL 人工判定。 */
    @NotBlank(message = "判定方式不能为空")
    @Pattern(regexp = "^(RANGE|STANDARD_VALUE|MANUAL)$",
             message = "判定方式必须为 RANGE、STANDARD_VALUE 或 MANUAL")
    private String judgmentMethod;

    /** 现场操作、仪器或取样方法说明，最长 255 个字符。 */
    @Size(max = 255, message = "检验方法长度不能超过 255")
    private String inspectionMethod;

    /** 是否为必检项目；创建时为空按 false 处理，修改时为空保留原值。 */
    private Boolean requiredFlag;

    /** 启用状态：0 停用、1 启用；创建时为空按启用处理，修改时为空保留原值。 */
    @Min(value = 0, message = "启用状态只能为 0 或 1")
    @Max(value = 1, message = "启用状态只能为 0 或 1")
    private Integer enabledStatus;

    /** 项目适用范围、维护原因或其他备注，最长 255 个字符。 */
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
