package com.badminton.mes.module.andon.dal.redis;

import java.time.Duration;

/**
 * 安灯模块 Redis Key 常量，集中管理详情缓存 Key 格式、资源命名与过期时间。
 *
 * <p>详情数据按资源类型和业务主键隔离；与详情 Key 一一对应的版本 Key 不设置为业务真值，
 * 仅用于识别“查询数据库期间发生过失效”的并发窗口。统一资源名可以避免类型、原因、配置和事件
 * 在级联失效时拼接出不一致的 Key。
 */
public final class AndonRedisKeyConstants {

    /** 安灯模块详情缓存 Key 模板，参数依次为资源类型与业务主键 id。 */
    public static final String ANDON_DETAIL = "mes:andon:%s:%d";

    /** 详情缓存版本 Key 模板；每次失效先递增版本，从而阻止并发慢查询回填旧值。 */
    public static final String ANDON_DETAIL_VERSION = "mes:andon:%s:%d:version";

    /** 详情缓存的最长存活时间；缓存只用于读性能优化，过期后仍以数据库重新加载结果为准。 */
    public static final Duration ANDON_DETAIL_TTL = Duration.ofMinutes(30);

    /** 安灯类型详情缓存资源名；类型变更时还需级联失效原因、配置和事件详情。 */
    public static final String TYPE_RESOURCE = "type";

    /** 安灯原因详情缓存资源名；响应中包含所属类型名称，因此受类型修改影响。 */
    public static final String REASON_RESOURCE = "reason";

    /** 安灯配置详情缓存资源名；响应中包含所属类型信息，因此受类型修改影响。 */
    public static final String CONFIGURATION_RESOURCE = "configuration";

    /** 安灯事件详情缓存资源名；详情聚合流程日志、通知记录和类型快照信息。 */
    public static final String EVENT_RESOURCE = "event";

    /**
     * 构造安灯模块详情缓存 Key。
     *
     * @param resourceName 受控资源名，只应使用本类定义的资源常量
     * @param id           业务主键，不承担逻辑删除状态编码
     * @return 可直接交给 Redis 访问的详情 Key
     */
    public static String detailKey(String resourceName, Long id) {
        return String.format(ANDON_DETAIL, resourceName, id);
    }

    /**
     * 构造详情缓存版本 Key。
     *
     * @param resourceName 受控资源名
     * @param id 业务主键
     * @return 与详情 Key 一一对应的版本 Key
     */
    public static String detailVersionKey(String resourceName, Long id) {
        return String.format(ANDON_DETAIL_VERSION, resourceName, id);
    }

    private AndonRedisKeyConstants() {
    }
}
