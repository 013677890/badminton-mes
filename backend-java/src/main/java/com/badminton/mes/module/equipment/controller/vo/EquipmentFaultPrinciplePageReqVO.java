package com.badminton.mes.module.equipment.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备故障原理分页查询请求 VO。
 *
 * <p>所有筛选条件均可空，并可与 {@link PageParam} 的分页参数组合使用。关键字采用模糊匹配，
 * 设备类别、故障等级和启停状态采用精确匹配；查询层统一排除逻辑删除记录。
 *
 * @author 角色C
 * @date 2026/07/10
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EquipmentFaultPrinciplePageReqVO extends PageParam {

    /** 故障编码、名称或描述关键字，采用模糊匹配，可空，最大 64 个字符。 */
    @Size(max = 64, message = "搜索关键字长度不能超过 64")
    private String keyword;

    /** 适用设备类别主键，精确筛选该类别的故障原理；非空时必须为正数。 */
    @Positive(message = "设备类别必须为正整数")
    private Long categoryId;

    /** 故障严重等级，可按 LOW、MEDIUM、HIGH 或 CRITICAL 精确筛选。 */
    @Pattern(regexp = "^(LOW|MEDIUM|HIGH|CRITICAL)$", message = "故障等级必须为 LOW、MEDIUM、HIGH、CRITICAL 之一")
    private String faultLevel;

    /** 启停状态精确筛选值：1 表示启用，0 表示停用；为空时查询全部状态。 */
    @Min(value = 0, message = "状态取值为 0 或 1")
    @Max(value = 1, message = "状态取值为 0 或 1")
    private Integer status;
}
