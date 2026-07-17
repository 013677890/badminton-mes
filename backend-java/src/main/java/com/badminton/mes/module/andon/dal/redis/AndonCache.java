package com.badminton.mes.module.andon.dal.redis;

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
 * 安灯模块 Redis 旁路缓存。
 *
 * <p>采用 Cache-Aside 模式：详情未命中时由调用方提供的加载器读取数据库，再尝试写回 Redis。
 * Redis 是弱依赖，读取、序列化、写入或删除失败只记录日志，不阻断数据库主业务。
 * 删除操作优先注册到事务提交后执行，避免数据库尚未提交时并发请求读到旧值并再次回填缓存。
 *
 * <p>版本 Key 用于关闭“读库与回填之间”的竞态窗口：读取详情前先观察版本，写回时由 Lua 脚本确认版本未变化；
 * 事务提交后的失效脚本先递增版本再删除详情。即使慢查询在失效后才完成，也无法把事务前的旧快照重新写入缓存。
 */
@Component
public class AndonCache {

    /** 缓存故障审计日志；故障不会升级为业务异常。 */
    private static final Logger logger = LoggerFactory.getLogger(AndonCache.class);

    /** 仅当查询开始时观察到的版本仍然有效时写入详情，保证版本校验和写入在 Redis 内原子执行。 */
    private static final DefaultRedisScript<Long> WRITE_IF_VERSION_UNCHANGED_SCRIPT =
            new DefaultRedisScript<>("""
                    local currentVersion = redis.call('GET', KEYS[2]) or '0'
                    if currentVersion ~= ARGV[1] then
                        return 0
                    end
                    redis.call('PSETEX', KEYS[1], ARGV[2], ARGV[3])
                    return 1
                    """, Long.class);

    /** 先递增版本再删除详情的原子失效脚本，用于拒绝仍在执行中的旧快照回填。 */
    private static final DefaultRedisScript<Long> INVALIDATE_SCRIPT =
            new DefaultRedisScript<>("""
                    redis.call('INCR', KEYS[2])
                    return redis.call('DEL', KEYS[1])
                    """, Long.class);

