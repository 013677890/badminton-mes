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
 * <p>注解负责必填、长度、正数及取值范围等单字段校验。类别编码唯一性、父类别是否存在、父子层级
 * 是否自引用或成环等依赖数据库和当前记录的规则，由 Service 在事务内校验。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Data
public class EquipmentCategorySaveReqVO {

    /** 业务唯一的类别编码，创建和修改时均不能为空，最大 32 个字符。 */
    @NotBlank(message = "类别编码不能为空")
    @Size(max = 32, message = "类别编码长度不能超过 32")
    private String categoryCode;

    /** 面向设备建档和查询展示的类别名称，最大 64 个字符。 */
    @NotBlank(message = "类别名称不能为空")
    @Size(max = 64, message = "类别名称长度不能超过 64")
    private String categoryName;

    /** 父类别主键；为空表示顶级类别，非空时必须为正数且不能形成循环层级。 */
    @Positive(message = "父级类别 id 必须为正数")
    private Long parentId;

    /** 同级类别的展示顺序，数值越小越靠前；可空并由 Service 补充默认值 0。 */
    @Min(value = 0, message = "排序号不能为负数")
    @Max(value = 9999, message = "排序号最大值为 9999")
    private Integer sortOrder;

    /** 类别用途或管理约定的补充说明，可空，最大 255 个字符。 */
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;

    /** 启停状态：1 表示启用，0 表示停用；可空并由 Service 补充默认值 1。 */
    @Min(value = 0, message = "状态取值为 0 或 1")
    @Max(value = 1, message = "状态取值为 0 或 1")
    private Integer status;
}
