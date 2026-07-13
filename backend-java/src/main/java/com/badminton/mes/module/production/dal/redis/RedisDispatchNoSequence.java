package com.badminton.mes.module.production.dal.redis;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.badminton.mes.common.core.GlobalErrorCodeConstants;
import com.badminton.mes.common.exception.ServiceException;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 基于 Redis INCR 的派工单号流水生成器，格式 DP + yyyyMMdd + 4 位流水。
 *
 * <p>与工单号生成器同构；数据库唯一索引 uk_dispatch_no 作为最终防重复兜底。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Component
public class RedisDispatchNoSequence implements DispatchNoSequence {

    /** 派工单号前缀 */
    private static final String DISPATCH_NO_PREFIX = "DP";

    /** 派工单号日期段格式；DateTimeFormatter 线程安全，静态复用 */
    private static final DateTimeFormatter SERIAL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 构造 Redis 派工单号流水生成器。
     *
     * @param stringRedisTemplate Redis 操作模板
     */
    public RedisDispatchNoSequence(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public String nextNo() {
        String date = LocalDate.now().format(SERIAL_DATE_FORMATTER);
        String serialKey = ProductionRedisKeyConstants.dispatchSerialKey(date);
        Long serial = stringRedisTemplate.opsForValue().increment(serialKey);
        if (serial == null) {
            throw new ServiceException(GlobalErrorCodeConstants.SYSTEM_ERROR);
        }
        if (serial == 1L) {
            stringRedisTemplate.expire(serialKey, ProductionRedisKeyConstants.DISPATCH_SERIAL_TTL);
        }
        return String.format("%s%s%04d", DISPATCH_NO_PREFIX, date, serial);
    }
}
