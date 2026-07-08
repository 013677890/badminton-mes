package com.badminton.mes.module.production.dal.redis;

import com.badminton.mes.common.exception.ServiceException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RedisWorkOrderNoSequence} 单元测试。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
@ExtendWith(MockitoExtension.class)
class RedisWorkOrderNoSequenceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisWorkOrderNoSequence workOrderNoSequence;

    @BeforeEach
    void setUp() {
        workOrderNoSequence = new RedisWorkOrderNoSequence(stringRedisTemplate);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("生成工单号：当日首个流水设置过期时间")
    void nextNoSetsExpireWhenFirstSerial() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        String workOrderNo = workOrderNoSequence.nextNo();

        assertThat(workOrderNo).matches("WO\\d{8}0001");
        verify(stringRedisTemplate).expire(anyString(), eq(ProductionRedisKeyConstants.WORK_ORDER_SERIAL_TTL));
    }

    @Test
    @DisplayName("生成工单号：非首个流水不重复设置过期时间")
    void nextNoDoesNotSetExpireWhenNotFirstSerial() {
        when(valueOperations.increment(anyString())).thenReturn(2L);

        String workOrderNo = workOrderNoSequence.nextNo();

        assertThat(workOrderNo).matches("WO\\d{8}0002");
        verify(stringRedisTemplate, never()).expire(anyString(), eq(ProductionRedisKeyConstants.WORK_ORDER_SERIAL_TTL));
    }

    @Test
    @DisplayName("生成工单号：Redis INCR 返回 null 时抛系统异常")
    void nextNoThrowsWhenIncrementReturnsNull() {
        when(valueOperations.increment(anyString())).thenReturn(null);

        assertThatThrownBy(() -> workOrderNoSequence.nextNo()).isInstanceOf(ServiceException.class);
    }
}
