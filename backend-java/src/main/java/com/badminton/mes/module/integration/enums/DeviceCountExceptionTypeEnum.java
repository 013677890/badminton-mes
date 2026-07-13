package com.badminton.mes.module.integration.enums;

import lombok.Getter;

/**
 * 设备计数异常类型，对应异常池 exception_type 字段。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Getter
public enum DeviceCountExceptionTypeEnum {

    /** 派工单号不存在 */
    DISPATCH_NOT_FOUND("DISPATCH_NOT_FOUND", "派工单不存在"),

    /** 派工单状态不是已下发或执行中 */
    DISPATCH_STATUS_INVALID("DISPATCH_STATUS_INVALID", "派工单状态不允许计数"),

    /** 工序编码不存在 */
    PROCESS_NOT_FOUND("PROCESS_NOT_FOUND", "工序不存在"),

    /** 累计计数值小于或等于零 */
    COUNT_NON_POSITIVE("COUNT_NON_POSITIVE", "计数值必须大于零"),

    /** 累计计数值小于最近一次记录 */
    COUNT_ROLLBACK("COUNT_ROLLBACK", "计数值发生倒退"),

    /** 设备未维护有效绑定 */
    EQUIPMENT_NOT_BOUND("EQUIPMENT_NOT_BOUND", "设备不存在或未启用"),

    /** 设备未绑定当前派工产线 */
    LINE_MISMATCH("LINE_MISMATCH", "设备与派工产线不匹配"),

    /** 设备未绑定当前工序 */
    PROCESS_MISMATCH("PROCESS_MISMATCH", "设备与工序不匹配"),

    /** 单次计数增量超过配置阈值 */
    COUNT_JUMP("COUNT_JUMP", "计数增量异常跳变");

    /** 数据库存储值 */
    private final String value;

    /** 异常描述 */
    private final String description;

    DeviceCountExceptionTypeEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
