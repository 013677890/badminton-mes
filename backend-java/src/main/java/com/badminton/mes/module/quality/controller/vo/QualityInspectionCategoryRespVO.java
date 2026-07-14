package com.badminton.mes.module.quality.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/** 检验分类响应。 */
@Data
public class QualityInspectionCategoryRespVO {

    private Long id;
    private String categoryCode;
    private String categoryName;
    private Integer enabledStatus;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
