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
 * <p>所有业务筛选条件均可空，可与父类的页码、每页数量组合使用。关键字匹配类别编码或名称，
 * 父类别和启停状态采用精确匹配；逻辑删除过滤由查询层统一追加。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EquipmentCategoryPageReqVO extends PageParam {

    /** 类别编码或名称关键字，采用模糊匹配，可空，最大 64 个字符。 */
    @Size(max = 64, message = "搜索关键字长度不能超过 64")
    private String keyword;

    /** 父类别主键，精确筛选其直接下级类别；可空，非空时必须为正数。 */
    @Positive(message = "父级类别 id 必须为正数")
    private Long parentId;

    /** 启停状态精确筛选值：1 表示启用，0 表示停用；为空时查询全部状态。 */
    @Min(value = 0, message = "状态取值为 0 或 1")
    @Max(value = 1, message = "状态取值为 0 或 1")
    private Integer status;
}
