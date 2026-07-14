package com.badminton.mes.module.quality.controller.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/** 检验项目响应。 */
@Data
public class QualityInspectionItemRespVO {

    private Long id;
    private String itemCode;
    private String itemName;
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private String valueType;
    private String unit;
    private String standardValue;
    private BigDecimal lowerLimit;
    private BigDecimal upperLimit;
    private String judgmentMethod;
    private String inspectionMethod;
    private Boolean requiredFlag;
    private Integer enabledStatus;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
