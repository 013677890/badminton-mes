package com.badminton.mes.module.production.enums;

import lombok.Getter;

/**
 * 欠料处理方式枚举，对应 prod_kit_shortage_handle.handle_type。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Getter
public enum ShortageHandleTypeEnum {

    /** 催采购：联系供应商加急交付 */
    URGE_PURCHASE(1, "催采购"),

    /** 调拨：从其他仓库/车间调剂 */
    TRANSFER(2, "调拨"),

    /** 代用料：使用经批准的替代物料 */
    SUBSTITUTE(3, "代用料"),

    /** 调整排产：推迟或拆分工单排产 */
    RESCHEDULE(4, "调整排产");

    /** 类型值，与数据库 handle_type 字段取值一致 */
    private final Integer type;

    /** 类型描述 */
    private final String description;

    ShortageHandleTypeEnum(Integer type, String description) {
        this.type = type;
        this.description = description;
    }
}
