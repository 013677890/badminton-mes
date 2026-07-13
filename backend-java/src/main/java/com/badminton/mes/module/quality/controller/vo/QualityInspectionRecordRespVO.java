package com.badminton.mes.module.quality.controller.vo;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/** 质量检验单响应。 */
@Data
public class QualityInspectionRecordRespVO {

    private Long id;
    private String inspectionNo;
    private String inspectionType;
    private Long planId;
    private String planCode;
    private Integer planVersion;
    private Long workOrderId;
    private Long sourceDocumentId;
    private String sourceDocumentNo;
    private Long productId;
    private Long customerId;
    private Long productionLineId;
    private String batchNo;
    private Integer sampleQuantity;
    private String recordStatus;
    private String conclusion;
    private String releaseStatus;
    private String nonconformanceDescription;
    private String disposition;
    private Long inspectorId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime inspectedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    private List<QualityInspectionResultRespVO> results;
}
