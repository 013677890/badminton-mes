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
 * @author 角色C
 * @date 2026/07/10
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EquipmentFaultPrinciplePageReqVO extends PageParam {

    /** 故障编码、名称或描述，模糊匹配，可空 */
    @Size(max = 64, message = "搜索关键字长度不能超过 64")
    private String keyword;

    /** 适用设备类别 id，可空 */
    @Positive(message = "设备类别必须为正整数")
    private Long categoryId;

    /** 故障等级，可空 */
    @Pattern(regexp = "^(LOW|MEDIUM|HIGH|CRITICAL)$", message = "故障等级必须为 LOW、MEDIUM、HIGH、CRITICAL 之一")
    private String faultLevel;

    /** 状态筛选，可空，取值 0 或 1 */
    @Min(value = 0, message = "状态取值为 0 或 1")
    @Max(value = 1, message = "状态取值为 0 或 1")
    private Integer status;
}
