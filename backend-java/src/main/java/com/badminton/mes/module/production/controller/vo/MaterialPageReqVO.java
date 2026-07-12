package com.badminton.mes.module.production.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 物料分页查询请求。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MaterialPageReqVO extends PageParam {
    /** 物料编码前缀 */
    @Size(max = 32, message = "物料编码长度不能超过 32")
    private String materialCode;
    /** 物料名称前缀 */
    @Size(max = 128, message = "物料名称长度不能超过 128")
    private String materialName;
    /** 物料类型 */
    private Integer materialType;
    /** 计量单位主键 */
    @Positive(message = "计量单位 id 必须为正数")
    private Long unitId;
    /** 是否关键物料 */
    private Boolean keyMaterial;
    /** 启停状态 */
    @Min(value = 0, message = "物料状态不合法")
    @Max(value = 1, message = "物料状态不合法")
    private Integer status;
}
