package com.badminton.mes.module.quality.controller.vo;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 检验方案中的项目规则明细响应。
 *
 * <p>该对象展示方案保存时固化的项目顺序、抽样要求和判定参数，供方案详情及后续检验单建单使用。
 */
@Data
public class QualityInspectionPlanItemRespVO {

    /** 方案项目明细主键，用于标识方案与检验项目之间的具体规则记录。 */
    private Long id;

    /** 关联的检验项目主键。 */
    private Long inspectionItemId;

    /** 检验项目业务编码。 */
    private String itemCode;

    /** 检验项目名称。 */
    private String itemName;

    /** 采集值类型：NUMERIC 数值、TEXT 文本、BOOLEAN 布尔。 */
    private String valueType;

    /** 数值型项目的计量单位；非数值型项目通常为空。 */
    private String unit;

    /** 方案内显示与执行顺序，按非负数由小到大排列。 */
    private Integer sortOrder;

    /** 该项目在一次检验中的计划抽样数量。 */
    private Integer sampleQuantity;

    /** 是否为提交检验单前必须完成判定的必检项目。 */
    private Boolean requiredFlag;

    /** 标准目标值；STANDARD_VALUE 判定方式以该值作为比对依据。 */
    private String standardValue;

    /** 数值合格区间下限；与上限共同服务于 RANGE 判定。 */
    private BigDecimal lowerLimit;

    /** 数值合格区间上限；不得小于下限。 */
    private BigDecimal upperLimit;

    /** 判定方式：RANGE 区间判定、STANDARD_VALUE 标准值比对、MANUAL 人工判定。 */
    private String judgmentMethod;
}
