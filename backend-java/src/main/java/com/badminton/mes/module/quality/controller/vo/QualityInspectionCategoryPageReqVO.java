package com.badminton.mes.module.quality.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 检验分类分页请求。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QualityInspectionCategoryPageReqVO extends PageParam {

    @Size(max = 64, message = "搜索关键字长度不能超过 64")
    private String keyword;

    @Min(value = 0, message = "启用状态只能为 0 或 1")
    @Max(value = 1, message = "启用状态只能为 0 或 1")
    private Integer enabledStatus;
}
