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
 * @author 张竹灏
 * @date 2026/07/13
 */
@Component
public class ErpModeValidator {

    /** 当前允许的 ERP 数据源模式，其他值在应用启动阶段立即拒绝。 */
    private static final Set<String> SUPPORTED_MODES = Set.of("mock", "remote");

    /** 配置文件中的 ERP 模式原始值。 */
    private final String mode;

    /** Spring 环境信息，用于识别生产 Profile 并施加更严格约束。 */
    private final Environment environment;

    /** 构造启动校验器；未显式配置时模式默认为 mock。 */
    public ErpModeValidator(@Value("${mes.erp.mode:mock}") String mode,
                            Environment environment) {
        this.mode = mode;
        this.environment = environment;
    }

    /** 校验配置值和生产环境约束。 */
    @PostConstruct
    public void validate() {
        // 配置值按小写比较，但异常消息保留原值以便定位拼写问题。
        String normalizedMode = mode.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_MODES.contains(normalizedMode)) {
            throw new IllegalStateException("Unsupported mes.erp.mode: " + mode);
        }
        if ("mock".equals(normalizedMode)
                && environment.acceptsProfiles(Profiles.of("prod"))) {
            // 生产环境禁止静默回退演示数据，必须显式启用远程 ERP 客户端。
            throw new IllegalStateException("Production profile requires mes.erp.mode=remote");
        }
    }
}
