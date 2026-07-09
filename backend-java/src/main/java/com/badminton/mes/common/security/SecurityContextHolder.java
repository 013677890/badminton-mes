package com.badminton.mes.common.security;

import com.badminton.mes.common.core.GlobalErrorCodeConstants;
import com.badminton.mes.common.exception.ServiceException;

/**
 * 登录上下文，基于 ThreadLocal 保存当前请求的 token 与登录用户。
 *
 * <p>由 {@link AuthInterceptor} 在校验通过后写入、afterCompletion 中清理；
 * 请求线程可能被容器复用，禁止在拦截器之外调用 {@link #set}(并发规约：
 * ThreadLocal 用完必须回收，防止脏读与内存泄漏)。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public final class SecurityContextHolder {

    private static final ThreadLocal<LoginSession> CONTEXT = new ThreadLocal<>();

    /** 当前请求会话：token 供登出时定位 Redis 键，loginUser 供业务取操作人 */
    private record LoginSession(String token, LoginUser loginUser) {
    }

    /**
     * 写入当前请求的登录上下文，仅供拦截器调用。
     *
     * @param token     登录令牌
     * @param loginUser 登录用户
     */
    public static void set(String token, LoginUser loginUser) {
        CONTEXT.set(new LoginSession(token, loginUser));
    }

    /**
     * 读取当前登录用户。
     *
     * @return 登录用户，白名单接口等未登录场景为 null
     */
    public static LoginUser getLoginUser() {
        LoginSession session = CONTEXT.get();
        return session == null ? null : session.loginUser();
    }

    /**
     * 读取当前登录用户，缺失视为登录已失效。
     *
     * @return 登录用户
     * @throws ServiceException 上下文为空时抛 A0230
     */
    public static LoginUser getRequiredLoginUser() {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null) {
            throw new ServiceException(GlobalErrorCodeConstants.UNAUTHORIZED);
        }
        return loginUser;
    }

    /**
     * 读取当前登录用户 id，业务落库 create_by / operate_by 使用。
     *
     * @return 登录用户主键
     * @throws ServiceException 上下文为空时抛 A0230
     */
    public static Long getRequiredLoginUserId() {
        return getRequiredLoginUser().getUserId();
    }

    /**
     * 读取当前请求的登录令牌，登出接口定位会话使用。
     *
     * @return 登录令牌，未登录场景为 null
     */
    public static String getToken() {
        LoginSession session = CONTEXT.get();
        return session == null ? null : session.token();
    }

    /**
     * 清理上下文，拦截器 afterCompletion 中必须调用。
     */
    public static void clear() {
        CONTEXT.remove();
    }

    private SecurityContextHolder() {
    }
}
