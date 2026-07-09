package com.badminton.mes.module.equipment.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备类别分页查询请求 VO，继承 {@link PageParam} 获得分页参数与入参保护。
 *
 * <p>筛选条件：类别编码/名称右模糊，状态精确匹配，父级类别精确匹配。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EquipmentCategoryPageReqVO extends PageParam {

    /** 类别编码或名称，模糊匹配，可空 */
    @Size(max = 64, message = "搜索关键字长度不能超过 64")
    private String keyword;

    /** 父级类别 id，精确匹配，可空 */
    @Positive(message = "父级类别 id 必须为正数")
    private Long parentId;

    /** 状态筛选，可空，取值 0 或 1 */
    @Min(value = 0, message = "状态取值为 0 或 1")
    @Max(value = 1, message = "状态取值为 0 或 1")
    private Integer status;
}
