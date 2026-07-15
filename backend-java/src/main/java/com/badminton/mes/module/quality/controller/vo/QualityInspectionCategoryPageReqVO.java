package com.badminton.mes.module.quality.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 检验分类分页筛选请求。
 *
 * <p>关键字和启用状态均为可选条件，分页参数及其边界继承自 {@link PageParam}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QualityInspectionCategoryPageReqVO extends PageParam {

    /** 分类编码或分类名称的模糊搜索关键字，最长 64 个字符。 */
    @Size(max = 64, message = "搜索关键字长度不能超过 64")
    private String keyword;

    /** 启用状态精确条件：0 停用、1 启用；为空时不限制状态。 */
    @Min(value = 0, message = "启用状态只能为 0 或 1")
    @Max(value = 1, message = "启用状态只能为 0 或 1")
    private Integer enabledStatus;
}
