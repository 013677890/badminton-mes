package com.badminton.mes.module.barcode.dal.redis;

import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.barcode.dal.entity.BarcodeSerialEntity;
import com.badminton.mes.module.barcode.dal.repository.BarcodeSerialRepository;
import com.badminton.mes.module.barcode.enums.BarcodeSerialResetCycleEnum;

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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link RedisBarcodeSerialSequence} 单元测试。
 *
 * <p>Redis 与数据库依赖全部 Mock。覆盖 Key 格式、周期 TTL、
 * Key 丢失后的 MySQL 播种恢复与 Redis 异常路径。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@ExtendWith(MockitoExtension.class)
class RedisBarcodeSerialSequenceTest {

    /** 测试用规则 id */
    private static final Long RULE_ID = 200L;

    /** 测试用流水作用域 */
    private static final String SCOPE = "20260712:YMQ01";

    /** 期望的流水 Key */
    private static final String EXPECTED_KEY = "mes:barcode:serial:200:20260712:YMQ01";

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private BarcodeSerialRepository barcodeSerialRepository;

    private RedisBarcodeSerialSequence sequence;

    @BeforeEach
    void setUp() {
        sequence = new RedisBarcodeSerialSequence(stringRedisTemplate, barcodeSerialRepository);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("非首次取号：直接返回 INCR 结果，不设 TTL 不读数据库")
    void nextReturnsIncrementResult() {
        when(valueOperations.increment(EXPECTED_KEY)).thenReturn(5L);

        long serial = sequence.next(RULE_ID, SCOPE, BarcodeSerialResetCycleEnum.DAILY);

        assertThat(serial).isEqualTo(5L);
        verify(stringRedisTemplate, never()).expire(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(java.time.Duration.class));
        verifyNoInteractions(barcodeSerialRepository);
    }

    @Test
    @DisplayName("首次取号(按日)：设置两天 TTL，数据库无记录时从 1 开始")
    void nextAppliesDailyTtlOnFirstIncrement() {
        when(valueOperations.increment(EXPECTED_KEY)).thenReturn(1L);
        when(barcodeSerialRepository.findByRuleIdAndSerialScopeAndDeletedFalse(RULE_ID, SCOPE))
                .thenReturn(Optional.empty());

        long serial = sequence.next(RULE_ID, SCOPE, BarcodeSerialResetCycleEnum.DAILY);

        assertThat(serial).isEqualTo(1L);
        verify(stringRedisTemplate).expire(EXPECTED_KEY, BarcodeRedisKeyConstants.SERIAL_DAILY_TTL);
    }

    @Test
    @DisplayName("首次取号(不重置)：不设置 TTL")
    void nextSkipsTtlForNeverCycle() {
        when(valueOperations.increment(EXPECTED_KEY)).thenReturn(1L);
        when(barcodeSerialRepository.findByRuleIdAndSerialScopeAndDeletedFalse(RULE_ID, SCOPE))
                .thenReturn(Optional.empty());

        sequence.next(RULE_ID, SCOPE, BarcodeSerialResetCycleEnum.NEVER);

        verify(stringRedisTemplate, never()).expire(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(java.time.Duration.class));
    }

    @Test
    @DisplayName("Key 丢失恢复：首次 INCR 时按 MySQL current_serial 播种续号")
    void nextSeedsFromDatabaseWhenKeyLost() {
        when(valueOperations.increment(EXPECTED_KEY)).thenReturn(1L);
        BarcodeSerialEntity record = new BarcodeSerialEntity();
        record.setRuleId(RULE_ID);
        record.setSerialScope(SCOPE);
        record.setCurrentSerial(100L);
        when(barcodeSerialRepository.findByRuleIdAndSerialScopeAndDeletedFalse(RULE_ID, SCOPE))
                .thenReturn(Optional.of(record));
        when(valueOperations.increment(EXPECTED_KEY, 100L)).thenReturn(101L);

        long serial = sequence.next(RULE_ID, SCOPE, BarcodeSerialResetCycleEnum.MONTHLY);

        assertThat(serial).isEqualTo(101L);
        verify(stringRedisTemplate).expire(EXPECTED_KEY, BarcodeRedisKeyConstants.SERIAL_MONTHLY_TTL);
    }

    @Test
    @DisplayName("Redis 异常路径：INCR 返回 null 时报系统错误")
    void nextRejectsNullIncrement() {
        when(valueOperations.increment(EXPECTED_KEY)).thenReturn(null);

        assertThatThrownBy(() -> sequence.next(RULE_ID, SCOPE, BarcodeSerialResetCycleEnum.DAILY))
                .isInstanceOf(ServiceException.class);
    }
}
