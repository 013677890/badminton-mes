package com.badminton.mes.module.barcode.dal.redis;

import java.time.Duration;

/**
 * 条码模块 Redis Key 常量，集中管理 Key 格式与 TTL。
 *
 * <p>流水 Key 按"规则 + 作用域"维度隔离；TTL 覆盖两个重置周期，
 * 周期切换后计数 Key 自动清理，不重置周期不设置过期。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public final class BarcodeRedisKeyConstants {

    /** 条码流水自增 Key 模板，参数为规则 id 与流水作用域 */
    public static final String BARCODE_SERIAL = "mes:barcode:serial:%d:%s";

    /** 按日重置周期的流水 Key TTL，跨天后计数 Key 自动清理 */
    public static final Duration SERIAL_DAILY_TTL = Duration.ofDays(2);

    /** 按月重置周期的流水 Key TTL，跨月后计数 Key 自动清理 */
    public static final Duration SERIAL_MONTHLY_TTL = Duration.ofDays(62);

    /**
     * 构造条码流水自增 Key。
     *
     * @param ruleId 条码规则主键
     * @param scope  流水作用域(周期日期段 + 对象编码)
     * @return 流水计数 Key
     */
    public static String barcodeSerialKey(Long ruleId, String scope) {
        return String.format(BARCODE_SERIAL, ruleId, scope);
    }

    private BarcodeRedisKeyConstants() {
    }
}
