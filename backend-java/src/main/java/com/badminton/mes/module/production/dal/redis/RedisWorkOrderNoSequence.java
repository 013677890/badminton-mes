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
        String date = LocalDate.now().format(SERIAL_DATE_FORMATTER);
        String serialKey = ProductionRedisKeyConstants.workOrderSerialKey(date);
        Long serial = stringRedisTemplate.opsForValue().increment(serialKey);
        if (serial == null) {
            throw new ServiceException(GlobalErrorCodeConstants.SYSTEM_ERROR);
        }
        if (serial == 1L) {
            stringRedisTemplate.expire(serialKey, ProductionRedisKeyConstants.WORK_ORDER_SERIAL_TTL);
        }
        return String.format("%s%s%04d", WORK_ORDER_NO_PREFIX, date, serial);
    }
}
