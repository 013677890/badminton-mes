package com.badminton.mes.module.quality.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 检验项目分页请求。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QualityInspectionItemPageReqVO extends PageParam {

    @Size(max = 64, message = "搜索关键字长度不能超过 64")
    private String keyword;

    @Positive(message = "检验分类必须为正整数")
    private Long categoryId;

    @Pattern(regexp = "^(NUMERIC|TEXT|BOOLEAN)$", message = "值类型不合法")
    private String valueType;

    private Boolean requiredFlag;

    @Min(value = 0, message = "启用状态只能为 0 或 1")
    @Max(value = 1, message = "启用状态只能为 0 或 1")
    private Integer enabledStatus;
}
