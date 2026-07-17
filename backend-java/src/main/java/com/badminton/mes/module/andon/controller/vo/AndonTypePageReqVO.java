package com.badminton.mes.module.andon.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 安灯类型分页查询请求。
 *
 * <p>在通用分页参数基础上按文本、异常类别、处理方式和启用状态组合筛选类型主数据。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AndonTypePageReqVO extends PageParam {

    /** 类型关键字，用于模糊匹配类型编码、名称等可检索文本。 */
    @Size(max = 64, message = "搜索关键字长度不能超过 64")
    private String keyword;

    /** 异常类别筛选值：生产、设备、质量、物料或非生产异常。 */
    @Pattern(regexp = "^(PRODUCTION|EQUIPMENT|QUALITY|MATERIAL|NON_PRODUCTION)$", message = "异常类别不合法")
    private String exceptionCategory;

    /** 处理方式筛选值：无需处理、自行处理或请求协助。 */
    @Pattern(regexp = "^(NO_ACTION|SELF_HANDLE|ASSISTANCE)$", message = "处理方式不合法")
    private String handlingMode;

    /** 启用状态筛选值：{@code 1} 启用，{@code 0} 停用。 */
    @Min(value = 0, message = "启用状态只能为 0 或 1")
    @Max(value = 1, message = "启用状态只能为 0 或 1")
    private Integer enabledStatus;
}
