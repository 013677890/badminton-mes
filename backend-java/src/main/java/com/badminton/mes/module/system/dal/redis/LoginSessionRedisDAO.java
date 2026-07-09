package com.badminton.mes.module.system.dal.redis;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.badminton.mes.common.security.LoginSessionReader;
import com.badminton.mes.common.security.LoginUser;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import tools.jackson.databind.ObjectMapper;

/**
 * 登录会话 Redis DAO，同时实现 common 的 {@link LoginSessionReader}
 * 供拦截器读会话。
 *
 * <p>与缓存不同，Redis 是会话的事实数据源(强依赖)：读写异常直接上抛，
 * 由全局异常处理兜底为系统错误，不做降级放行。
 *
 * <p>正查 Key(token → LoginUser)承载鉴权，反向索引(userId → token)
 * 实现单设备登录与强制下线；两键同 TTL、同步续期。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Component
public class LoginSessionRedisDAO implements LoginSessionReader {

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    /**
     * 构造会话 DAO。
     *
     * @param stringRedisTemplate Redis 操作模板
     * @param objectMapper        JSON 序列化器
     */
    public LoginSessionRedisDAO(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 写入新会话；该用户已有旧会话时先删除旧 token(单设备登录互踢)。
     *
     * @param token     新登录令牌
     * @param loginUser 登录用户载荷
     */
    public void createSession(String token, LoginUser loginUser) {
        String indexKey = SystemRedisKeyConstants.loginUserIndexKey(loginUser.getUserId());
        String oldToken = stringRedisTemplate.opsForValue().get(indexKey);
        if (StringUtils.hasText(oldToken)) {
            stringRedisTemplate.delete(SystemRedisKeyConstants.loginTokenKey(oldToken));
        }
        String json = objectMapper.writeValueAsString(loginUser);
        stringRedisTemplate.opsForValue().set(SystemRedisKeyConstants.loginTokenKey(token),
                json, SystemRedisKeyConstants.LOGIN_SESSION_TTL);
        stringRedisTemplate.opsForValue().set(indexKey, token, SystemRedisKeyConstants.LOGIN_SESSION_TTL);
    }

    @Override
    public Optional<LoginUser> resolve(String token) {
        String tokenKey = SystemRedisKeyConstants.loginTokenKey(token);
        String json = stringRedisTemplate.opsForValue().get(tokenKey);
        if (!StringUtils.hasText(json)) {
            return Optional.empty();
        }
        LoginUser loginUser = objectMapper.readValue(json, LoginUser.class);
        renewIfNeeded(tokenKey, loginUser.getUserId());
        return Optional.of(loginUser);
    }

    /**
     * 删除当前 token 的会话(登出)。
     *
     * <p>反向索引仅在仍指向本 token 时删除，避免并发重新登录后误删新会话索引。
     *
     * @param token 登录令牌
     */
    public void removeSession(String token) {
        String tokenKey = SystemRedisKeyConstants.loginTokenKey(token);
        String json = stringRedisTemplate.opsForValue().get(tokenKey);
        stringRedisTemplate.delete(tokenKey);
        if (!StringUtils.hasText(json)) {
            return;
        }
        LoginUser loginUser = objectMapper.readValue(json, LoginUser.class);
        String indexKey = SystemRedisKeyConstants.loginUserIndexKey(loginUser.getUserId());
        if (token.equals(stringRedisTemplate.opsForValue().get(indexKey))) {
            stringRedisTemplate.delete(indexKey);
        }
    }

    /**
     * 按用户强制下线：停用、删除、重置密码、修改密码后调用。
     *
     * @param userId 用户主键
     */
    public void removeSessionByUserId(Long userId) {
        String indexKey = SystemRedisKeyConstants.loginUserIndexKey(userId);
        String token = stringRedisTemplate.opsForValue().get(indexKey);
        if (StringUtils.hasText(token)) {
            stringRedisTemplate.delete(SystemRedisKeyConstants.loginTokenKey(token));
        }
        stringRedisTemplate.delete(indexKey);
    }

    /**
     * 判断工号是否处于登录锁定状态(连续失败达到阈值)。
     *
     * @param userNo 工号
     * @return true 锁定中
     */
    public boolean isLoginLocked(String userNo) {
        String count = stringRedisTemplate.opsForValue().get(SystemRedisKeyConstants.loginFailKey(userNo));
        return count != null && Long.parseLong(count) >= SystemRedisKeyConstants.LOGIN_FAIL_MAX;
    }

    /**
     * 记录一次登录失败，并把锁定窗口重置为 15 分钟(自最后一次失败起算)。
     *
     * @param userNo 工号
     * @return 本次失败后的累计失败次数，供调用方判断是否触发锁定
     */
    public long recordLoginFail(String userNo) {
        String failKey = SystemRedisKeyConstants.loginFailKey(userNo);
        Long count = stringRedisTemplate.opsForValue().increment(failKey);
        stringRedisTemplate.expire(failKey, SystemRedisKeyConstants.LOGIN_FAIL_TTL);
        return count != null ? count : 0L;
    }

    /**
     * 登录成功后清空失败计数。
     *
     * @param userNo 工号
     */
    public void clearLoginFail(String userNo) {
        stringRedisTemplate.delete(SystemRedisKeyConstants.loginFailKey(userNo));
    }

    /**
     * 剩余有效期不足一半时把正查与反向索引同步续期到完整 TTL。
     *
     * @param tokenKey 会话正查 Key
     * @param userId   用户主键
     */
    private void renewIfNeeded(String tokenKey, Long userId) {
        Long remainingSeconds = stringRedisTemplate.getExpire(tokenKey, TimeUnit.SECONDS);
        // -2 键不存在、-1 未设置过期，均不续期
        if (remainingSeconds == null || remainingSeconds < 0) {
            return;
        }
        if (remainingSeconds < SystemRedisKeyConstants.LOGIN_SESSION_TTL.toSeconds() / 2) {
            stringRedisTemplate.expire(tokenKey, SystemRedisKeyConstants.LOGIN_SESSION_TTL);
            stringRedisTemplate.expire(SystemRedisKeyConstants.loginUserIndexKey(userId),
                    SystemRedisKeyConstants.LOGIN_SESSION_TTL);
        }
    }
}
