package com.badminton.mes.module.quality.controller.vo;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 质量检验单中的单项结果响应。
 *
 * <p>项目名称、采集类型和判定规则均为检验单创建时形成的快照，避免基础资料或方案后续变更影响历史追溯。
 */
@Data
public class QualityInspectionResultRespVO {

    /** 检验结果快照主键，保存实测结果时作为结果标识提交。 */
    private Long id;

    /** 结果快照来源的检验项目主键。 */
    private Long inspectionItemId;

    /** 建单时固化的检验项目业务编码。 */
    private String itemCode;

    /** 建单时固化的检验项目名称。 */
    private String itemName;

    /** 采集值类型快照：NUMERIC 数值、TEXT 文本、BOOLEAN 布尔。 */
    private String valueType;

    /** 数值型项目的计量单位快照；非数值型项目通常为空。 */
    private String unit;

    /** 是否为提交检验单前必须完成判定的必检项目。 */
    private Boolean requiredFlag;

    /** 标准目标值快照，供 STANDARD_VALUE 方式比对。 */
    private String standardValue;

    /** 数值合格区间下限快照，供 RANGE 方式判定。 */
    private BigDecimal lowerLimit;

    /** 数值合格区间上限快照，供 RANGE 方式判定。 */
    private BigDecimal upperLimit;

    /** 判定方式快照：RANGE 区间、STANDARD_VALUE 标准值、MANUAL 人工判定。 */
    private String judgmentMethod;

    /** 检验员录入的实际测量值或文本值；尚未采集时为空。 */
    private String measuredValue;

    /** 项目级判定结果：PASS 合格、FAIL 不合格；尚未判定时为空。 */
    private String judgmentResult;

    /** 项目不合格时记录的不良现象或偏差说明。 */
    private String defectDescription;

    /** 结果在检验单中的显示与执行顺序，按非负数由小到大排列。 */
    private Integer sortOrder;
}
