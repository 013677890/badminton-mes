package com.badminton.mes.module.barcode.enums;

import lombok.Getter;

/**
 * 条码业务对象类型枚举，对应 barcode_apply_rule.object_type。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Getter
public enum BarcodeApplyObjectTypeEnum {

    /** 产品：product_id 必填 */
    PRODUCT(1, "产品"),

    /** 物料：material_id 必填(材料码) */
    MATERIAL(2, "物料");

    /** 类型值，与数据库 object_type 字段取值一致 */
    private final Integer type;

    /** 类型描述 */
    private final String description;

    BarcodeApplyObjectTypeEnum(Integer type, String description) {
        this.type = type;
        this.description = description;
    }
}
