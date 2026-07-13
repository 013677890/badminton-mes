package com.badminton.mes.module.barcode.enums;

import lombok.Getter;

/**
 * 条码模板字段类型枚举，对应 barcode_template_field.field_type。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Getter
public enum BarcodeTemplateFieldTypeEnum {

    /** 文本：普通文字内容 */
    TEXT(1, "文本"),

    /** 条码：一维条码承载条码值 */
    BARCODE(2, "条码"),

    /** 二维码：二维码承载条码值 */
    QR_CODE(3, "二维码");

    /** 类型值，与数据库 field_type 字段取值一致 */
    private final Integer type;

    /** 类型描述 */
    private final String description;

    BarcodeTemplateFieldTypeEnum(Integer type, String description) {
        this.type = type;
        this.description = description;
    }

    /**
     * 判断字段类型是否承载条码值(条码或二维码)。
     *
     * @param type 类型值
     * @return true 承载条码值
     */
    public static boolean carriesBarcodeValue(Integer type) {
        return BARCODE.type.equals(type) || QR_CODE.type.equals(type);
    }
}
