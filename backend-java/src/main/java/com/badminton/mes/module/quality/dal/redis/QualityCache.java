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
 * <p>Redis 是弱依赖：读取、写入或删除失败只记录日志，不阻断主业务。
 * 删除操作优先注册到事务提交后执行，避免提交前并发读旧值并回填缓存。
 */
@Component
public class QualityCache {

    private static final Logger logger = LoggerFactory.getLogger(QualityCache.class);

    private static final DefaultRedisScript<Long> WRITE_IF_VERSION_UNCHANGED_SCRIPT =
            new DefaultRedisScript<>("""
                    local currentVersion = redis.call('GET', KEYS[2]) or '0'
                    if currentVersion ~= ARGV[1] then
                        return 0
                    end
                    redis.call('PSETEX', KEYS[1], ARGV[2], ARGV[3])
                    return 1
                    """, Long.class);

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

    public void evictDetailAfterCommit(String resourceName, Long id) {
        evictDetailsAfterCommit(resourceName, List.of(id));
    }

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

    private record CacheKeyPair(String detailKey, String versionKey) {
    }
}
