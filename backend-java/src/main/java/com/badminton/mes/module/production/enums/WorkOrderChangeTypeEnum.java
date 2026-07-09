package com.badminton.mes.module.production.enums;

import lombok.Getter;

/**
 * 工单状态日志变更类型枚举，对应 prod_work_order_status_log.change_type 字段。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
@Getter
public enum WorkOrderChangeTypeEnum {

    /** 状态流转：下达、暂停、恢复、完工、关闭、作废 */
    STATUS_TRANSITION(1, "状态流转"),

    /** 计划变更：下达后修改计划数量或交期 */
    PLAN_CHANGE(2, "计划变更");

    /** 类型值，与数据库 change_type 字段取值一致 */
    private final Integer type;

    /** 类型描述 */
    private final String description;

    WorkOrderChangeTypeEnum(Integer type, String description) {
        this.type = type;
        this.description = description;
    }
}
