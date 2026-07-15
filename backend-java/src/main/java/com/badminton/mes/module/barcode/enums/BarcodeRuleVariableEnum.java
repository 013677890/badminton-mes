package com.badminton.mes.module.barcode.enums;

import lombok.Getter;

/**
 * 条码规则变量枚举，item_type = 变量 时 item_value 的合法取值。
 *
 * <p>基线契约限定变量为产品编码/产线编码(barcode_rule_item.item_type 注释)；
 * 新增变量必须先扩展本枚举并同步接口文档。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Getter
public enum BarcodeRuleVariableEnum {

    /** 产品编码，生成时取业务上下文产品档案编码 */
    PRODUCT_CODE("productCode", "产品编码"),

    /** 产线编码，生成时取业务上下文产线编码 */
    LINE_CODE("lineCode", "产线编码");

    /** 变量名，与 item_value 存储值一致 */
    private final String variable;

    /** 变量描述 */
    private final String description;

    BarcodeRuleVariableEnum(String variable, String description) {
        this.variable = variable;
        this.description = description;
    }

    /**
     * 按变量名解析枚举。
     *
     * @param variable 变量名
     * @return 对应枚举；无匹配返回 null
     */
    public static BarcodeRuleVariableEnum of(String variable) {
        for (BarcodeRuleVariableEnum value : values()) {
            if (value.variable.equals(variable)) {
                return value;
            }
        }
        return null;
    }
}
