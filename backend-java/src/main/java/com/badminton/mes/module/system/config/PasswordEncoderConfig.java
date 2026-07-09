package com.badminton.mes.module.system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码编码器配置：BCrypt 加盐哈希，满足 sys_user.password"加密存储"设计。
 *
 * <p>项目仅引入 spring-security-crypto 工具包，无 Spring Security 过滤器链。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * BCrypt 密码编码器，默认强度 10。
     *
     * @return 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
