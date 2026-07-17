package com.badminton.mes.module.barcode.enums;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import lombok.Getter;

/**
 * 条码流水号重置周期枚举，对应 barcode_rule.serial_reset_cycle。
 *
 * <p>周期同时决定流水作用域的日期段：按日 yyyyMMdd、按月 yyyyMM、
 * 不重置固定常量，保证同一规则同一周期内流水连续且周期切换后从头计数
 * (M1 待确认事项①已按基线契约冻结，见 2026-07-12-B组M1条码结构迁移)。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Getter
public enum BarcodeSerialResetCycleEnum {

    /** 按日重置：作用域日期段 yyyyMMdd */
    DAILY(1, "按日", "yyyyMMdd"),

    /** 按月重置：作用域日期段 yyyyMM */
    MONTHLY(2, "按月", "yyyyMM"),

    /** 不重置：作用域日期段固定为 ALL */
    NEVER(3, "不重置", null);

    /** 不重置周期的固定作用域日期段 */
    public static final String NEVER_SCOPE_SEGMENT = "ALL";

    /** 周期值，与数据库 serial_reset_cycle 字段取值一致 */
    private final Integer cycle;

    /** 周期描述 */
    private final String description;

    /** 作用域日期段格式，不重置时为 null */
    private final String scopeDateFormat;

    BarcodeSerialResetCycleEnum(Integer cycle, String description, String scopeDateFormat) {
        this.cycle = cycle;
        this.description = description;
        this.scopeDateFormat = scopeDateFormat;
    }

    /**
     * 按周期值解析枚举。
     *
     * @param cycle 周期值
     * @return 对应枚举；无匹配返回 null
     */
    public static BarcodeSerialResetCycleEnum of(Integer cycle) {
        for (BarcodeSerialResetCycleEnum value : values()) {
            if (value.cycle.equals(cycle)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 计算业务日期在本周期下的流水作用域日期段。
     *
     * @param date 业务日期
     * @return 作用域日期段：按日 yyyyMMdd、按月 yyyyMM、不重置 ALL
     */
    public String scopeSegment(LocalDate date) {
        if (this == NEVER) {
            return NEVER_SCOPE_SEGMENT;
        }
        return date.format(DateTimeFormatter.ofPattern(scopeDateFormat));
    }
}
