package com.badminton.mes.module.production.dal.redis;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;
import com.badminton.mes.module.production.enums.WorkOrderSourceTypeEnum;
import com.badminton.mes.module.production.enums.WorkOrderStatusEnum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link WorkOrderCache} 单元测试。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
@ExtendWith(MockitoExtension.class)
class WorkOrderCacheTest {

    private static final Long WORK_ORDER_ID = 100L;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private WorkOrderCache workOrderCache;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @BeforeEach
    void setUp() {
        workOrderCache = new WorkOrderCache(stringRedisTemplate, jsonMapper);
    }

    @Test
    @DisplayName("读取缓存：命中时反序列化为工单实体")
    void getReturnsEntityWhenCacheHits() {
        WorkOrderEntity entity = buildWorkOrder();
        String key = ProductionRedisKeyConstants.workOrderDetailKey(WORK_ORDER_ID);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(jsonMapper.writeValueAsString(WorkOrderCacheDTO.fromEntity(entity)));

        Optional<WorkOrderEntity> result = workOrderCache.get(WORK_ORDER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getWorkOrderNo()).isEqualTo("WO202607080001");
    }

    @Test
    @DisplayName("读取缓存：未命中返回空 Optional")
    void getReturnsEmptyWhenCacheMisses() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(ProductionRedisKeyConstants.workOrderDetailKey(WORK_ORDER_ID))).thenReturn(null);

        Optional<WorkOrderEntity> result = workOrderCache.get(WORK_ORDER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("读取缓存：Redis 故障时降级为空")
    void getReturnsEmptyWhenRedisFails() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RedisConnectionFailureException("connection refused"));

        Optional<WorkOrderEntity> result = workOrderCache.get(WORK_ORDER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("写入缓存：使用工单详情 Key 与固定 TTL")
    void putWritesWithDetailKeyAndTtl() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        workOrderCache.put(buildWorkOrder());

        verify(valueOperations).set(eq(ProductionRedisKeyConstants.workOrderDetailKey(WORK_ORDER_ID)),
                anyString(), eq(Duration.ofMinutes(30)));
    }

    @Test
    @DisplayName("删除缓存：Redis 故障时不向外抛异常")
    void evictSwallowsRedisFailure() {
        doThrow(new RedisConnectionFailureException("connection refused"))
                .when(stringRedisTemplate).delete(ProductionRedisKeyConstants.workOrderDetailKey(WORK_ORDER_ID));

        workOrderCache.evict(WORK_ORDER_ID);
    }

    private WorkOrderEntity buildWorkOrder() {
        WorkOrderEntity workOrder = new WorkOrderEntity();
        workOrder.setId(WORK_ORDER_ID);
        workOrder.setWorkOrderNo("WO202607080001");
        workOrder.setSourceType(WorkOrderSourceTypeEnum.MANUAL.getType());
        workOrder.setProductId(10L);
        workOrder.setProductName("比赛级羽毛球");
        workOrder.setUnitId(1L);
        workOrder.setWorkshopId(20L);
        workOrder.setPlanQuantity(1000);
        workOrder.setPlanStartTime(LocalDateTime.of(2026, 7, 10, 8, 0, 0));
        workOrder.setPlanEndTime(LocalDateTime.of(2026, 7, 15, 18, 0, 0));
        workOrder.setOrderStatus(WorkOrderStatusEnum.CREATED.getStatus());
        workOrder.setDeleted(false);
        return workOrder;
    }
}
