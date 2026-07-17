package com.badminton.mes.module.barcode.controller.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 条码应用规则选项查询请求 VO：生成条码时按业务对象过滤可用规则。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodeApplicationRuleOptionReqVO {

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
}
