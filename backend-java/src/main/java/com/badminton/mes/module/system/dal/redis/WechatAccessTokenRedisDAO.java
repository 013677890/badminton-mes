package com.badminton.mes.module.system.dal.redis;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 微信 access_token Redis 缓存。 */
@Component
public class WechatAccessTokenRedisDAO {

    private final StringRedisTemplate redisTemplate;

    public WechatAccessTokenRedisDAO(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Optional<String> find(String appId) {
        String token = redisTemplate.opsForValue().get(
                SystemRedisKeyConstants.wechatAccessTokenKey(appId));
        return StringUtils.hasText(token) ? Optional.of(token) : Optional.empty();
    }

    public void save(String appId, String token, Duration ttl) {
        redisTemplate.opsForValue().set(
                SystemRedisKeyConstants.wechatAccessTokenKey(appId), token, ttl);
    }

    public void remove(String appId) {
        redisTemplate.delete(SystemRedisKeyConstants.wechatAccessTokenKey(appId));
    }
}
