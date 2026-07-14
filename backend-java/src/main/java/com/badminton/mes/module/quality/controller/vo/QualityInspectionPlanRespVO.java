package com.badminton.mes.module.quality.controller.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/** 检验标准方案响应。 */
@Data
public class QualityInspectionPlanRespVO {

    private Long id;
    private String planCode;
    private String planName;
    private Long productId;
    private Long customerId;
    private String inspectionType;
    private Integer versionNo;
    private String planStatus;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveDate;

    private Boolean defaultFlag;
    private String remark;
    private Long createBy;
    private Long auditBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    private List<QualityInspectionPlanItemRespVO> items;
}
