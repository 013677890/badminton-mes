package com.badminton.mes.module.barcode.enums;

import lombok.Getter;

/**
 * 条码持久状态枚举，对应 barcode.barcode_status。
 *
 * <p>持久状态只有未使用/已使用/已作废；"已打印"是打印记录的派生属性，
 * 不进入本状态(2026-07-11 契约差异登记 §3.3.6)。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Getter
public enum BarcodeStatusEnum {

    /** 未使用：已生成尚未被扫码消费，可作废 */
    UNUSED(0, "未使用"),

    /** 已使用：已被现场扫码消费，不可作废 */
    USED(1, "已使用"),

    /** 已作废：未使用状态下人工作废 */
    CANCELLED(2, "已作废");

    /** 状态值，与数据库 barcode_status 字段取值一致 */
    private final Integer status;

    /** 状态描述 */
    private final String description;

    BarcodeStatusEnum(Integer status, String description) {
        this.status = status;
        this.description = description;
    }
}
