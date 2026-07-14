package com.badminton.mes.common.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API 安全与跨域配置。
 *
 * <p>认证默认启用，仅登录接口免认证；受控开发环境可通过配置显式关闭认证。
 */
@Configuration
public class SecurityWebConfig implements WebMvcConfigurer {

    /** 登录接口路径，唯一免登录白名单。 */
    public static final String LOGIN_PATH = "/api/system/auth/login";

    private final AuthInterceptor authInterceptor;
    private final List<String> allowedOrigins;
    private final boolean authDisabled;

    public SecurityWebConfig(
            AuthInterceptor authInterceptor,
            @Value("${mes.web.cors.allowed-origins:http://localhost:5173}") List<String> allowedOrigins,
            @Value("${mes.security.auth-disabled:false}") boolean authDisabled) {
        this.authInterceptor = authInterceptor;
        this.allowedOrigins = List.copyOf(allowedOrigins);
        this.authDisabled = authDisabled;
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
        if (authDisabled) {
            return;
        }
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(LOGIN_PATH);
    }
}
