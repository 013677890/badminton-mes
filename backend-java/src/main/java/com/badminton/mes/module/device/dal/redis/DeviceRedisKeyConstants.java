package com.badminton.mes.module.device.dal.redis;

import java.time.Duration;

/**
 * 设备接入模块 Redis Key 规则。
 *
 * <p>集中管理不同详情资源的命名空间、版本键和过期时间，避免业务服务自行拼接导致缓存冲突。
 * 每个详情键都有对应版本键：事务提交后的失效操作先递增版本，再删除详情；并发查询只有在观察版本未变化时
 * 才能回填，从而阻止数据库旧快照覆盖更新后的缓存状态。
 */
public final class DeviceRedisKeyConstants {

    /** 详情缓存 Key 模板，参数依次为受控资源类型和业务主键。 */
    public static final String DEVICE_DETAIL = "mes:device:%s:%d";

    /** 详情版本 Key 模板，用于判断读取期间是否发生过提交后失效。 */
    public static final String DEVICE_DETAIL_VERSION = "mes:device:%s:%d:version";

    /** 详情缓存生存时间；过期仅影响性能，不影响数据库作为最终事实来源。 */
    public static final Duration DEVICE_DETAIL_TTL = Duration.ofMinutes(30);

    /** 设备接入配置详情的资源命名空间。 */
    public static final String ACCESS_CONFIG_RESOURCE = "access_config";

    /** 设备联调记录详情的资源命名空间。 */
    public static final String COMMISSIONING_RECORD_RESOURCE = "commissioning_record";

    /** 设备计数记录详情的资源命名空间。 */
    public static final String COUNT_RECORD_RESOURCE = "count_record";

    /** 设备计数异常详情的资源命名空间。 */
    public static final String COUNT_EXCEPTION_RESOURCE = "count_exception";

    /**
     * 构造指定资源的详情缓存 Key。
     *
     * @param resourceName 受控资源类型，调用方应使用本类声明的资源名常量
     * @param id           资源业务主键
     * @return 可直接访问详情 JSON 的 Redis Key
     */
    public static String detailKey(String resourceName, Long id) {
        return String.format(DEVICE_DETAIL, resourceName, id);
    }

    /**
     * 构造指定资源的详情版本 Key。
     *
     * @param resourceName 受控资源类型
     * @param id           资源业务主键
     * @return 用于并发回填校验的 Redis 版本 Key
     */
    public static String detailVersionKey(String resourceName, Long id) {
        return String.format(DEVICE_DETAIL_VERSION, resourceName, id);
    }

    /** 工具类不允许实例化。 */
    private DeviceRedisKeyConstants() {
    }
}
