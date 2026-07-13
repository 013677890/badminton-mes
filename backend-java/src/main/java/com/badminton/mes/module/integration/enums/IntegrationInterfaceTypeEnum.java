package com.badminton.mes.module.integration.enums;

import lombok.Getter;

/**
 * 外部写入接口类型。
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
    DISPATCH_ORDER_WRITE("DISPATCH_ORDER_WRITE");

    /** 数据库存储值 */
    private final String value;

    IntegrationInterfaceTypeEnum(String value) {
        this.value = value;
    }
}
