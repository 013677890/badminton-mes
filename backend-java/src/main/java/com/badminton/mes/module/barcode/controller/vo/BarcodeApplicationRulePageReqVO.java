package com.badminton.mes.module.barcode.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 条码应用规则分页查询请求 VO，继承 {@link PageParam} 获得分页参数与入参保护。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BarcodeApplicationRulePageReqVO extends PageParam {

    /** 对象类型：1 产品 2 物料，可空 */
    @Min(value = 1, message = "对象类型取值为 1-2")
    @Max(value = 2, message = "对象类型取值为 1-2")
    private Integer objectType;

    /** 产品 id，可空 */
    @Positive(message = "产品 id 必须为正数")
    private Long productId;

    /** 物料 id，可空 */
    @Positive(message = "物料 id 必须为正数")
    private Long materialId;

    /** 条码类型 id，可空 */
    @Positive(message = "条码类型 id 必须为正数")
    private Long barcodeTypeId;

    /** 条码来源：1 规则生成 2 传入值生成 3 外部导入，可空 */
    @Min(value = 1, message = "条码来源取值为 1-3")
    @Max(value = 3, message = "条码来源取值为 1-3")
    private Integer sourceType;

    /** 状态：1 启用 0 停用，可空 */
    @Min(value = 0, message = "状态取值为 0-1")
    @Max(value = 1, message = "状态取值为 0-1")
    private Integer status;
}
