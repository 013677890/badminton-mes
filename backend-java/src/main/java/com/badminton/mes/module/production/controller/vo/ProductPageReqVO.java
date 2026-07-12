package com.badminton.mes.module.production.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 产品分页查询请求。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductPageReqVO extends PageParam {
    /** 产品编码前缀 */
    @Size(max = 32, message = "产品编码长度不能超过 32")
    private String productCode;
    /** 产品名称前缀 */
    @Size(max = 128, message = "产品名称长度不能超过 128")
    private String productName;
    /** 产品类型 */
    private Integer productType;
    /** 计量单位主键 */
    @Positive(message = "计量单位 id 必须为正数")
    private Long unitId;
    /** 状态 */
    @Min(value = 0, message = "产品状态不合法")
    @Max(value = 1, message = "产品状态不合法")
    private Integer status;
}
