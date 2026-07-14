package com.badminton.mes.module.production.enums;

import lombok.Getter;

/**
 * BOM 状态枚举，对应 base_bom.bom_status 字段。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
@Getter
public enum BomStatusEnum {

    /** 草稿：不可用于工单下达 */
    DRAFT(0, "草稿"),

    /** 生效：可用于工单下达与物料需求计算 */
    EFFECTIVE(1, "生效"),

    /** 停用：历史版本，不可再引用 */
    DISABLED(2, "停用");

    /** 状态值，与数据库 bom_status 字段取值一致 */
    private final Integer status;

    /** 状态描述 */
    private final String description;

    BomStatusEnum(Integer status, String description) {
        this.status = status;
        this.description = description;
    }

    /**
     * 判断状态值是否属于 BOM 状态枚举。
     *
     * @param status 状态值
     * @return true 表示合法
     */
    public static boolean contains(Integer status) {
        for (BomStatusEnum value : values()) {
            if (value.status.equals(status)) {
                return true;
            }
        }
        return false;
    }
}
