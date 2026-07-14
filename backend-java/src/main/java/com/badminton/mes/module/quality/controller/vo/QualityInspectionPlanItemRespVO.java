package com.badminton.mes.module.quality.controller.vo;

import java.math.BigDecimal;

import lombok.Data;

/** 检验方案项目明细响应。 */
@Data
public class QualityInspectionPlanItemRespVO {

    private Long id;
    private Long inspectionItemId;
    private String itemCode;
    private String itemName;
    private String valueType;
    private String unit;
    private Integer sortOrder;
    private Integer sampleQuantity;
    private Boolean requiredFlag;
    private String standardValue;
    private BigDecimal lowerLimit;
    private BigDecimal upperLimit;
    private String judgmentMethod;
}
