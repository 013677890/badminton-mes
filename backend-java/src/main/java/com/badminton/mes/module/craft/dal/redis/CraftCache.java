package com.badminton.mes.module.craft.dal.redis;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.craft.controller.vo.CraftProcessDefectReasonRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessSopRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteRespVO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import tools.jackson.databind.ObjectMapper;

/**
 * 工艺基础资料与默认路线 Redis 旁路缓存。
 *
 * <p>Redis 是弱依赖：读写或删除失败仅记录日志，数据库始终是最终数据源。
 * 所有失效操作在数据库事务提交后执行，避免并发读取旧数据并回填缓存。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
@Component
public class CraftCache {

    private static final Logger logger = LoggerFactory.getLogger(CraftCache.class);

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    /**
     * 构造工艺缓存组件。
     *
     * @param stringRedisTemplate Redis 操作模板
     * @param objectMapper        JSON 序列化器
     */
    public CraftCache(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 读取产品默认工艺路线缓存。
     *
     * @param productId 产品主键
     * @return 缓存命中的路线聚合响应
     */
    public Optional<CraftRouteRespVO> getDefaultRoute(Long productId) {
        return read(CraftRedisKeyConstants.defaultRouteKey(productId),
                CraftRouteRespVO.class, "默认路线", productId);
    }

    /**
     * 写入产品默认工艺路线缓存。
     *
     * @param productId 产品主键
     * @param route     路线聚合响应
     */
    public void putDefaultRoute(Long productId, CraftRouteRespVO route) {
        write(CraftRedisKeyConstants.defaultRouteKey(productId), route,
                CraftRedisKeyConstants.DEFAULT_ROUTE_TTL, "默认路线", productId);
    }

    /**
     * 读取工序详情缓存。
     *
     * @param processId 工序主键
     * @return 缓存命中的工序详情
     */
    public Optional<CraftProcessRespVO> getProcess(Long processId) {
        return read(CraftRedisKeyConstants.processDetailKey(processId),
                CraftProcessRespVO.class, "工序详情", processId);
    }

    /**
     * 写入工序详情缓存。
     *
     * @param process 工序详情
     */
    public void putProcess(CraftProcessRespVO process) {
        write(CraftRedisKeyConstants.processDetailKey(process.getId()), process,
                CraftRedisKeyConstants.PROCESS_DATA_TTL, "工序详情", process.getId());
    }

    /**
     * 读取工序 SOP 列表缓存。
     *
     * @param processId 工序主键
     * @return 缓存命中的 SOP 列表
     */
    public Optional<List<CraftProcessSopRespVO>> getProcessSops(Long processId) {
        Optional<CraftProcessSopRespVO[]> cached = read(
                CraftRedisKeyConstants.processSopsKey(processId),
                CraftProcessSopRespVO[].class, "工序 SOP", processId);
        return cached.map(items -> List.copyOf(Arrays.asList(items)));
    }

    /**
     * 写入工序 SOP 列表缓存。
     *
     * @param processId 工序主键
     * @param sops      SOP 列表
     */
    public void putProcessSops(Long processId, List<CraftProcessSopRespVO> sops) {
        write(CraftRedisKeyConstants.processSopsKey(processId), sops,
                CraftRedisKeyConstants.PROCESS_DATA_TTL, "工序 SOP", processId);
    }

    /**
     * 读取工序不良原因列表缓存。
     *
     * @param processId 工序主键
     * @return 缓存命中的不良原因列表
     */
    public Optional<List<CraftProcessDefectReasonRespVO>> getProcessDefectReasons(Long processId) {
        Optional<CraftProcessDefectReasonRespVO[]> cached = read(
                CraftRedisKeyConstants.processDefectReasonsKey(processId),
                CraftProcessDefectReasonRespVO[].class, "工序不良原因", processId);
        return cached.map(items -> List.copyOf(Arrays.asList(items)));
    }

    /**
     * 写入工序不良原因列表缓存。
     *
     * @param processId 工序主键
     * @param reasons   不良原因列表
     */
    public void putProcessDefectReasons(Long processId,
                                        List<CraftProcessDefectReasonRespVO> reasons) {
        write(CraftRedisKeyConstants.processDefectReasonsKey(processId), reasons,
                CraftRedisKeyConstants.PROCESS_DATA_TTL, "工序不良原因", processId);
    }

    /**
     * 在事务提交后删除一组产品的默认路线缓存。
     *
     * @param productIds 产品主键集合
     */
    public void evictDefaultRoutesAfterCommit(Collection<Long> productIds) {
        evictAfterCommit(productIds.stream()
                .distinct()
                .map(CraftRedisKeyConstants::defaultRouteKey)
                .toList());
    }

    /**
     * 在事务提交后删除工序详情缓存。
     *
     * @param processId 工序主键
     */
    public void evictProcessAfterCommit(Long processId) {
        evictAfterCommit(List.of(CraftRedisKeyConstants.processDetailKey(processId)));
    }

    /**
     * 在事务提交后删除工序及其子资料缓存。
     *
     * @param processId 工序主键
     */
    public void evictProcessAggregateAfterCommit(Long processId) {
        evictAfterCommit(List.of(
                CraftRedisKeyConstants.processDetailKey(processId),
                CraftRedisKeyConstants.processSopsKey(processId),
                CraftRedisKeyConstants.processDefectReasonsKey(processId)));
    }

    /**
     * 在事务提交后删除工序 SOP 列表缓存。
     *
     * @param processId 工序主键
     */
    public void evictProcessSopsAfterCommit(Long processId) {
        evictAfterCommit(List.of(CraftRedisKeyConstants.processSopsKey(processId)));
    }

    /**
     * 在事务提交后删除工序不良原因列表缓存。
     *
     * @param processId 工序主键
     */
    public void evictProcessDefectReasonsAfterCommit(Long processId) {
        evictAfterCommit(List.of(CraftRedisKeyConstants.processDefectReasonsKey(processId)));
    }

    /**
     * 读取并反序列化缓存，Redis 异常时按未命中降级。
     *
     * @param key        缓存 Key
     * @param valueType  目标类型
     * @param cacheName  日志中的缓存名称
     * @param businessId 业务主键
     * @param <T>        缓存值类型
     * @return 缓存值
     */
    private <T> Optional<T> read(String key, Class<T> valueType,
                                 String cacheName, Long businessId) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(json)) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, valueType));
        } catch (RuntimeException exception) {
            logger.warn("[{}缓存读取失败] businessId: {}, errorMessage: {}",
                    cacheName, businessId, exception.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 序列化并写入带 TTL 的缓存，Redis 异常时不阻断业务。
     *
     * @param key        缓存 Key
     * @param value      缓存值
     * @param ttl        过期时间
     * @param cacheName  日志中的缓存名称
     * @param businessId 业务主键
     */
    private void write(String key, Object value, Duration ttl,
                       String cacheName, Long businessId) {
        try {
            String json = objectMapper.writeValueAsString(value);
            stringRedisTemplate.opsForValue().set(key, json, ttl);
        } catch (RuntimeException exception) {
            logger.warn("[{}缓存写入失败] businessId: {}, errorMessage: {}",
                    cacheName, businessId, exception.getMessage());
        }
    }

    /**
     * 注册事务提交后的批量缓存删除；无事务同步时立即删除。
     *
     * @param keys 待删除的缓存 Key
     */
    private void evictAfterCommit(Collection<String> keys) {
        List<String> immutableKeys = List.copyOf(keys);
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
     * 批量删除缓存，Redis 异常时保留主业务结果并记录错误。
     *
     * @param keys 待删除的缓存 Key
     */
    private void evict(Collection<String> keys) {
        try {
            stringRedisTemplate.delete(keys);
        } catch (RuntimeException exception) {
            logger.error("[工艺缓存删除失败] keys: {}, errorMessage: {}",
                    keys, exception.getMessage(), exception);
        }
    }
}