    /** 执行字符串缓存访问和 Lua 脚本的 Redis 客户端。 */
    private final StringRedisTemplate stringRedisTemplate;
    /** 在响应 VO 与 JSON 之间转换的序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 创建安灯旁路缓存组件。
     *
     * @param stringRedisTemplate Redis 字符串访问模板
     * @param objectMapper 详情对象 JSON 序列化器
     */
    public AndonCache(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 读取详情缓存，并在未命中时回源加载。
     *
     * <p>加载器异常属于数据库主流程异常，会原样向上传播；只有 Redis 和序列化异常被降级处理。
     * 若读取版本失败，则仍返回回源结果，但主动跳过回填，防止在无法判断并发失效时污染缓存。
     *
     * @param resourceName 资源类型，用于隔离不同业务表的相同主键
     * @param id 详情业务主键
     * @param valueType 反序列化目标类型
     * @param loader 缓存未命中时执行的数据库加载器
     * @param <T> 详情响应类型
     * @return 缓存值或数据库加载结果
     */
    public <T> T getOrLoadDetail(String resourceName, Long id, Class<T> valueType, Supplier<T> loader) {
        // 详情 Key 保存业务数据，版本 Key 只用于判断查询期间是否发生过数据库提交后的缓存失效。
        String detailKey = AndonRedisKeyConstants.detailKey(resourceName, id);
        String versionKey = AndonRedisKeyConstants.detailVersionKey(resourceName, id);
        String observedVersion = readVersion(versionKey, resourceName, id);
        try {
            // 命中时直接反序列化响应对象，避免再次访问 MySQL 及其关联表。
            String json = stringRedisTemplate.opsForValue().get(detailKey);
            if (StringUtils.hasText(json)) {
                return objectMapper.readValue(json, valueType);
            }
        } catch (RuntimeException exception) {
            // Redis 或 JSON 读取异常按弱依赖降级，后续仍会继续执行数据库加载器。
            logger.warn("[安灯详情缓存读取失败] resourceName: {}, id: {}, errorMessage: {}",
                    resourceName, id, exception.getMessage());
        }
        // loader 的数据库异常必须向上传播；只有成功获得事实数据后才尝试回填缓存。
        T loadedValue = loader.get();
        writeIfVersionUnchanged(detailKey, versionKey, observedVersion, loadedValue, resourceName, id);
        return loadedValue;
    }

    /**
     * 在当前事务成功提交后失效单个详情；无活动事务时立即失效。
     *
     * @param resourceName 资源类型
     * @param id 业务主键
     */
    public void evictDetailAfterCommit(String resourceName, Long id) {
        evictDetailsAfterCommit(resourceName, List.of(id));
    }

    /**
     * 批量安排事务后详情失效。
     *
     * <p>方法会过滤空主键并去重，适用于类型修改后对原因、配置和事件聚合详情进行级联失效。
     * 不在提交前删除缓存，是为了避免事务最终回滚但缓存已经消失，或并发查询在未提交窗口重新写入旧数据。
     *
     * @param resourceName 资源类型
     * @param ids 需要失效的业务主键集合
     */
    public void evictDetailsAfterCommit(String resourceName, Collection<Long> ids) {
        // 先过滤空主键并去重，避免在同一事务提交回调中重复执行相同 Lua 失效脚本。
        List<CacheKeyPair> cacheKeys = ids.stream()
                .filter(id -> id != null)
                .distinct()
                .map(id -> new CacheKeyPair(
                        AndonRedisKeyConstants.detailKey(resourceName, id),
                        AndonRedisKeyConstants.detailVersionKey(resourceName, id)))
                .toList();
        evictAfterCommit(cacheKeys);
    }

    /** 读取并发控制版本；失败时返回 {@code null}，由调用方跳过不安全的缓存回填。 */
    private String readVersion(String versionKey, String resourceName, Long id) {
        try {
            String version = stringRedisTemplate.opsForValue().get(versionKey);
            // 尚未创建版本 Key 的详情按初始版本 0 处理，使第一次查询可以正常写入缓存。
            return StringUtils.hasText(version) ? version : "0";
        } catch (RuntimeException exception) {
            logger.warn("[安灯详情缓存版本读取失败] resourceName: {}, id: {}, errorMessage: {}",
                    resourceName, id, exception.getMessage());
            return null;
        }
    }

    /**
     * 通过 Lua 原子比较版本并写入详情。
     *
     * <p>该方法不会影响调用方已经获得的数据库结果；Redis 写入失败仅降低后续命中率。
     */
    private void writeIfVersionUnchanged(String detailKey, String versionKey, String observedVersion,
                                         Object value, String resourceName, Long id) {
        // 版本读取失败时无法证明回源结果仍然新鲜，因此宁可不缓存，也不冒险写入旧快照。
        if (observedVersion == null) {
            return;
        }
        try {
            // 序列化在调用 Lua 前完成；脚本在 Redis 内原子完成版本比较、写值和设置 TTL。
            String json = objectMapper.writeValueAsString(value);
            stringRedisTemplate.execute(WRITE_IF_VERSION_UNCHANGED_SCRIPT,
                    List.of(detailKey, versionKey), observedVersion,
                    Long.toString(AndonRedisKeyConstants.ANDON_DETAIL_TTL.toMillis()), json);
        } catch (RuntimeException exception) {
            logger.warn("[安灯详情缓存写入失败] resourceName: {}, id: {}, errorMessage: {}",
                    resourceName, id, exception.getMessage());
        }
    }

    /**
     * 将失效动作绑定到事务提交回调；复制集合以避免调用方后续修改集合改变回调内容。
     */
    private void evictAfterCommit(Collection<CacheKeyPair> keys) {
        // 拷贝为不可变集合，防止事务提交前调用方修改原集合导致实际失效对象发生变化。
        List<CacheKeyPair> immutableKeys = List.copyOf(keys);
        if (immutableKeys.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // 查询外或无事务写入场景没有提交回调可注册，直接执行失效即可。
            evict(immutableKeys);
            return;
        }
        // 数据库事务成功提交后再递增版本并删除详情，回滚事务不会触碰已有缓存。
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                evict(immutableKeys);
            }
        });
    }

    /** 执行版本递增与详情删除；任一 Redis 故障只记日志，不回滚已经提交的数据库事务。 */
    private void evict(Collection<CacheKeyPair> keys) {
        try {
            for (CacheKeyPair key : keys) {
                // 每组详情 Key 与版本 Key 必须交给同一 Lua 脚本处理，避免出现先删值后增版本的竞态窗口。
                stringRedisTemplate.execute(INVALIDATE_SCRIPT, List.of(key.detailKey(), key.versionKey()));
            }
        } catch (RuntimeException exception) {
            logger.error("[安灯详情缓存删除失败] keys: {}, errorMessage: {}",
                    keys, exception.getMessage(), exception);
        }
    }

    /** 同一业务详情的缓存 Key 与版本 Key，不允许拆分失效。 */
    private record CacheKeyPair(String detailKey, String versionKey) {
    }
}
