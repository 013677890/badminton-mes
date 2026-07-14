package com.badminton.mes.module.andon.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 安灯类型分页请求。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AndonTypePageReqVO extends PageParam {

    @Size(max = 64, message = "搜索关键字长度不能超过 64")
    private String keyword;

    @Pattern(regexp = "^(PRODUCTION|EQUIPMENT|QUALITY|MATERIAL|NON_PRODUCTION)$", message = "异常类别不合法")
    private String exceptionCategory;

    @Pattern(regexp = "^(NO_ACTION|SELF_HANDLE|ASSISTANCE)$", message = "处理方式不合法")
    private String handlingMode;

    @Min(value = 0, message = "启用状态只能为 0 或 1")
    @Max(value = 1, message = "启用状态只能为 0 或 1")
    private Integer enabledStatus;
}
