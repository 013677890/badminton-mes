package com.badminton.mes.module.production.enums;

import java.util.Arrays;

import lombok.Getter;

/** 羽毛球生产物料类型。 */
@Getter
public enum MaterialTypeEnum {
    /** 球头 */
    CORK_HEAD(1),
    /** 羽片 */
    FEATHER(2),
    /** 胶水 */
    GLUE(3),
    /** 线圈 */
    THREAD(4),
    /** 包装材料 */
    PACKAGING(5),
    /** 其他 */
    OTHER(9);

    private final Integer type;

    MaterialTypeEnum(Integer type) {
        this.type = type;
    }

    /** 判断类型值是否合法。 */
    public static boolean contains(Integer type) {
        return Arrays.stream(values()).anyMatch(item -> item.type.equals(type));
    }
}
