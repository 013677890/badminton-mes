package com.badminton.mes.module.equipment.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备制造商分页查询请求 VO，继承 {@link PageParam} 获得分页参数与入参保护。
 *
 * <p>关键字和启停状态均为可选条件，可与父类分页参数组合使用。查询层负责追加逻辑删除过滤，
 * 避免已删除制造商进入正常选择列表。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EquipmentManufacturerPageReqVO extends PageParam {

    /** 制造商编码或名称关键字，采用模糊匹配，可空，最大 64 个字符。 */
    @Size(max = 64, message = "搜索关键字长度不能超过 64")
    private String keyword;

    /** 启停状态精确筛选值：1 表示启用，0 表示停用；为空时查询全部状态。 */
    @Min(value = 0, message = "状态取值为 0 或 1")
    @Max(value = 1, message = "状态取值为 0 或 1")
    private Integer status;
}
