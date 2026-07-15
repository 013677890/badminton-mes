package com.badminton.mes.module.barcode.enums;

import lombok.Getter;

/**
 * 条码规则组成项类型枚举，对应 barcode_rule_item.item_type。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Getter
public enum BarcodeRuleItemTypeEnum {

    /** 常量：固定字符段，item_value 为常量内容 */
    CONSTANT(1, "常量"),

    /** 日期：按 date_format 格式化生成日期段 */
    DATE(2, "日期"),

    /** 变量：运行时取业务上下文值，item_value 为变量名(产品编码/产线编码) */
    VARIABLE(3, "变量"),

    /** 流水号：按规则流水位数左补零，一个规则必须且只能包含一个 */
    SERIAL(4, "流水号");

    /** 类型值，与数据库 item_type 字段取值一致 */
    private final Integer type;

    /** 类型描述 */
    private final String description;

    BarcodeRuleItemTypeEnum(Integer type, String description) {
        this.type = type;
        this.description = description;
    }

    /**
     * 按类型值解析枚举。
     *
     * @param type 类型值
     * @return 对应枚举；无匹配返回 null
     */
    public static BarcodeRuleItemTypeEnum of(Integer type) {
        for (BarcodeRuleItemTypeEnum value : values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        return null;
    }
}
