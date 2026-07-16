package com.badminton.mes.module.quality.dal.redis;

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
 * 质量模块 Redis 旁路缓存。
 *
 * <p>详情采用 Cache-Aside 模式并按 {@code resourceName + id} 隔离，值统一序列化为 JSON，TTL 由
 * {@link QualityRedisKeyConstants#QUALITY_DETAIL_TTL} 控制。Redis 是弱依赖：读取、序列化、写入或删除
 * 失败只记录日志，数据库加载与主业务事务仍可继续。
 *
 * <p>每个详情 Key 配套一个不设 TTL 的版本 Key。缓存未命中时先读取版本，再加载数据库，并通过 Lua
 * 原子比较版本后回填；事务提交后的失效操作则以另一段 Lua 先递增版本再删除详情。即使并发读已拿到旧数据库
 * 值，只要更新事务完成失效，其回填也会因版本变化被拒绝，避免旧值复活。
 */
@Component
public class QualityCache {

    private static final Logger logger = LoggerFactory.getLogger(QualityCache.class);

    /** 仅在版本仍等于查询开始时观察值时写入带 TTL 的详情，原子封闭“比较后再写”的竞争窗口。 */
    private static final DefaultRedisScript<Long> WRITE_IF_VERSION_UNCHANGED_SCRIPT =
            new DefaultRedisScript<>("""
                    local currentVersion = redis.call('GET', KEYS[2]) or '0'
                    if currentVersion ~= ARGV[1] then
                        return 0
                    end
                    redis.call('PSETEX', KEYS[1], ARGV[2], ARGV[3])
                    return 1
                    """, Long.class);

    /** 原子递增版本并删除详情；版本先推进，使并发中的旧值加载无法在删除后重新写回。 */
    private static final DefaultRedisScript<Long> INVALIDATE_SCRIPT =
            new DefaultRedisScript<>("""
                    redis.call('INCR', KEYS[2])
                    return redis.call('DEL', KEYS[1])
                    """, Long.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public QualityCache(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 读取详情缓存，未命中或 Redis 异常时回源，并尝试按观察到的版本安全回填。
     *
     * <p>{@code loader} 是权威数据来源；版本读取失败时仍返回其结果，但主动跳过回填，避免缺少并发校验
     * 的旧值进入缓存。成功回填的详情将在统一 TTL 后自动过期。
     */
    public <T> T getOrLoadDetail(String resourceName, Long id, Class<T> valueType, Supplier<T> loader) {
        String detailKey = QualityRedisKeyConstants.detailKey(resourceName, id);
        String versionKey = QualityRedisKeyConstants.detailVersionKey(resourceName, id);
        String observedVersion = readVersion(versionKey, resourceName, id);
        try {
            String json = stringRedisTemplate.opsForValue().get(detailKey);
            if (StringUtils.hasText(json)) {
                return objectMapper.readValue(json, valueType);
            }
        } catch (RuntimeException exception) {
            logger.warn("[质量详情缓存读取失败] resourceName: {}, id: {}, errorMessage: {}",
                    resourceName, id, exception.getMessage());
        }
        T loadedValue = loader.get();
        writeIfVersionUnchanged(detailKey, versionKey, observedVersion, loadedValue, resourceName, id);
        return loadedValue;
    }

    /** 在当前事务成功提交后失效一个详情；无活动事务时立即失效。 */
    public void evictDetailAfterCommit(String resourceName, Long id) {
        evictDetailsAfterCommit(resourceName, List.of(id));
    }

    /**
     * 在事务提交后批量失效同类详情，自动忽略空主键并去重。
     *
     * <p>该入口同时承载级联缓存失效：分类变更可清理其项目详情，项目变更可清理引用它的方案详情。
     */
    public void evictDetailsAfterCommit(String resourceName, Collection<Long> ids) {
        List<CacheKeyPair> cacheKeys = ids.stream()
                .filter(id -> id != null)
                .distinct()
                .map(id -> new CacheKeyPair(
                        QualityRedisKeyConstants.detailKey(resourceName, id),
                        QualityRedisKeyConstants.detailVersionKey(resourceName, id)))
                .toList();
        evictAfterCommit(cacheKeys);
    }

    /** 读取详情版本；Key 尚不存在时以初始版本 0 参与比较，Redis 异常则返回 null 禁止回填。 */
    private String readVersion(String versionKey, String resourceName, Long id) {
        try {
            String version = stringRedisTemplate.opsForValue().get(versionKey);
            return StringUtils.hasText(version) ? version : "0";
        } catch (RuntimeException exception) {
            logger.warn("[质量详情缓存版本读取失败] resourceName: {}, id: {}, errorMessage: {}",
                    resourceName, id, exception.getMessage());
            return null;
        }
    }

    /** 序列化回源结果，并以 Lua 原子完成版本比较和带 TTL 写入。 */
    private void writeIfVersionUnchanged(String detailKey, String versionKey, String observedVersion,
                                         Object value, String resourceName, Long id) {
        if (observedVersion == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            stringRedisTemplate.execute(WRITE_IF_VERSION_UNCHANGED_SCRIPT,
                    List.of(detailKey, versionKey), observedVersion,
                    Long.toString(QualityRedisKeyConstants.QUALITY_DETAIL_TTL.toMillis()), json);
        } catch (RuntimeException exception) {
            logger.warn("[质量详情缓存写入失败] resourceName: {}, id: {}, errorMessage: {}",
                    resourceName, id, exception.getMessage());
        }
    }

    /** 将失效动作挂到事务提交回调，回滚时不删除仍与数据库一致的原缓存。 */
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

    /** 执行版本推进和详情删除；Redis 故障仅降级为日志，不反向破坏已提交的数据库事务。 */
    private void evict(Collection<CacheKeyPair> keys) {
        try {
            for (CacheKeyPair key : keys) {
                stringRedisTemplate.execute(INVALIDATE_SCRIPT, List.of(key.detailKey(), key.versionKey()));
            }
        } catch (RuntimeException exception) {
            logger.error("[质量详情缓存删除失败] keys: {}, errorMessage: {}",
                    keys, exception.getMessage(), exception);
        }
    }

    /** 绑定同一资源实例的详情 Key 与版本 Key，确保失效脚本总是成对操作。 */
    private record CacheKeyPair(String detailKey, String versionKey) {
    }
}
