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
        String migration = readMigration("V8__integration_write_api.sql");

        assertThat(migration).contains("COALESCE(`source_system`, '')");
        assertThat(migration).contains(
                "(`source_type`, `source_system_key`, `source_order_no`)");
        assertThat(migration).doesNotContain(
                "(`source_type`, `source_system`, `source_order_no`)");
    }

    @Test
    @DisplayName("接口写入日志：包含创建和更新时间审计字段")
    void integrationWriteLogContainsRequiredAuditTimes() throws IOException {
        String createMigration = readMigration("V8__integration_write_api.sql");
        String auditMigration = readMigration("V10__integration_write_log_audit.sql");

        assertThat(createMigration).contains(
                "`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'");
        assertThat(auditMigration).contains("ADD COLUMN `update_time` datetime NOT NULL");
        assertThat(auditMigration).contains(
                "ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `create_time`");
    }

    /**
     * 读取数据库迁移脚本。
     *
     * @param migrationFileName 迁移文件名
     * @return 迁移脚本文本
     * @throws IOException 读取失败
     */
    private String readMigration(String migrationFileName) throws IOException {
        try (var inputStream = getClass().getResourceAsStream(
                "/db/migration/" + migrationFileName)) {
            assertThat(inputStream).isNotNull();
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
