package com.badminton.mes.module.system.dal.redis;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 微信小程序账号绑定临时票据 Redis DAO。
 *
 * @author Codex
 * @date 2026/07/15
 */
@Component
public class MiniAppBindTicketRedisDAO {

    private final StringRedisTemplate redisTemplate;

    private final Duration ticketTtl;

    /**
     * 构造绑定票据 DAO。
     *
     * @param redisTemplate Redis 模板
     * @param ticketTtl 票据有效期
     */
    public MiniAppBindTicketRedisDAO(
            StringRedisTemplate redisTemplate,
            @Value("${mes.wechat.mini-app.bind-ticket-ttl:5m}") Duration ticketTtl) {
        this.redisTemplate = redisTemplate;
        this.ticketTtl = ticketTtl;
    }

    /**
     * 保存绑定票据。
     *
     * @param ticket 随机票据
     * @param openId 微信 OpenID
     */
    public void save(String ticket, String openId) {
        redisTemplate.opsForValue().set(SystemRedisKeyConstants.miniAppBindTicketKey(ticket), openId, ticketTtl);
    }

    /**
     * 读取票据但不消费，只有绑定成功后才删除。
     *
     * @param ticket 随机票据
     * @return OpenID
     */
    public Optional<String> find(String ticket) {
        String openId = redisTemplate.opsForValue().get(SystemRedisKeyConstants.miniAppBindTicketKey(ticket));
        return StringUtils.hasText(openId) ? Optional.of(openId) : Optional.empty();
    }

    /**
     * 删除已成功使用的票据。
     *
     * @param ticket 随机票据
     */
    public void remove(String ticket) {
        redisTemplate.delete(SystemRedisKeyConstants.miniAppBindTicketKey(ticket));
    }
}
