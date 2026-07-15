package com.badminton.mes.module.equipment.dal.redis;

import java.time.Duration;

/**
 * 设备模块 Redis Key 常量，集中管理 Key 格式与 TTL。
 *
 * <p>详情 Key 遵循 {@code mes:equipment:{resource}:{id}} 命名空间，版本 Key 在同一前缀后追加
 * {@code :version}。资源名隔离类别、制造商、台账、故障、保养和报修数据，业务主键隔离具体记录。
 * 详情值设置有限 TTL 以限制脏数据存活时间；版本 Key 由失效 Lua 单调递增，承担并发回填屏障。
 *
 * @author 角色C
 * @date 2026/07/09
 */
public final class EquipmentRedisKeyConstants {

    /** 详情缓存 Key 模板；两个占位符依次为资源名和业务主键，值保存序列化后的详情 JSON。 */
    public static final String EQUIPMENT_DETAIL = "mes:equipment:%s:%d";

    /** 详情版本 Key 模板；保存单调递增版本号，供 Lua 比较并阻止失效前读请求回填旧值。 */
    public static final String EQUIPMENT_DETAIL_VERSION = "mes:equipment:%s:%d:version";

    /** 详情 JSON 的统一生存期为 30 分钟；到期后按 Cache-Aside 流程重新加载权威数据。 */
    public static final Duration EQUIPMENT_DETAIL_TTL = Duration.ofMinutes(30);

    /** 设备类别详情的资源段，生成 {@code mes:equipment:category:{id}}。 */
    public static final String CATEGORY_RESOURCE = "category";

    /** 设备制造商详情的资源段，避免与相同主键的其他资源发生 Key 冲突。 */
    public static final String MANUFACTURER_RESOURCE = "manufacturer";

    /** 设备台账详情的资源段，用于隔离设备核心主数据缓存。 */
    public static final String LEDGER_RESOURCE = "ledger";

    /** 设备故障原理详情的资源段，下划线属于稳定 Key 协议的一部分。 */
    public static final String FAULT_PRINCIPLE_RESOURCE = "fault_principle";

    /** 设备保养计划详情的资源段，下划线命名与其他多词资源保持一致。 */
    public static final String MAINTENANCE_PLAN_RESOURCE = "maintenance_plan";

    /** 设备保养任务记录详情的资源段，与保养计划缓存分离。 */
    public static final String MAINTENANCE_RECORD_RESOURCE = "maintenance_record";

    /** 设备报修任务详情的资源段，用于生成报修详情及其版本 Key。 */
    public static final String REPAIR_ORDER_RESOURCE = "repair_order";

    /**
     * 按统一模板构造设备模块详情缓存 Key。
     *
     * @param resourceName 资源类型，决定缓存命名空间中的资源段
     * @param id           业务主键，定位该资源的具体记录
     * @return 可保存详情 JSON 的完整缓存 Key
     */
    public static String detailKey(String resourceName, Long id) {
        return String.format(EQUIPMENT_DETAIL, resourceName, id);
    }

    /**
     * 按统一模板构造详情版本 Key。
     *
     * <p>版本 Key 与详情 Key 使用相同资源名和主键，仅追加版本后缀，供读取快照、条件回填及
     * 提交后失效脚本共同使用。
     *
     * @param resourceName 资源类型
     * @param id           业务主键
     * @return 与详情 Key 配套的完整版本 Key
     */
    public static String detailVersionKey(String resourceName, Long id) {
        return String.format(EQUIPMENT_DETAIL_VERSION, resourceName, id);
    }

    /** 纯静态 Key 协议定义类，不允许实例化。 */
    private EquipmentRedisKeyConstants() {
    }
}
