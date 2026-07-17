package com.badminton.mes.module.production.dal.redis;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.badminton.mes.common.core.GlobalErrorCodeConstants;
import com.badminton.mes.common.exception.ServiceException;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 基于 Redis INCR 的工单号流水生成器。
 *
 * <p>工单号生成当前强依赖 Redis；数据库唯一索引仍作为最终防重复兜底。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
@Component
public class RedisWorkOrderNoSequence implements WorkOrderNoSequence {

    /** 工单号前缀 */
    private static final String WORK_ORDER_NO_PREFIX = "WO";

    /** 工单号日期段格式；DateTimeFormatter 线程安全，静态复用 */
    private static final DateTimeFormatter SERIAL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 构造 Redis 工单号流水生成器。
     *
     * @param stringRedisTemplate Redis 操作模板
     */
    public RedisWorkOrderNoSequence(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public String nextNo() {
        // 日期作为 Redis Key 的一部分，保证每日独立计数，同时让旧日期计数在 TTL 到期后清理。
        String date = LocalDate.now().format(SERIAL_DATE_FORMATTER);
        String serialKey = ProductionRedisKeyConstants.workOrderSerialKey(date);
        // Redis INCR 提供跨实例原子递增；这里不做本地缓存，避免多节点分配相同流水。
        Long serial = stringRedisTemplate.opsForValue().increment(serialKey);
        if (serial == null) {
            throw new ServiceException(GlobalErrorCodeConstants.SYSTEM_ERROR);
        }
        if (serial == 1L) {
            // 只在第一次创建当天计数 Key 时设置 TTL，后续递增不延长生命周期。
            stringRedisTemplate.expire(serialKey, ProductionRedisKeyConstants.WORK_ORDER_SERIAL_TTL);
        }
        // 生成号可能因事务回滚出现空洞，这是可接受的；数据库唯一索引负责最终防重复。
        return String.format("%s%s%04d", WORK_ORDER_NO_PREFIX, date, serial);
    }
}
