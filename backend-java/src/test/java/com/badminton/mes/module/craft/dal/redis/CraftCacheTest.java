package com.badminton.mes.module.craft.dal.redis;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.craft.controller.vo.CraftProcessRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessSopRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteRespVO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CraftCache} 单元测试。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
@ExtendWith(MockitoExtension.class)
class CraftCacheTest {

    private static final Long PRODUCT_ID = 10L;

    private static final Long PROCESS_ID = 20L;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private CraftCache craftCache;

    @BeforeEach
    void setUp() {
        craftCache = new CraftCache(stringRedisTemplate, jsonMapper);
    }

    @Test
    @DisplayName("默认路线缓存：命中时反序列化聚合响应")
    void getDefaultRouteReturnsCachedValue() {
        CraftRouteRespVO route = new CraftRouteRespVO();
        route.setId(100L);
        route.setRoutingCode("RT-01");
        String key = CraftRedisKeyConstants.defaultRouteKey(PRODUCT_ID);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(jsonMapper.writeValueAsString(route));

        Optional<CraftRouteRespVO> result = craftCache.getDefaultRoute(PRODUCT_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getRoutingCode()).isEqualTo("RT-01");
    }

    @Test
    @DisplayName("默认路线缓存：写入时使用产品 Key 与一小时 TTL")
    void putDefaultRouteUsesExpectedKeyAndTtl() {
        CraftRouteRespVO route = new CraftRouteRespVO();
        route.setId(100L);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        craftCache.putDefaultRoute(PRODUCT_ID, route);

        verify(valueOperations).set(eq(CraftRedisKeyConstants.defaultRouteKey(PRODUCT_ID)),
                anyString(), eq(Duration.ofHours(1)));
    }

    @Test
    @DisplayName("工序 SOP 缓存：空数组也是有效命中")
    void getProcessSopsTreatsEmptyArrayAsHit() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(CraftRedisKeyConstants.processSopsKey(PROCESS_ID)))
                .thenReturn("[]");

        Optional<List<CraftProcessSopRespVO>> result = craftCache.getProcessSops(PROCESS_ID);

        assertThat(result).isPresent();
        assertThat(result.get()).isEmpty();
    }

    @Test
    @DisplayName("工序详情缓存：写入时使用精确 Key 与固定 TTL")
    void putProcessUsesExpectedKeyAndTtl() {
        CraftProcessRespVO process = new CraftProcessRespVO();
        process.setId(PROCESS_ID);
        process.setProcessCode("P-01");
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        craftCache.putProcess(process);

        verify(valueOperations).set(eq(CraftRedisKeyConstants.processDetailKey(PROCESS_ID)),
                anyString(), eq(Duration.ofMinutes(45)));
    }

    @Test
    @DisplayName("缓存读取：Redis 故障时降级为未命中")
    void getReturnsEmptyWhenRedisFails() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString()))
                .thenThrow(new RedisConnectionFailureException("connection refused"));

        Optional<CraftProcessRespVO> result = craftCache.getProcess(PROCESS_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("默认路线失效：对产品主键去重后批量删除")
    void evictDefaultRoutesDeduplicatesProductIds() {
        craftCache.evictDefaultRoutesAfterCommit(List.of(PRODUCT_ID, PRODUCT_ID, 11L));

        verify(stringRedisTemplate).delete(List.of(
                CraftRedisKeyConstants.defaultRouteKey(PRODUCT_ID),
                CraftRedisKeyConstants.defaultRouteKey(11L)));
    }

    @Test
    @DisplayName("工序聚合失效：事务提交前不删除，提交后删除全部相关 Key")
    void evictProcessAggregateDefersUntilAfterCommit() {
        List<String> expectedKeys = List.of(
                CraftRedisKeyConstants.processDetailKey(PROCESS_ID),
                CraftRedisKeyConstants.processSopsKey(PROCESS_ID),
                CraftRedisKeyConstants.processDefectReasonsKey(PROCESS_ID));
        TransactionSynchronizationManager.initSynchronization();
        try {
            craftCache.evictProcessAggregateAfterCommit(PROCESS_ID);

            verify(stringRedisTemplate, never()).delete(expectedKeys);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            verify(stringRedisTemplate).delete(expectedKeys);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
