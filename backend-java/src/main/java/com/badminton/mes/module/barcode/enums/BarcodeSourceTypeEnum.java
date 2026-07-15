package com.badminton.mes.module.barcode.enums;

import lombok.Getter;

/**
 * 条码来源枚举，对应 barcode_apply_rule.source_type 与 barcode.source_type。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Getter
public enum BarcodeSourceTypeEnum {

    /** 规则生成：按条码规则组成项拼接生成 */
    RULE_GENERATE(1, "规则生成"),

    /** 传入值生成：调用方直接提供条码值 */
    INPUT_VALUE(2, "传入值生成"),

    /** 外部导入：批量导入外部系统条码 */
    EXTERNAL_IMPORT(3, "外部导入");

    /** 来源值，与数据库 source_type 字段取值一致 */
    private final Integer type;

    /** 来源描述 */
    private final String description;

    BarcodeSourceTypeEnum(Integer type, String description) {
        this.type = type;
        this.description = description;
    }
}
