package com.badminton.mes.module.andon.dal.redis;

import java.time.Duration;

/**
 * 安灯模块 Redis Key 常量，集中管理详情缓存 Key 格式与 TTL。
 */
public final class AndonRedisKeyConstants {

    /** 安灯模块详情缓存 Key 模板，参数为资源类型与业务主键 id。 */
    public static final String ANDON_DETAIL = "mes:andon:%s:%d";

    /** 安灯模块详情缓存版本 Key 模板，用于阻止并发查询回填旧值。 */
    public static final String ANDON_DETAIL_VERSION = "mes:andon:%s:%d:version";

    /** 安灯模块详情缓存 TTL。 */
    public static final Duration ANDON_DETAIL_TTL = Duration.ofMinutes(30);

    /** 安灯类型详情缓存资源名。 */
    public static final String TYPE_RESOURCE = "type";

    /** 安灯原因详情缓存资源名。 */
    public static final String REASON_RESOURCE = "reason";

    /** 安灯配置详情缓存资源名。 */
    public static final String CONFIGURATION_RESOURCE = "configuration";

    /** 安灯事件详情缓存资源名。 */
    public static final String EVENT_RESOURCE = "event";

    /**
     * 构造安灯模块详情缓存 Key。
     *
     * @param resourceName 资源类型
     * @param id           业务主键
     * @return 缓存 Key
     */
    public static String detailKey(String resourceName, Long id) {
        return String.format(ANDON_DETAIL, resourceName, id);
    }

    /** 构造安灯模块详情缓存版本 Key。 */
    public static String detailVersionKey(String resourceName, Long id) {
        return String.format(ANDON_DETAIL_VERSION, resourceName, id);
    }

    private AndonRedisKeyConstants() {
    }
}
