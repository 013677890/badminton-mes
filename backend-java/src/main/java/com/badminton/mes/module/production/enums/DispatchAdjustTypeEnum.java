package com.badminton.mes.module.production.enums;

import lombok.Getter;

/**
 * 派工单调整日志类型枚举，对应 prod_dispatch_adjust_log.adjust_type。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Getter
public enum DispatchAdjustTypeEnum {

    /** 系统建议：采纳排产建议创建 */
    SUGGEST_CREATE(1, "系统建议"),

    /** 人工创建 */
    MANUAL_CREATE(2, "人工创建"),

    /** 调整：修改产线/班次/日期/数量 */
    ADJUST(3, "调整"),

    /** 审核 */
    AUDIT(4, "审核"),

    /** 下发 */
    ISSUE(5, "下发"),

    /** 取消 */
    CANCEL(6, "取消");

    /** 类型值，与数据库 adjust_type 字段取值一致 */
    private final Integer type;

    /** 类型描述 */
    private final String description;

    DispatchAdjustTypeEnum(Integer type, String description) {
        this.type = type;
        this.description = description;
    }
}
