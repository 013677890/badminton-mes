package com.badminton.mes.module.equipment.dal.redis;

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
 * 设备模块 Redis 旁路缓存。
 *
 * <p>详情查询采用 Cache-Aside：先读取详情 Key，未命中时由 {@code loader} 查询权威数据源并回填。
 * Redis 被视为弱依赖，版本读取、详情读取、序列化、写入或失效失败均只记录日志，数据库主流程
 * 不因缓存不可用而失败。
 *
 * <p>每个详情 Key 配套一个单调递增的版本 Key。读线程在加载数据库前记录版本，回填时通过 Lua
 * 原子比较版本并设置带 TTL 的详情值；写事务提交后则通过另一段 Lua 原子递增版本并删除详情值。
 * 即使旧读请求跨越数据库提交，其观察到的旧版本也无法回填旧值，从而关闭“删除后旧值回填”窗口。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Component
public class EquipmentCache {

    /** 缓存异常专用日志记录器；仅用于观测降级，不将 Redis 故障升级为业务异常。 */
    private static final Logger logger = LoggerFactory.getLogger(EquipmentCache.class);

    /**
     * 版本未变化时写入详情的原子 Lua 脚本。
     *
     * <p>{@code KEYS[1]} 为详情 Key，{@code KEYS[2]} 为版本 Key；{@code ARGV[1]} 是数据库加载前
     * 观察到的版本，{@code ARGV[2]} 是毫秒 TTL，{@code ARGV[3]} 是 JSON。版本 Key 不存在按
     * {@code 0} 处理；仅版本仍一致时执行 {@code PSETEX}，返回 {@code 1}，否则拒绝旧值并返回
     * {@code 0}。比较和写入在 Redis 单线程执行脚本期间不可被其他命令穿插。
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
     * 详情失效原子 Lua 脚本。
     *
     * <p>先递增 {@code KEYS[2]} 版本，再删除 {@code KEYS[1]} 详情；即使详情本来不存在也会推进
     * 版本，使已经读取旧版本但尚未完成数据库加载的并发请求无法重新写回失效前的数据。
     * 返回值是详情 Key 的删除数量，版本递增与删除在同一脚本中原子完成。
     */
    private static final DefaultRedisScript<Long> INVALIDATE_SCRIPT =
            new DefaultRedisScript<>("""
                    redis.call('INCR', KEYS[2])
                    return redis.call('DEL', KEYS[1])
                    """, Long.class);

    /** 执行字符串 Key/Value 操作及 Lua 脚本的 Redis 模板。 */
    private final StringRedisTemplate stringRedisTemplate;

