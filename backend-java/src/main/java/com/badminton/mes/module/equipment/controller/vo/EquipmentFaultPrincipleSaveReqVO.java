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
 * @author 角色C
 * @date 2026/07/10
 */
@Data
public class EquipmentFaultPrincipleSaveReqVO {

    /** 故障编码，唯一 */
    @NotBlank(message = "故障编码不能为空")
    @Size(max = 32, message = "故障编码长度不能超过 32")
    private String faultCode;

    /** 故障名称 */
    @NotBlank(message = "故障名称不能为空")
    @Size(max = 128, message = "故障名称长度不能超过 128")
    private String faultName;

    /** 适用设备类别 id，可空，为空表示通用故障 */
    @Positive(message = "设备类别必须为正整数")
    private Long categoryId;

    /** 故障等级，可空，默认 LOW */
    @Pattern(regexp = "^(LOW|MEDIUM|HIGH|CRITICAL)$", message = "故障等级必须为 LOW、MEDIUM、HIGH、CRITICAL 之一")
    private String faultLevel;

    /** 故障描述 */
    @Size(max = 500, message = "故障描述长度不能超过 500")
    private String faultDescription;

    /** 建议处理方案 */
    @Size(max = 500, message = "建议处理方案长度不能超过 500")
    private String suggestedSolution;

    /** 排序号，可空，默认 0 */
    @Min(value = 0, message = "排序号不能小于 0")
    @Max(value = 999999, message = "排序号不能超过 999999")
    private Integer sortOrder;

    /** 备注说明 */
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;

    /** 状态：1 启用 0 停用，可空，默认 1 */
    @Min(value = 0, message = "状态取值为 0 或 1")
    @Max(value = 1, message = "状态取值为 0 或 1")
    private Integer status;
}
