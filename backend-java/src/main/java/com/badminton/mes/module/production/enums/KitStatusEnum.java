package com.badminton.mes.module.production.enums;

import lombok.Getter;

/**
 * 齐套状态枚举，对应 kit_analysis.kit_status 与 prod_work_order.kit_status。
 *
 * <p>工单级状态取各物料行状态的最大值：任一行欠料则工单欠料，
 * 保证"欠料数量大于 0 不能标记齐套"的业务规则。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Getter
public enum KitStatusEnum {

    /** 未分析：工单尚未执行齐套分析(仅工单冗余字段使用) */
    NOT_ANALYZED(0, "未分析"),

    /** 齐套：剩余需求全部可由可用库存覆盖 */
    COMPLETE(1, "齐套"),

    /** 部分齐套：有可用库存但不足以覆盖剩余需求 */
    PARTIAL(2, "部分齐套"),

    /** 欠料：可用库存为 0 */
    SHORTAGE(3, "欠料");

    /** 状态值，与数据库 kit_status 字段取值一致 */
    private final Integer status;

    /** 状态描述 */
    private final String description;

    KitStatusEnum(Integer status, String description) {
        this.status = status;
        this.description = description;
    }
}
