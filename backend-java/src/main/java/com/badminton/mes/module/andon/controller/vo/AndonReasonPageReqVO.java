package com.badminton.mes.module.andon.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 安灯异常原因分页查询请求。
 *
 * <p>在通用分页参数基础上按文本、所属安灯类型及启用状态组合筛选，未填写的字段不参与过滤。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AndonReasonPageReqVO extends PageParam {

    /** 原因关键字，用于模糊匹配原因编码、名称等可检索文本。 */
    @Size(max = 64, message = "搜索关键字长度不能超过 64")
    private String keyword;

    /** 所属安灯类型主键，用于仅查询某一类型可配置或已停用的原因。 */
    @Positive(message = "安灯类型必须为正整数")
    private Long andonTypeId;

    /** 启用状态筛选值：{@code 1} 启用，{@code 0} 停用。 */
    @Min(value = 0, message = "启用状态只能为 0 或 1")
    @Max(value = 1, message = "启用状态只能为 0 或 1")
    private Integer enabledStatus;
}
