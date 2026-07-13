package com.badminton.mes.module.integration.service;

import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * ERP 数据源模式启动校验，防止生产环境静默使用演示数据。
 *
 * @author Codex
 * @date 2026/07/13
 */
@Component
public class ErpModeValidator {

    private static final Set<String> SUPPORTED_MODES = Set.of("mock", "remote");

    private final String mode;
    private final Environment environment;

    public ErpModeValidator(@Value("${mes.erp.mode:mock}") String mode,
                            Environment environment) {
        this.mode = mode;
        this.environment = environment;
    }

    /** 校验配置值和生产环境约束。 */
    @PostConstruct
    public void validate() {
        String normalizedMode = mode.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_MODES.contains(normalizedMode)) {
            throw new IllegalStateException("Unsupported mes.erp.mode: " + mode);
        }
        if ("mock".equals(normalizedMode)
                && environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException("Production profile requires mes.erp.mode=remote");
        }
    }
}
