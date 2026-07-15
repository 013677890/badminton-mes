package com.badminton.mes.module.quality.controller.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 检验项目基础资料响应。
 *
 * <p>返回项目所属分类、采集值类型、判定规则、启停状态及审计时间，供详情和分页列表统一展示。
 */
@Data
public class QualityInspectionItemRespVO {

    /** 检验项目数据库主键。 */
    private Long id;

    /** 检验项目业务编码，在未删除项目中保持唯一。 */
    private String itemCode;

    /** 检验项目名称。 */
    private String itemName;

    /** 所属检验分类主键。 */
    private Long categoryId;

    /** 所属检验分类业务编码。 */
    private String categoryCode;

    /** 所属检验分类名称。 */
    private String categoryName;

    /** 采集值类型：NUMERIC 数值、TEXT 文本、BOOLEAN 布尔。 */
    private String valueType;

    /** 数值型项目的计量单位；非数值型项目通常为空。 */
    private String unit;

    /** 标准目标值；STANDARD_VALUE 判定方式以该值作为比对依据。 */
    private String standardValue;

    /** 数值合格区间下限；与上限共同服务于 RANGE 判定。 */
    private BigDecimal lowerLimit;

    /** 数值合格区间上限；不得小于下限。 */
    private BigDecimal upperLimit;

    /** 判定方式：RANGE 区间判定、STANDARD_VALUE 标准值比对、MANUAL 人工判定。 */
    private String judgmentMethod;

    /** 现场操作、仪器、取样或观测方法说明。 */
    private String inspectionMethod;

    /** 是否为默认必检项目；方案项目可在固化规则时覆盖该值。 */
    private Boolean requiredFlag;

    /** 启用状态：0 停用、1 启用。 */
    private Integer enabledStatus;

    /** 项目适用范围、维护原因或其他说明。 */
    private String remark;

    /** 项目创建时间，按系统记录时间格式化到秒。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 项目最近一次更新时间，按系统记录时间格式化到秒。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
