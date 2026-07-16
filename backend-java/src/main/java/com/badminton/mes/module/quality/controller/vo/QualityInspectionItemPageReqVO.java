package com.badminton.mes.module.quality.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 检验项目分页筛选请求。
 *
 * <p>各筛选条件均为可选条件；分页参数继承自 {@link PageParam}，组合条件用于检索未逻辑删除的项目资料。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QualityInspectionItemPageReqVO extends PageParam {

    /** 项目编码或项目名称的模糊搜索关键字；最长 64 个字符。 */
    @Size(max = 64, message = "搜索关键字长度不能超过 64")
    private String keyword;

    /** 检验分类主键精确条件；填写时必须为正整数。 */
    @Positive(message = "检验分类必须为正整数")
    private Long categoryId;

    /** 值类型精确条件：NUMERIC 数值、TEXT 文本、BOOLEAN 布尔。 */
    @Pattern(regexp = "^(NUMERIC|TEXT|BOOLEAN)$", message = "值类型不合法")
    private String valueType;

    /** 必检标记精确条件；true 仅查询必检项目，false 仅查询非必检项目。 */
    private Boolean requiredFlag;

    /** 启用状态精确条件：0 停用、1 启用；为空时不限制状态。 */
    @Min(value = 0, message = "启用状态只能为 0 或 1")
    @Max(value = 1, message = "启用状态只能为 0 或 1")
    private Integer enabledStatus;
}
