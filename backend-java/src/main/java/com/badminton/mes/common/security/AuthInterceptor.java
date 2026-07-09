package com.badminton.mes.common.security;

import java.util.List;

import com.badminton.mes.common.core.GlobalErrorCodeConstants;
import com.badminton.mes.common.exception.ServiceException;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 登录鉴权拦截器。
 *
 * <p>preHandle 按序执行：解析 Bearer token → Redis 还原会话(含滑动续期)→
 * 校验 {@link RequiresRoles} → 写入 {@link SecurityContextHolder}。
 * 角色校验放在写上下文之前：preHandle 抛异常时容器不回调本拦截器的
 * afterCompletion，先写后抛会在线程池中残留脏上下文。
 *
 * <p>鉴权失败抛 {@code ServiceException}，由 GlobalExceptionHandler 统一
 * 映射为 401(A0230)/403(A0301) 的四要素响应(API-003)。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** Authorization 请求头的 Bearer 方案前缀 */
    public static final String BEARER_PREFIX = "Bearer ";

    private final LoginSessionReader loginSessionReader;

    /**
     * 构造器注入，依赖不可变。
     *
     * @param loginSessionReader 会话读取接口，system 模块提供实现
     */
    public AuthInterceptor(LoginSessionReader loginSessionReader) {
        this.loginSessionReader = loginSessionReader;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 静态资源等非 Controller 处理器不做登录控制
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        String token = resolveBearerToken(request);
        if (!StringUtils.hasText(token)) {
            throw new ServiceException(GlobalErrorCodeConstants.UNAUTHORIZED);
        }
        LoginUser loginUser = loginSessionReader.resolve(token)
                .orElseThrow(() -> new ServiceException(GlobalErrorCodeConstants.UNAUTHORIZED));
        checkRoles(handlerMethod, loginUser);
        SecurityContextHolder.set(token, loginUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        SecurityContextHolder.clear();
    }

    /**
     * 从 Authorization 头解析 Bearer token。
     *
     * @param request 当前请求
     * @return token，头缺失或非 Bearer 方案时为 null
     */
    private String resolveBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return header.substring(BEARER_PREFIX.length()).trim();
    }

    /**
     * 校验角色注解：方法级优先于类级，任一角色命中即放行。
     *
     * @param handlerMethod 目标处理方法
     * @param loginUser     登录用户
     * @throws ServiceException 声明了角色且全部未命中时抛 A0301
     */
    private void checkRoles(HandlerMethod handlerMethod, LoginUser loginUser) {
        RequiresRoles requiresRoles = handlerMethod.getMethodAnnotation(RequiresRoles.class);
        if (requiresRoles == null) {
            requiresRoles = handlerMethod.getBeanType().getAnnotation(RequiresRoles.class);
        }
        if (requiresRoles == null) {
            return;
        }
        List<String> ownedRoles = loginUser.getRoleCodes() == null ? List.of() : loginUser.getRoleCodes();
        for (String required : requiresRoles.value()) {
            if (ownedRoles.contains(required)) {
                return;
            }
        }
        throw new ServiceException(GlobalErrorCodeConstants.FORBIDDEN);
    }
}
