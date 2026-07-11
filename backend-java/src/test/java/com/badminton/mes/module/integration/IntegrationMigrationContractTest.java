package com.badminton.mes.module.integration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V8 外部写入接口迁移关键约束测试。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
class IntegrationMigrationContractTest {

    @Test
    @DisplayName("外部工单唯一键：NULL 来源系统通过生成列参与防重")
    void externalWorkOrderUniqueKeyNormalizesNullSourceSystem() throws IOException {
        String migration;
        try (var inputStream = getClass().getResourceAsStream(
                "/db/migration/V8__integration_write_api.sql")) {
            assertThat(inputStream).isNotNull();
            migration = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(migration).contains("COALESCE(`source_system`, '')");
        assertThat(migration).contains(
                "(`source_type`, `source_system_key`, `source_order_no`)");
        assertThat(migration).doesNotContain(
                "(`source_type`, `source_system`, `source_order_no`)");
    }
}
