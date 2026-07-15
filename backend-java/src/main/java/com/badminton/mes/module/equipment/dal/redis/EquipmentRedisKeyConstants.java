package com.badminton.mes.module.equipment.dal.redis;

import java.time.Duration;

/**
 * 设备模块 Redis Key 常量，集中管理 Key 格式与 TTL。
 *
 * @author 角色C
 * @date 2026/07/09
 */
public final class EquipmentRedisKeyConstants {

    /** 设备模块详情缓存 Key 模板，参数为资源类型与业务主键 id。 */
    public static final String EQUIPMENT_DETAIL = "mes:equipment:%s:%d";

    /** 设备模块详情缓存版本 Key 模板，用于阻止并发查询回填旧值。 */
    public static final String EQUIPMENT_DETAIL_VERSION = "mes:equipment:%s:%d:version";

    /** 设备模块详情缓存 TTL。 */
    public static final Duration EQUIPMENT_DETAIL_TTL = Duration.ofMinutes(30);

    /** 设备类别详情缓存资源名。 */
    public static final String CATEGORY_RESOURCE = "category";

    /** 设备制造商详情缓存资源名。 */
    public static final String MANUFACTURER_RESOURCE = "manufacturer";

    /** 设备台账详情缓存资源名。 */
    public static final String LEDGER_RESOURCE = "ledger";

    /** 设备故障原理详情缓存资源名。 */
    public static final String FAULT_PRINCIPLE_RESOURCE = "fault_principle";

    /** 设备保养计划详情缓存资源名。 */
    public static final String MAINTENANCE_PLAN_RESOURCE = "maintenance_plan";

    /** 设备保养记录详情缓存资源名。 */
    public static final String MAINTENANCE_RECORD_RESOURCE = "maintenance_record";

    /** 设备报修任务详情缓存资源名。 */
    public static final String REPAIR_ORDER_RESOURCE = "repair_order";

    /**
     * 构造设备模块详情缓存 Key。
     *
     * @param resourceName 资源类型
     * @param id           业务主键
     * @return 缓存 Key
     */
    public static String detailKey(String resourceName, Long id) {
        return String.format(EQUIPMENT_DETAIL, resourceName, id);
    }

    /**
     * 构造设备模块详情缓存版本 Key。
     *
     * @param resourceName 资源类型
     * @param id           业务主键
     * @return 缓存版本 Key
     */
    public static String detailVersionKey(String resourceName, Long id) {
        return String.format(EQUIPMENT_DETAIL_VERSION, resourceName, id);
    }

    private EquipmentRedisKeyConstants() {
    }
}
