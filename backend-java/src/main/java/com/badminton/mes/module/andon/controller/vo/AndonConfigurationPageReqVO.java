package com.badminton.mes.module.andon.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 安灯异常处理配置分页请求。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AndonConfigurationPageReqVO extends PageParam {

    @Positive(message = "安灯类型必须为正整数")
    private Long andonTypeId;

    @Positive(message = "产线必须为正整数")
    private Long productionLineId;

    @Positive(message = "处理人必须为正整数")
    private Long handlerUserId;

    @Min(value = 0, message = "启用状态只能为 0 或 1")
    @Max(value = 1, message = "启用状态只能为 0 或 1")
    private Integer enabledStatus;
}
