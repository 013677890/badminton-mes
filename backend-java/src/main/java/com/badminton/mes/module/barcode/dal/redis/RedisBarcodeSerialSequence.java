package com.badminton.mes.module.barcode.dal.redis;

import com.badminton.mes.common.core.GlobalErrorCodeConstants;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.barcode.dal.entity.BarcodeSerialEntity;
import com.badminton.mes.module.barcode.dal.repository.BarcodeSerialRepository;
import com.badminton.mes.module.barcode.enums.BarcodeSerialResetCycleEnum;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 基于 Redis INCR 的条码流水发号器。
 *
 * <p>Key 按"规则 + 作用域"维度隔离；周期性 Key 设置覆盖两个周期的 TTL。
 * Redis Key 丢失(重启/淘汰)时以 MySQL barcode_serial 的 current_serial
 * 播种恢复，避免从 1 重新计数造成大面积撞码；播种存在极小并发窗口，
 * 残余冲突由 barcode 表唯一索引兜底并由调用方重试(已冻结决策)。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Component
public class RedisBarcodeSerialSequence implements BarcodeSerialSequence {

    private final StringRedisTemplate stringRedisTemplate;

    private final BarcodeSerialRepository barcodeSerialRepository;

    /**
     * 构造 Redis 条码流水发号器。
     *
     * @param stringRedisTemplate     Redis 操作模板
     * @param barcodeSerialRepository 流水记录 Repository，Key 丢失恢复播种
     */
    public RedisBarcodeSerialSequence(StringRedisTemplate stringRedisTemplate,
                                      BarcodeSerialRepository barcodeSerialRepository) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.barcodeSerialRepository = barcodeSerialRepository;
    }

    @Override
    public long next(Long ruleId, String scope, BarcodeSerialResetCycleEnum cycle) {
        String serialKey = BarcodeRedisKeyConstants.barcodeSerialKey(ruleId, scope);
        Long serial = stringRedisTemplate.opsForValue().increment(serialKey);
        if (serial == null) {
            throw new ServiceException(GlobalErrorCodeConstants.SYSTEM_ERROR);
        }
        if (serial == 1L) {
            applyCycleTtl(serialKey, cycle);
            // 首次计数时从 MySQL 播种：Key 丢失场景避免从 1 重复发号
            long dbSerial = barcodeSerialRepository
                    .findByRuleIdAndSerialScopeAndDeletedFalse(ruleId, scope)
                    .map(BarcodeSerialEntity::getCurrentSerial)
                    .orElse(0L);
            if (dbSerial > 0) {
                Long seeded = stringRedisTemplate.opsForValue().increment(serialKey, dbSerial);
                if (seeded == null) {
                    throw new ServiceException(GlobalErrorCodeConstants.SYSTEM_ERROR);
                }
                return seeded;
            }
        }
        return serial;
    }

    /**
     * 按重置周期设置计数 Key TTL；不重置周期不过期。
     *
     * @param serialKey 计数 Key
     * @param cycle     重置周期
     */
    private void applyCycleTtl(String serialKey, BarcodeSerialResetCycleEnum cycle) {
        switch (cycle) {
            case DAILY -> stringRedisTemplate.expire(serialKey,
                    BarcodeRedisKeyConstants.SERIAL_DAILY_TTL);
            case MONTHLY -> stringRedisTemplate.expire(serialKey,
                    BarcodeRedisKeyConstants.SERIAL_MONTHLY_TTL);
            case NEVER -> {
                // 不重置：计数 Key 长期有效，与 MySQL 播种共同保证连续性
            }
        }
    }
}
