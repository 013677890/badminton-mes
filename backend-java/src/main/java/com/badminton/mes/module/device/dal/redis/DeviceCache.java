package com.badminton.mes.module.device.dal.redis;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import tools.jackson.databind.ObjectMapper;

/**
 * 设备接入模块 Redis 旁路缓存。
 *
 * <p>Redis 是弱依赖：读取、写入或删除失败只记录日志，不阻断主业务。
 * 删除操作优先注册到事务提交后执行，避免提交前并发读旧值并回填缓存。
 */
@Component
public class DeviceCache {

    /** 记录 Redis 弱依赖故障；缓存异常降级后仍由数据库加载器完成主流程。 */
    private static final Logger logger = LoggerFactory.getLogger(DeviceCache.class);

    /**
     * 带版本校验的原子回填脚本。
     *
     * <p>仅当数据库加载前观察到的版本仍等于当前版本时，才以毫秒 TTL 写入详情 JSON；
     * 若事务已提交并递增版本，脚本返回 0 且拒绝旧快照回填，避免并发读覆盖最新状态。
     */
    private static final DefaultRedisScript<Long> WRITE_IF_VERSION_UNCHANGED_SCRIPT =
            new DefaultRedisScript<>("""
                    local currentVersion = redis.call('GET', KEYS[2]) or '0'
                    if currentVersion ~= ARGV[1] then
                        return 0
                    end
                    redis.call('PSETEX', KEYS[1], ARGV[2], ARGV[3])
                    return 1
                    """, Long.class);

    /**
     * 详情失效脚本：先递增版本，再删除详情键，并在一个 Lua 调用内保持原子性。
     *
     * <p>即使详情键不存在，版本仍会前进，从而使正在执行的旧值加载无法通过版本校验。
     */
    private static final DefaultRedisScript<Long> INVALIDATE_SCRIPT =
            new DefaultRedisScript<>("""
                    redis.call('INCR', KEYS[2])
                    return redis.call('DEL', KEYS[1])
                    """, Long.class);

    /** Redis 字符串访问入口；连接故障按弱依赖策略捕获并记录。 */
    private final StringRedisTemplate stringRedisTemplate;

    /** 详情对象与缓存 JSON 之间的序列化器。 */
    private final ObjectMapper objectMapper;

