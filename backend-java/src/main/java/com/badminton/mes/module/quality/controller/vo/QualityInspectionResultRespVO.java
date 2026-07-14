package com.badminton.mes.module.quality.controller.vo;

import java.math.BigDecimal;

import lombok.Data;

/** 质量检验项目结果响应。 */
@Data
public class QualityInspectionResultRespVO {

    private Long id;
    private Long inspectionItemId;
    private String itemCode;
    private String itemName;
    private String valueType;
    private String unit;
    private Boolean requiredFlag;
    private String standardValue;
    private BigDecimal lowerLimit;
    private BigDecimal upperLimit;
    private String judgmentMethod;
    private String measuredValue;
    private String judgmentResult;
    private String defectDescription;
    private Integer sortOrder;
}
