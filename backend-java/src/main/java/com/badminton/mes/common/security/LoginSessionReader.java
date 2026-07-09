package com.badminton.mes.common.security;

import java.util.Optional;

/**
 * 登录会话读取接口，供 {@link AuthInterceptor} 按 token 还原登录用户。
 *
 * <p>接口定义在 common、由 system 模块的 Redis DAO 实现，
 * 避免 common 反向依赖业务模块(工程结构分层约定)。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public interface LoginSessionReader {

    /**
     * 按 token 读取会话，命中且剩余有效期不足一半时滑动续期。
     *
     * @param token 登录令牌
     * @return 会话中的登录用户，token 无效或已过期时为空
     */
    Optional<LoginUser> resolve(String token);
}
