package com.badminton.mes.module.integration.enums;

import lombok.Getter;

/**
 * 外部集成接口类型。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
@Getter
public enum IntegrationInterfaceTypeEnum {

    /** 计量单位写入 */
    UNIT_WRITE("UNIT_WRITE"),

    /** 生产工单写入 */
    WORK_ORDER_WRITE("WORK_ORDER_WRITE"),

    /** 生产任务单（派工单）写入 */
    DISPATCH_ORDER_WRITE("DISPATCH_ORDER_WRITE"),

    /** ERP 生产任务单同步 */
    ERP_TASK_SYNC("ERP_TASK_SYNC"),

    /** ERP 工艺数据同步 */
    ERP_CRAFT_SYNC("ERP_CRAFT_SYNC"),

    /** 设备累计计数写入 */
    DEVICE_COUNT_WRITE("DEVICE_COUNT_WRITE"),

    /** 生产完工单读取 */
    COMPLETION_ORDER_READ("COMPLETION_ORDER_READ");

    /** 数据库存储值 */
    private final String value;

    IntegrationInterfaceTypeEnum(String value) {
        this.value = value;
    }
}
