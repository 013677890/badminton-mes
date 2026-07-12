package com.badminton.mes.module.production.enums;

import java.util.Arrays;

import lombok.Getter;

/** 产品类型。 */
@Getter
public enum ProductTypeEnum {
    /** 成品 */
    FINISHED(1),
    /** 半成品 */
    SEMI_FINISHED(2);

    private final Integer type;

    ProductTypeEnum(Integer type) {
        this.type = type;
    }

    /** 判断类型值是否合法。 */
    public static boolean contains(Integer type) {
        return Arrays.stream(values()).anyMatch(item -> item.type.equals(type));
    }
}
