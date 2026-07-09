package com.badminton.mes.common.security;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.core.GlobalErrorCodeConstants;
import com.badminton.mes.common.exception.ServiceException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.method.HandlerMethod;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AuthInterceptor} 单元测试。
 *
 * <p>覆盖 token 解析、会话还原、角色校验与 ThreadLocal 清理时序，
 * 依赖 LoginSessionReader 全部 Mock。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

    private static final String TOKEN = "abc123";

    @Mock
    private LoginSessionReader loginSessionReader;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private AuthInterceptor authInterceptor;

    /**
     * 测试用 Controller：方法级与类级角色注解共存，便于验证优先级。
     */
    private static class TestController {

        @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
        public void adminOrPmcMethod() {
        }

        @RequiresRoles(RoleCodeConstants.OPERATOR)
        public void operatorMethod() {
        }

        public void openMethod() {
        }
    }

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        authInterceptor = new AuthInterceptor(loginSessionReader);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clear();
    }

    @Test
    @DisplayName("非 Controller 处理器(静态资源等)直接放行，不校验 token")
    void preHandleSkipsNonHandlerMethod() {
        boolean result = authInterceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(SecurityContextHolder.getLoginUser()).isNull();
    }

    @Test
    @DisplayName("无 Authorization 头抛 A0230，不写上下文")
    void preHandleRejectsMissingAuthHeader() {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        assertThatThrownBy(() -> authInterceptor.preHandle(request, response,
                handlerMethod("openMethod")))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCodeConstants.UNAUTHORIZED));
        assertThat(SecurityContextHolder.getLoginUser()).isNull();
    }

    @Test
    @DisplayName("非 Bearer 方案抛 A0230")
    void preHandleRejectsNonBearerScheme() {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic dXNlcjpwYXNz");

        assertThatThrownBy(() -> authInterceptor.preHandle(request, response,
                handlerMethod("openMethod")))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCodeConstants.UNAUTHORIZED));
    }

    @Test
    @DisplayName("token 无效或已过期抛 A0230，不写上下文")
    void preHandleRejectsInvalidToken() {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(AuthInterceptor.BEARER_PREFIX + TOKEN);
        when(loginSessionReader.resolve(TOKEN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authInterceptor.preHandle(request, response,
                handlerMethod("openMethod")))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCodeConstants.UNAUTHORIZED));
        assertThat(SecurityContextHolder.getLoginUser()).isNull();
    }

    @Test
    @DisplayName("有效 token 且无 @RequiresRoles 注解：写上下文并放行")
    void preHandlePassesWithoutRoleAnnotation() {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(AuthInterceptor.BEARER_PREFIX + TOKEN);
        LoginUser loginUser = buildLoginUser(List.of(RoleCodeConstants.OPERATOR));
        when(loginSessionReader.resolve(TOKEN)).thenReturn(Optional.of(loginUser));

        boolean result = authInterceptor.preHandle(request, response, handlerMethod("openMethod"));

        assertThat(result).isTrue();
        assertThat(SecurityContextHolder.getToken()).isEqualTo(TOKEN);
        assertThat(SecurityContextHolder.getLoginUser()).isSameAs(loginUser);
    }

    @Test
    @DisplayName("有效 token 且角色命中方法级注解：放行")
    void preHandlePassesWhenRoleMatches() {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(AuthInterceptor.BEARER_PREFIX + TOKEN);
        when(loginSessionReader.resolve(TOKEN))
                .thenReturn(Optional.of(buildLoginUser(List.of(RoleCodeConstants.PMC))));

        boolean result = authInterceptor.preHandle(request, response, handlerMethod("adminOrPmcMethod"));

        assertThat(result).isTrue();
        assertThat(SecurityContextHolder.getLoginUser()).isNotNull();
    }

    @Test
    @DisplayName("有效 token 但角色未命中注解：抛 A0301，不写上下文")
    void preHandleRejectsWhenRoleNotMatched() {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(AuthInterceptor.BEARER_PREFIX + TOKEN);
        when(loginSessionReader.resolve(TOKEN))
                .thenReturn(Optional.of(buildLoginUser(List.of(RoleCodeConstants.OPERATOR))));

        assertThatThrownBy(() -> authInterceptor.preHandle(request, response,
                handlerMethod("adminOrPmcMethod")))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCodeConstants.FORBIDDEN));
        // 角色校验在写上下文之前，抛异常时上下文应保持干净
        assertThat(SecurityContextHolder.getLoginUser()).isNull();
    }

    @Test
    @DisplayName("登录用户角色列表为 null 时不抛空指针，按无角色处理")
    void preHandleHandlesNullRoleCodes() {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(AuthInterceptor.BEARER_PREFIX + TOKEN);
        when(loginSessionReader.resolve(TOKEN)).thenReturn(Optional.of(buildLoginUser(null)));

        // 无 @RequiresRoles 的方法不校验角色，应放行
        boolean result = authInterceptor.preHandle(request, response, handlerMethod("openMethod"));

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("afterCompletion 清理 ThreadLocal，防止线程池脏读")
    void afterCompletionClearsContext() {
        SecurityContextHolder.set(TOKEN, buildLoginUser(List.of(RoleCodeConstants.ADMIN)));
        assertThat(SecurityContextHolder.getLoginUser()).isNotNull();

        authInterceptor.afterCompletion(request, response, new Object(), null);

        assertThat(SecurityContextHolder.getLoginUser()).isNull();
        assertThat(SecurityContextHolder.getToken()).isNull();
    }

    @Test
    @DisplayName("afterCompletion 即使有异常也清理上下文")
    void afterCompletionClearsContextEvenWithException() {
        SecurityContextHolder.set(TOKEN, buildLoginUser(List.of(RoleCodeConstants.ADMIN)));

        authInterceptor.afterCompletion(request, response, new Object(), new RuntimeException("boom"));

        assertThat(SecurityContextHolder.getLoginUser()).isNull();
    }

    /**
     * 构造测试 Controller 的 HandlerMethod。
     *
     * @param methodName 目标方法名
     * @return HandlerMethod 实例
     */
    private HandlerMethod handlerMethod(String methodName) {
        try {
            return new HandlerMethod(new TestController(), methodName);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("测试方法不存在: " + methodName, e);
        }
    }

    /**
     * 构造登录用户。
     *
     * @param roleCodes 角色编码列表
     * @return 登录用户
     */
    private LoginUser buildLoginUser(List<String> roleCodes) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(1L);
        loginUser.setUserNo("admin");
        loginUser.setUserName("管理员");
        loginUser.setRoleCodes(roleCodes);
        return loginUser;
    }
}