    /** 注入 Redis 客户端和 JSON 序列化器，不在构造阶段访问外部缓存。 */
    public DeviceCache(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 按“缓存读取—数据库加载—受版本保护回填”流程获取详情。
     *
     * <p>先读取版本再读取详情，缓存未命中或反序列化失败时调用数据库加载器；回填通过 Lua 同时校验版本
     * 并设置 {@link DeviceRedisKeyConstants#DEVICE_DETAIL_TTL}。Redis 读取失败不阻断数据库查询，
     * 且版本读取失败时主动放弃回填，防止无法确认并发状态时写入旧值。
     *
     * @param resourceName 资源命名空间，用于隔离不同详情类型
     * @param id           详情业务主键
     * @param valueType    JSON 反序列化目标类型
     * @param loader       缓存不可用或未命中时执行的数据库加载器
     * @param <T>          详情对象类型
     * @return 缓存命中的对象，或数据库加载结果
     */
    public <T> T getOrLoadDetail(String resourceName, Long id, Class<T> valueType, Supplier<T> loader) {
        String detailKey = DeviceRedisKeyConstants.detailKey(resourceName, id);
        String versionKey = DeviceRedisKeyConstants.detailVersionKey(resourceName, id);
        String observedVersion = readVersion(versionKey, resourceName, id);
        try {
            String json = stringRedisTemplate.opsForValue().get(detailKey);
            if (StringUtils.hasText(json)) {
                return objectMapper.readValue(json, valueType);
            }
        } catch (RuntimeException exception) {
            logger.warn("[设备接入详情缓存读取失败] resourceName: {}, id: {}, errorMessage: {}",
                    resourceName, id, exception.getMessage());
        }
        T loadedValue = loader.get();
        writeIfVersionUnchanged(detailKey, versionKey, observedVersion, loadedValue, resourceName, id);
        return loadedValue;
    }

    /**
     * 安排单个详情在当前事务成功提交后失效；无活动事务时立即失效。
     *
     * <p>提交后执行可避免数据库尚未提交时就删除缓存，继而被并发查询用旧数据库快照重新回填。
     */
    public void evictDetailAfterCommit(String resourceName, Long id) {
        evictDetailsAfterCommit(resourceName, List.of(id));
    }

    /**
     * 批量安排详情缓存失效。
     *
     * <p>过滤空主键并去重后生成详情键与版本键配对；真正失效遵循事务提交后执行规则，
     * 适用于批量更新后统一清除缓存且避免同一主键重复访问 Redis。
     */
    public void evictDetailsAfterCommit(String resourceName, Collection<Long> ids) {
        List<CacheKeyPair> cacheKeys = ids.stream()
                .filter(id -> id != null)
                .distinct()
                .map(id -> new CacheKeyPair(
                        DeviceRedisKeyConstants.detailKey(resourceName, id),
                        DeviceRedisKeyConstants.detailVersionKey(resourceName, id)))
                .toList();
        evictAfterCommit(cacheKeys);
    }

    /**
     * 读取详情版本；版本键尚未建立时按初始版本 0 处理。
     *
     * <p>Redis 异常时返回 {@code null} 作为“不允许回填”标记，但不影响后续数据库加载。
     */
    private String readVersion(String versionKey, String resourceName, Long id) {
        try {
            String version = stringRedisTemplate.opsForValue().get(versionKey);
            return StringUtils.hasText(version) ? version : "0";
        } catch (RuntimeException exception) {
            logger.warn("[设备接入详情缓存版本读取失败] resourceName: {}, id: {}, errorMessage: {}",
                    resourceName, id, exception.getMessage());
            return null;
        }
    }

    /**
     * 使用 Lua 在版本未变化时原子写入详情和 TTL。
     *
     * <p>观察版本为空表示此前无法确认 Redis 并发状态，此时直接跳过；序列化或写入失败仅记录告警，
     * 保持缓存作为弱依赖，不改变主业务返回的数据库结果。
     */
    private void writeIfVersionUnchanged(String detailKey, String versionKey, String observedVersion,
                                         Object value, String resourceName, Long id) {
        if (observedVersion == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            stringRedisTemplate.execute(WRITE_IF_VERSION_UNCHANGED_SCRIPT,
                    List.of(detailKey, versionKey), observedVersion,
                    Long.toString(DeviceRedisKeyConstants.DEVICE_DETAIL_TTL.toMillis()), json);
        } catch (RuntimeException exception) {
            logger.warn("[设备接入详情缓存写入失败] resourceName: {}, id: {}, errorMessage: {}",
                    resourceName, id, exception.getMessage());
        }
    }

    /**
     * 根据事务上下文选择失效时机。
     *
     * <p>活动事务中注册 {@link TransactionSynchronization#afterCommit()} 回调，仅成功提交后清理；
     * 非事务调用没有提交边界，因而立即执行。复制集合用于冻结回调捕获的键集合。
     */
    private void evictAfterCommit(Collection<CacheKeyPair> keys) {
        List<CacheKeyPair> immutableKeys = List.copyOf(keys);
        if (immutableKeys.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            evict(immutableKeys);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                evict(immutableKeys);
            }
        });
    }

    /**
     * 逐组执行版本递增和详情删除原子脚本。
     *
     * <p>版本先变更可阻止进行中的旧查询回填；Redis 失败只记录错误，不回滚已提交的数据库事务。
     */
    private void evict(Collection<CacheKeyPair> keys) {
        try {
            for (CacheKeyPair key : keys) {
                stringRedisTemplate.execute(INVALIDATE_SCRIPT, List.of(key.detailKey(), key.versionKey()));
            }
        } catch (RuntimeException exception) {
            logger.error("[设备接入详情缓存删除失败] keys: {}, errorMessage: {}",
                    keys, exception.getMessage(), exception);
        }
    }

    /** 同一资源实例的详情键与版本键配对，确保失效脚本不会混用不同主键。 */
    private record CacheKeyPair(String detailKey, String versionKey) {
    }
}
