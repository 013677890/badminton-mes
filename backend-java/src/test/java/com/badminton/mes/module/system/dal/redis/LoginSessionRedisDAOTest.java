package com.badminton.mes.module.system.dal.redis;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.badminton.mes.common.security.LoginUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link LoginSessionRedisDAO} 单元测试。
 *
 * <p>覆盖会话创建(单设备互踢)、解析(滑动续期)、登出(乐观删索引)、
 * 强制下线与防爆破计数，StringRedisTemplate 全部 Mock，ObjectMapper 用真实实例。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@ExtendWith(MockitoExtension.class)
class LoginSessionRedisDAOTest {

    private static final String TOKEN = "token-aaa";

    private static final Long USER_ID = 1L;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private LoginSessionRedisDAO loginSessionRedisDAO;

    @BeforeEach
    void setUp() {
        loginSessionRedisDAO = new LoginSessionRedisDAO(stringRedisTemplate, new ObjectMapper());
    }

    @Test
    @DisplayName("创建会话：已有旧 token 时先删旧再写新(单设备互踢)")
    void createSessionKicksOldToken() {
        String oldToken = "token-old";
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(SystemRedisKeyConstants.loginUserIndexKey(USER_ID))).thenReturn(oldToken);

        loginSessionRedisDAO.createSession(TOKEN, buildLoginUser());

