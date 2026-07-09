package com.badminton.mes.module.equipment.controller.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设备类别创建/修改请求 VO。
 *
 * <p>对外开放接口必须做入参校验，单字段规则用注解声明；
 * 跨字段业务规则在 Service 层校验。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Data
public class EquipmentCategorySaveReqVO {

    /** 类别编码，唯一 */
    @NotBlank(message = "类别编码不能为空")
    @Size(max = 32, message = "类别编码长度不能超过 32")
    private String categoryCode;

    /** 类别名称 */
    @NotBlank(message = "类别名称不能为空")
    @Size(max = 64, message = "类别名称长度不能超过 64")
    private String categoryName;

    /** 父级类别 id，顶级为 null */
    @Positive(message = "父级类别 id 必须为正数")
    private Long parentId;

    /** 排序号，数字越小越靠前，默认 0 */
    @Min(value = 0, message = "排序号不能为负数")
    @Max(value = 9999, message = "排序号最大值为 9999")
    private Integer sortOrder;

    /** 备注说明，可空 */
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;

    /** 状态：1 启用 0 停用，可空，默认 1 */
    @Min(value = 0, message = "状态取值为 0 或 1")
    @Max(value = 1, message = "状态取值为 0 或 1")
    private Integer status;
}
