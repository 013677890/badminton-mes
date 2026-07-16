package com.badminton.mes.module.system.dal.redis;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.system.constants.WechatBindingStatusConstants;
import com.badminton.mes.module.system.service.dto.WechatBindingResult;
import com.badminton.mes.module.system.service.dto.WechatBindingTicket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import tools.jackson.databind.ObjectMapper;

/** 微信账号绑定临时票据 Redis DAO。 */
@Component
public class MiniAppBindTicketRedisDAO {

    private static final DefaultRedisScript<String> CONSUME_SCRIPT = new DefaultRedisScript<>(
            "local value = redis.call('GET', KEYS[1]); "
                    + "if value then redis.call('DEL', KEYS[1]); return value; end; return nil;",
            String.class);

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    private final Duration ticketTtl;

    private final Duration resultTtl;

    public MiniAppBindTicketRedisDAO(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${mes.wechat.mini-app.bind-ticket-ttl:5m}") Duration ticketTtl,
            @Value("${mes.wechat.mini-app.bind-result-ttl:10m}") Duration resultTtl) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ticketTtl = ticketTtl;
        this.resultTtl = resultTtl.compareTo(ticketTtl) < 0 ? ticketTtl.plusMinutes(1) : resultTtl;
    }

    /** 同时保存可消费票据和供所属账号轮询的初始状态。 */
    public void save(String ticket, WechatBindingTicket payload) {
        String json = objectMapper.writeValueAsString(payload);
        WechatBindingResult pending = new WechatBindingResult(
                payload.userId(), WechatBindingStatusConstants.PENDING, payload.expiresAt(), null);
        redisTemplate.opsForValue().set(
                SystemRedisKeyConstants.miniAppBindTicketKey(ticket), json, ticketTtl);
        saveResult(ticket, pending);
    }

    public Optional<WechatBindingTicket> find(String ticket) {
        String json = redisTemplate.opsForValue().get(
                SystemRedisKeyConstants.miniAppBindTicketKey(ticket));
        return read(json, WechatBindingTicket.class);
    }

    /** 使用 Lua 在 Redis 内原子执行 GET + DEL，确保票据只能消费一次。 */
    public Optional<WechatBindingTicket> consume(String ticket) {
        String json = redisTemplate.execute(CONSUME_SCRIPT,
                List.of(SystemRedisKeyConstants.miniAppBindTicketKey(ticket)));
        return read(json, WechatBindingTicket.class);
    }

    public void saveResult(String ticket, WechatBindingResult result) {
        redisTemplate.opsForValue().set(
                SystemRedisKeyConstants.miniAppBindResultKey(ticket),
                objectMapper.writeValueAsString(result), resultTtl);
    }

    public Optional<WechatBindingResult> findResult(String ticket) {
        String json = redisTemplate.opsForValue().get(
                SystemRedisKeyConstants.miniAppBindResultKey(ticket));
        return read(json, WechatBindingResult.class);
    }

    public Duration getTicketTtl() {
        return ticketTtl;
    }

    private <T> Optional<T> read(String json, Class<T> type) {
        if (!StringUtils.hasText(json)) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.readValue(json, type));
    }
}
