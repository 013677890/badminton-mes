package com.badminton.mes.module.device.dal.redis;

import java.time.Duration;

/**
 * 设备接入模块 Redis Key 常量，集中管理详情缓存 Key 格式与 TTL。
 */
public final class DeviceRedisKeyConstants {

    /** 设备接入模块详情缓存 Key 模板，参数为资源类型与业务主键 id。 */
    public static final String DEVICE_DETAIL = "mes:device:%s:%d";

    /** 设备接入模块详情缓存版本 Key 模板，用于阻止并发查询回填旧值。 */
    public static final String DEVICE_DETAIL_VERSION = "mes:device:%s:%d:version";

    /** 设备接入模块详情缓存 TTL。 */
    public static final Duration DEVICE_DETAIL_TTL = Duration.ofMinutes(30);

    /** 设备接入配置详情缓存资源名。 */
    public static final String ACCESS_CONFIG_RESOURCE = "access_config";

    /** 设备联调记录详情缓存资源名。 */
    public static final String COMMISSIONING_RECORD_RESOURCE = "commissioning_record";

    /** 设备计数记录详情缓存资源名。 */
    public static final String COUNT_RECORD_RESOURCE = "count_record";

    /** 设备计数异常详情缓存资源名。 */
    public static final String COUNT_EXCEPTION_RESOURCE = "count_exception";

    /**
     * 构造设备接入模块详情缓存 Key。
     *
     * @param resourceName 资源类型
     * @param id           业务主键
     * @return 缓存 Key
     */
    public static String detailKey(String resourceName, Long id) {
        return String.format(DEVICE_DETAIL, resourceName, id);
    }

    /** 构造设备接入模块详情缓存版本 Key。 */
    public static String detailVersionKey(String resourceName, Long id) {
        return String.format(DEVICE_DETAIL_VERSION, resourceName, id);
    }

    private DeviceRedisKeyConstants() {
    }
}
