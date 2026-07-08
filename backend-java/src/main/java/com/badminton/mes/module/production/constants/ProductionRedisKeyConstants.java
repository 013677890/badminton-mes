package com.badminton.mes.module.production.constants;

import java.time.Duration;

/**
 * 生产订单模块 Redis Key 常量，集中管理 Key 格式与 TTL，避免魔法字符串散落。
 *
 * <p>Key 命名遵循 {@code mes:<module>:<resource>:<id>} 模式，格式稳定、可搜索。
 *
 * @author 张竹灏
 * @date 2026/07/07
 */
public final class ProductionRedisKeyConstants {

    /** 工单详情缓存 Key 模板，参数为工单主键 id */
    public static final String WORK_ORDER_DETAIL = "mes:production:work_order:%d";

    /** 工单详情缓存 TTL；写操作删除缓存失败时也由 TTL 兜底过期 */
    public static final Duration WORK_ORDER_DETAIL_TTL = Duration.ofMinutes(30);

    /** 工单号当日流水自增 Key 模板，参数为 yyyyMMdd 日期串 */
    public static final String WORK_ORDER_SERIAL = "mes:production:work_order_serial:%s";

    /** 工单号流水 Key TTL，跨天后计数 Key 自动清理 */
    public static final Duration WORK_ORDER_SERIAL_TTL = Duration.ofDays(2);

    /**
     * 构造工单详情缓存 Key。
     *
     * @param id 工单主键
     * @return 缓存 Key
     */
    public static String workOrderDetailKey(Long id) {
        return String.format(WORK_ORDER_DETAIL, id);
    }

    /**
     * 构造工单号当日流水 Key。
     *
     * @param date yyyyMMdd 格式日期串
     * @return 流水计数 Key
     */
    public static String workOrderSerialKey(String date) {
        return String.format(WORK_ORDER_SERIAL, date);
    }

    private ProductionRedisKeyConstants() {
    }
}