        verify(stringRedisTemplate).delete(SystemRedisKeyConstants.loginTokenKey(oldToken));
        verify(valueOperations).set(eq(SystemRedisKeyConstants.loginTokenKey(TOKEN)), anyString(),
                eq(SystemRedisKeyConstants.LOGIN_SESSION_TTL));
        verify(valueOperations).set(eq(SystemRedisKeyConstants.loginUserIndexKey(USER_ID)), eq(TOKEN),
                eq(SystemRedisKeyConstants.LOGIN_SESSION_TTL));
    }

    @Test
    @DisplayName("创建会话：无旧 token 时直接写入，不触发删除")
    void createSessionWithoutOldToken() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(SystemRedisKeyConstants.loginUserIndexKey(USER_ID))).thenReturn(null);

        loginSessionRedisDAO.createSession(TOKEN, buildLoginUser());

        verify(stringRedisTemplate, never()).delete(anyString());
        verify(valueOperations).set(eq(SystemRedisKeyConstants.loginTokenKey(TOKEN)), anyString(),
                eq(SystemRedisKeyConstants.LOGIN_SESSION_TTL));
    }

    @Test
    @DisplayName("解析会话：token 无效返回 empty，不续期")
    void resolveReturnsEmptyForInvalidToken() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(SystemRedisKeyConstants.loginTokenKey(TOKEN))).thenReturn(null);

        Optional<LoginUser> result = loginSessionRedisDAO.resolve(TOKEN);

        assertThat(result).isEmpty();
        verify(stringRedisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("解析会话：token 有效且剩余不足一半时滑动续期")
    void resolveRenewsWhenRemainingBelowHalf() {
        String json = new ObjectMapper().writeValueAsString(buildLoginUser());
        String tokenKey = SystemRedisKeyConstants.loginTokenKey(TOKEN);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(tokenKey)).thenReturn(json);
        // 剩余 1 小时，TTL 8 小时，不足一半 -> 续期
        when(stringRedisTemplate.getExpire(tokenKey, TimeUnit.SECONDS)).thenReturn(3600L);

        Optional<LoginUser> result = loginSessionRedisDAO.resolve(TOKEN);

        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(USER_ID);
        verify(stringRedisTemplate).expire(tokenKey, SystemRedisKeyConstants.LOGIN_SESSION_TTL);
        verify(stringRedisTemplate).expire(SystemRedisKeyConstants.loginUserIndexKey(USER_ID),
                SystemRedisKeyConstants.LOGIN_SESSION_TTL);
    }

    @Test
    @DisplayName("解析会话：剩余超过一半时不续期")
    void resolveDoesNotRenewWhenRemainingAboveHalf() {
        String json = new ObjectMapper().writeValueAsString(buildLoginUser());
        String tokenKey = SystemRedisKeyConstants.loginTokenKey(TOKEN);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(tokenKey)).thenReturn(json);
        // 剩余 5 小时，TTL 8 小时，超过一半 -> 不续期
        when(stringRedisTemplate.getExpire(tokenKey, TimeUnit.SECONDS)).thenReturn(18000L);

        loginSessionRedisDAO.resolve(TOKEN);

        verify(stringRedisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("登出：索引仍指向当前 token 时删除索引")
    void removeSessionDeletesIndexWhenMatching() {
        String json = new ObjectMapper().writeValueAsString(buildLoginUser());
        String tokenKey = SystemRedisKeyConstants.loginTokenKey(TOKEN);
        String indexKey = SystemRedisKeyConstants.loginUserIndexKey(USER_ID);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(tokenKey)).thenReturn(json);
        when(valueOperations.get(indexKey)).thenReturn(TOKEN);

        loginSessionRedisDAO.removeSession(TOKEN);

        verify(stringRedisTemplate).delete(tokenKey);
        verify(stringRedisTemplate).delete(indexKey);
    }

    @Test
    @DisplayName("登出：索引已指向新 token 时不删索引(避免误删重新登录后的会话)")
    void removeSessionSkipsIndexWhenNotMatching() {
        String json = new ObjectMapper().writeValueAsString(buildLoginUser());
        String indexKey = SystemRedisKeyConstants.loginUserIndexKey(USER_ID);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(SystemRedisKeyConstants.loginTokenKey(TOKEN))).thenReturn(json);
        when(valueOperations.get(indexKey)).thenReturn("token-new");

        loginSessionRedisDAO.removeSession(TOKEN);

        verify(stringRedisTemplate).delete(SystemRedisKeyConstants.loginTokenKey(TOKEN));
        verify(stringRedisTemplate, never()).delete(indexKey);
    }

    @Test
    @DisplayName("登出：token 不存在时仅尝试删除 token，不读索引")
    void removeSessionHandlesMissingToken() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(SystemRedisKeyConstants.loginTokenKey(TOKEN))).thenReturn(null);

        loginSessionRedisDAO.removeSession(TOKEN);

        verify(stringRedisTemplate).delete(SystemRedisKeyConstants.loginTokenKey(TOKEN));
        verify(valueOperations, never()).get(SystemRedisKeyConstants.loginUserIndexKey(USER_ID));
    }

    @Test
    @DisplayName("强制下线：删除 token 和索引")
    void removeSessionByUserIdDeletesBothKeys() {
        String indexKey = SystemRedisKeyConstants.loginUserIndexKey(USER_ID);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(indexKey)).thenReturn(TOKEN);

        loginSessionRedisDAO.removeSessionByUserId(USER_ID);

        verify(stringRedisTemplate).delete(SystemRedisKeyConstants.loginTokenKey(TOKEN));
        verify(stringRedisTemplate).delete(indexKey);
    }

    @Test
    @DisplayName("强制下线：无索引时仅删索引，不触发 token 删除")
    void removeSessionByUserIdWithoutIndex() {
        String indexKey = SystemRedisKeyConstants.loginUserIndexKey(USER_ID);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(indexKey)).thenReturn(null);

        loginSessionRedisDAO.removeSessionByUserId(USER_ID);

        // delete(String) 只被调用一次，且参数是索引键而非 token 键
        verify(stringRedisTemplate, times(1)).delete(anyString());
        verify(stringRedisTemplate).delete(indexKey);
    }

    @Test
    @DisplayName("登录锁定：失败计数达到阈值返回 true")
    void isLoginLockedReturnsTrueAtThreshold() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(SystemRedisKeyConstants.loginFailKey("EMP001")))
                .thenReturn(String.valueOf(SystemRedisKeyConstants.LOGIN_FAIL_MAX));

        assertThat(loginSessionRedisDAO.isLoginLocked("EMP001")).isTrue();
    }

    @Test
    @DisplayName("登录锁定：失败计数低于阈值返回 false")
    void isLoginLockedReturnsFalseBelowThreshold() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(SystemRedisKeyConstants.loginFailKey("EMP001"))).thenReturn("3");

        assertThat(loginSessionRedisDAO.isLoginLocked("EMP001")).isFalse();
    }

    @Test
    @DisplayName("记录登录失败：递增计数并重置锁定窗口，返回当前次数")
    void recordLoginFailIncrementsAndReturnsCount() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(SystemRedisKeyConstants.loginFailKey("EMP001"))).thenReturn(5L);

        long count = loginSessionRedisDAO.recordLoginFail("EMP001");

        assertThat(count).isEqualTo(5L);
        verify(stringRedisTemplate).expire(SystemRedisKeyConstants.loginFailKey("EMP001"),
                SystemRedisKeyConstants.LOGIN_FAIL_TTL);
    }

    @Test
    @DisplayName("登录成功：清空失败计数")
    void clearLoginFailDeletesKey() {
        loginSessionRedisDAO.clearLoginFail("EMP001");

        verify(stringRedisTemplate).delete(SystemRedisKeyConstants.loginFailKey("EMP001"));
    }

    /**
     * 构造登录用户载荷。
     *
     * @return 登录用户
     */
    private LoginUser buildLoginUser() {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(USER_ID);
        loginUser.setUserNo("admin");
        loginUser.setUserName("管理员");
        loginUser.setRoleCodes(java.util.List.of("ADMIN"));
        return loginUser;
    }
}
