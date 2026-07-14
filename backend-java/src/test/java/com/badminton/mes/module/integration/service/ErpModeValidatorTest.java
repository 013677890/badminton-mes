package com.badminton.mes.module.integration.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ErpModeValidator} 单元测试。
 *
 * @author Codex
 * @date 2026/07/13
 */
class ErpModeValidatorTest {

    @Test
    void rejectsUnknownMode() {
        ErpModeValidator validator = new ErpModeValidator("remtoe", new MockEnvironment());

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unsupported");
    }

    @Test
    void rejectsMockModeInProduction() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        ErpModeValidator validator = new ErpModeValidator("mock", environment);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Production");
    }

    @Test
    void acceptsRemoteModeInProduction() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        ErpModeValidator validator = new ErpModeValidator("remote", environment);

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }
}
