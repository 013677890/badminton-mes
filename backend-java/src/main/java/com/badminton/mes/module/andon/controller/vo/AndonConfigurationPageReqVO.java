package com.badminton.mes.module.andon.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 安灯异常处理配置分页查询请求。
 *
 * <p>按规则所属类型、产线作用域、具体处理人和启用状态组合筛选；未填写的条件不参与过滤。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AndonConfigurationPageReqVO extends PageParam {

    /** 安灯类型主键，用于查看某一类型的产线配置及全局规则。 */
    @Positive(message = "安灯类型必须为正整数")
    private Long andonTypeId;

    /** 产线主键，用于定位覆盖该产线的专属处理规则。 */
    @Positive(message = "产线必须为正整数")
    private Long productionLineId;

    /** 初始处理用户主键，用于查询由某一用户直接承接的配置。 */
    @Positive(message = "处理人必须为正整数")
    private Long handlerUserId;

    /** 启用状态筛选值：{@code 1} 生效，{@code 0} 停用。 */
    @Min(value = 0, message = "启用状态只能为 0 或 1")
    @Max(value = 1, message = "启用状态只能为 0 或 1")
    private Integer enabledStatus;
}
