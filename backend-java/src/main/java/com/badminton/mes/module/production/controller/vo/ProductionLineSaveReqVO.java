package com.badminton.mes.module.production.controller.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 产线基础资料创建与修改请求。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
@Data
public class ProductionLineSaveReqVO {

    /** 产线编码 */
    @NotBlank(message = "产线编码不能为空")
    @Size(max = 32, message = "产线编码长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "产线编码只能包含字母、数字、下划线和连字符")
    private String lineCode;

    /** 产线名称 */
    @NotBlank(message = "产线名称不能为空")
    @Size(max = 64, message = "产线名称长度不能超过 64")
    private String lineName;

    /** 所属车间 id */
    @NotNull(message = "所属车间不能为空")
    @Positive(message = "车间 id 必须为正数")
    private Long workshopId;

    /** 标准日产能，可空 */
    @Positive(message = "标准日产能必须大于 0")
    private Integer standardCapacity;

    /** 状态：1 启用 0 停用 */
    @NotNull(message = "产线状态不能为空")
    @Min(value = 0, message = "产线状态不合法")
    @Max(value = 1, message = "产线状态不合法")
    private Integer status;
}
