package com.badminton.mes.module.production.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 车间分页查询请求。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkshopPageReqVO extends PageParam {

    /** 车间编码前缀 */
    @Size(max = 32, message = "车间编码长度不能超过 32")
    private String workshopCode;

    /** 车间名称前缀 */
    @Size(max = 64, message = "车间名称长度不能超过 64")
    private String workshopName;

    /** 车间主管用户 id */
    @Positive(message = "车间主管 id 必须为正数")
    private Long managerId;

    /** 状态 */
    @Min(value = 0, message = "车间状态不合法")
    @Max(value = 1, message = "车间状态不合法")
    private Integer status;
}
