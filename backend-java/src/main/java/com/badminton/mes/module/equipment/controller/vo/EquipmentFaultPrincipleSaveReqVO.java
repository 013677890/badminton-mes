package com.badminton.mes.module.equipment.controller.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设备故障原理创建/修改请求 VO。
 *
 * <p>注解负责必填、长度、正数、等级枚举和数值范围等单字段约束。故障编码唯一性、适用设备类别
 * 是否存在以及修改目标是否有效等依赖持久层的业务规则由 Service 校验。
 *
 * @author 角色C
 * @date 2026/07/10
 */
@Data
public class EquipmentFaultPrincipleSaveReqVO {

    /** 业务唯一的故障编码，不能为空，最大 32 个字符。 */
    @NotBlank(message = "故障编码不能为空")
    @Size(max = 32, message = "故障编码长度不能超过 32")
    private String faultCode;

    /** 面向报修人员展示和选择的故障名称。 */
    @NotBlank(message = "故障名称不能为空")
    @Size(max = 128, message = "故障名称长度不能超过 128")
    private String faultName;

    /** 适用设备类别主键；为空表示可供所有设备类别使用。 */
    @Positive(message = "设备类别必须为正整数")
    private Long categoryId;

    /** 故障严重等级；可空并默认 LOW，可选 LOW、MEDIUM、HIGH 或 CRITICAL。 */
    @Pattern(regexp = "^(LOW|MEDIUM|HIGH|CRITICAL)$", message = "故障等级必须为 LOW、MEDIUM、HIGH、CRITICAL 之一")
    private String faultLevel;

    /** 典型故障现象、成因或判定依据，可空，最大 500 个字符。 */
    @Size(max = 500, message = "故障描述长度不能超过 500")
    private String faultDescription;

    /** 针对该故障的标准排查步骤或建议处理方案，可空，最大 500 个字符。 */
    @Size(max = 500, message = "建议处理方案长度不能超过 500")
    private String suggestedSolution;

    /** 故障原理在选择列表中的展示顺序；可空并默认 0，数值越小越靠前。 */
    @Min(value = 0, message = "排序号不能小于 0")
    @Max(value = 999999, message = "排序号不能超过 999999")
    private Integer sortOrder;

    /** 适用限制、维护来源等补充说明，可空，最大 255 个字符。 */
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;

    /** 启停状态：1 表示启用，0 表示停用；可空并默认 1。 */
    @Min(value = 0, message = "状态取值为 0 或 1")
    @Max(value = 1, message = "状态取值为 0 或 1")
    private Integer status;
}