    /** 将详情对象与 JSON 缓存值相互转换的序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 构造设备缓存组件。
     *
     * @param stringRedisTemplate Redis 操作模板
     * @param objectMapper        JSON 序列化器
     */
    public EquipmentCache(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 读取设备模块详情缓存，未命中时加载权威数据并尝试回填。
     *
     * <p>方法先读取版本快照，再读取详情；命中有效 JSON 时直接反序列化返回。未命中或 Redis
     * 读异常时同步调用 {@code loader}，随后仅在版本未变化时以统一 TTL 写入。缓存读写失败不会
     * 改变 {@code loader} 的结果或异常语义；版本读取失败时仍加载数据，但主动跳过回填。
     *
     * @param resourceName 资源类型
     * @param id           业务主键
     * @param valueType    缓存值类型
     * @param loader       缓存未命中时访问数据库等权威数据源的加载器
     * @param <T>          缓存值类型
     * @return 缓存命中的详情，或加载器返回的最新详情
     */
    public <T> T getOrLoadDetail(String resourceName, Long id, Class<T> valueType, Supplier<T> loader) {
        String detailKey = EquipmentRedisKeyConstants.detailKey(resourceName, id);
        String versionKey = EquipmentRedisKeyConstants.detailVersionKey(resourceName, id);
        String observedVersion = readVersion(versionKey, resourceName, id);
        try {
            String json = stringRedisTemplate.opsForValue().get(detailKey);
            if (StringUtils.hasText(json)) {
                return objectMapper.readValue(json, valueType);
            }
        } catch (RuntimeException exception) {
            logger.warn("[设备详情缓存读取失败] resourceName: {}, id: {}, errorMessage: {}",
                    resourceName, id, exception.getMessage());
        }
        T loadedValue = loader.get();
        writeIfVersionUnchanged(detailKey, versionKey, observedVersion, loadedValue, resourceName, id);
        return loadedValue;
    }

    /**
     * 在当前事务成功提交后失效单个详情缓存。
     *
     * <p>无事务同步上下文时立即失效；事务回滚时不会执行提交后回调，避免删除仍与数据库一致的缓存。
     *
     * @param resourceName 资源类型，参与详情与版本 Key 的命名空间隔离
     * @param id           需要失效的业务主键
     */
    public void evictDetailAfterCommit(String resourceName, Long id) {
        evictDetailsAfterCommit(resourceName, List.of(id));
    }

    /**
     * 在事务提交后批量失效同一资源类型的详情缓存。
     *
     * <p>空主键会被过滤，重复主键会合并；方法同时生成详情 Key 和版本 Key，随后统一注册提交后
     * 回调。批量接口用于一次数据库操作影响多个详情的场景，避免调用方自行拼装 Redis Key。
     *
     * @param resourceName 资源类型
     * @param ids          待失效业务主键集合
     */
    public void evictDetailsAfterCommit(String resourceName, Collection<Long> ids) {
        List<CacheKeyPair> cacheKeys = ids.stream()
                .filter(id -> id != null)
                .distinct()
                .map(id -> new CacheKeyPair(
                        EquipmentRedisKeyConstants.detailKey(resourceName, id),
                        EquipmentRedisKeyConstants.detailVersionKey(resourceName, id)))
                .toList();
        evictAfterCommit(cacheKeys);
    }

    /**
     * 读取详情版本快照，供后续 Lua 判断数据库加载期间是否发生过失效。
     *
     * <p>版本 Key 尚未创建时统一视为 {@code 0}；Redis 异常返回 {@code null}，以明确指示调用方
     * 跳过本次回填，避免在无法建立并发保护时写入可能过期的数据。
     */
    private String readVersion(String versionKey, String resourceName, Long id) {
        try {
            String version = stringRedisTemplate.opsForValue().get(versionKey);
            return StringUtils.hasText(version) ? version : "0";
        } catch (RuntimeException exception) {
            logger.warn("[设备详情缓存版本读取失败] resourceName: {}, id: {}, errorMessage: {}",
                    resourceName, id, exception.getMessage());
            return null;
        }
    }

    /**
     * 将加载结果序列化，并在版本仍等于观察值时原子写入带 TTL 的详情缓存。
     *
     * <p>观察版本为空表示版本读取已降级，此时不尝试写入；脚本返回的拒绝结果无需重试，因为
     * 版本变化说明数据库加载期间已有写事务提交并使该结果失去回填资格。
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
                    Long.toString(EquipmentRedisKeyConstants.EQUIPMENT_DETAIL_TTL.toMillis()), json);
        } catch (RuntimeException exception) {
            logger.warn("[设备详情缓存写入失败] resourceName: {}, id: {}, errorMessage: {}",
                    resourceName, id, exception.getMessage());
        }
    }

    /**
     * 注册事务提交后的缓存失效；无事务同步时立即执行。
     *
     * <p>复制为不可变集合，防止调用方在事务提交前修改待失效 Key。仅注册 {@code afterCommit}
     * 而不在事务内提前删除，可避免并发请求在数据库尚未提交时读取旧库值并重新回填缓存。
     *
     * @param keys 待失效的详情 Key 与版本 Key 配对集合
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
     * 逐项推进版本并删除详情缓存，Redis 异常时保留已提交的主业务结果并记录错误。
     *
     * <p>每一对 Key 通过失效 Lua 保证“版本递增、详情删除”原子执行；缓存属于弱依赖，异常不向
     * 已完成的数据库事务反向传播。错误日志保留完整 Key 集合及异常堆栈，便于后续排查和补偿。
     *
     * @param keys 待失效的详情 Key 与版本 Key 配对集合
     */
    private void evict(Collection<CacheKeyPair> keys) {
        try {
            for (CacheKeyPair key : keys) {
                stringRedisTemplate.execute(INVALIDATE_SCRIPT, List.of(key.detailKey(), key.versionKey()));
            }
        } catch (RuntimeException exception) {
            logger.error("[设备详情缓存删除失败] keys: {}, errorMessage: {}",
                    keys, exception.getMessage(), exception);
        }
    }

    /** 将同一资源实例的详情 Key 与并发控制版本 Key 绑定，避免批量失效时错配。 */
    private record CacheKeyPair(String detailKey, String versionKey) {
    }
}
