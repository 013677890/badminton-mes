package com.badminton.mes.module.production.enums;

import lombok.Getter;

/**
 * 欠料处理状态枚举，对应 prod_kit_shortage_handle.handle_status。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Getter
public enum ShortageHandleStatusEnum {

    /** 处理中：措施已登记，欠料尚未消除 */
    PROCESSING(0, "处理中"),

    /** 已解决：欠料已消除或措施已闭环 */
    RESOLVED(1, "已解决");

    /** 状态值，与数据库 handle_status 字段取值一致 */
    private final Integer status;

    /** 状态描述 */
    private final String description;

    ShortageHandleStatusEnum(Integer status, String description) {
        this.status = status;
        this.description = description;
    }
}
