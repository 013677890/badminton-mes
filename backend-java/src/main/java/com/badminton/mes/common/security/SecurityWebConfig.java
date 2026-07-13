package com.badminton.mes.common.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
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

    private final List<String> allowedOrigins;

    /**
     * 构造器注入，依赖不可变。
     *
     * @param authInterceptor 登录鉴权拦截器
     * @param allowedOrigins  前端允许来源列表
     */
    public SecurityWebConfig(AuthInterceptor authInterceptor,
                             @Value("${mes.web.cors.allowed-origins:http://localhost:5173}")
                             List<String> allowedOrigins) {
        this.authInterceptor = authInterceptor;
        this.allowedOrigins = List.copyOf(allowedOrigins);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE)
                .allowCredentials(false)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(LOGIN_PATH);
    }
}
