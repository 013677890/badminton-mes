package com.badminton.mes.common.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 安全拦截配置：/api/** 默认全部要求登录，白名单仅登录接口。
 *
 * <p>新增免登录接口须在此显式登记，保持"默认拒绝"的安全基线(SEC-001)。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Configuration
public class SecurityWebConfig implements WebMvcConfigurer {

    /** 登录接口路径，唯一免登录白名单 */
    public static final String LOGIN_PATH = "/api/system/auth/login";

    private final AuthInterceptor authInterceptor;

    private final boolean authDisabled;

    /**
     * 构造器注入，依赖不可变。
     *
     * @param authInterceptor 登录鉴权拦截器
     */
    public SecurityWebConfig(AuthInterceptor authInterceptor,
                             @Value("${mes.security.auth-disabled:false}") boolean authDisabled) {
        this.authInterceptor = authInterceptor;
        this.authDisabled = authDisabled;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (authDisabled) {
            return;
        }

        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(LOGIN_PATH);
    }
}
