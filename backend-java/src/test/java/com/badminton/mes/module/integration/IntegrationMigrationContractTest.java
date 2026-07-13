package com.badminton.mes.module.integration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 外部接口数据库迁移关键约束测试。
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

    @Test
    @DisplayName("ERP 工艺暂存：来源系统、路线编码和版本由唯一键防重")
    void erpCraftPendingUsesSourceVersionUniqueKey() throws IOException {
        String migration = readMigration("V14__erp_craft_pending.sql");

        assertThat(migration).contains("UNIQUE KEY `uk_source_code_version`");
        assertThat(migration).contains(
                "(`source_system`, `erp_routing_code`, `erp_routing_version`)");
    }

    @Test
    @DisplayName("ERP 工艺种子：仅跳过已存在工序且不忽略其他 SQL 错误")
    void erpCraftSeedDataUsesExplicitExistenceChecks() throws IOException {
        String migration = readMigration("V14__erp_craft_pending.sql");

        assertThat(migration).doesNotContain("INSERT IGNORE");
        assertThat(migration).contains("WHERE NOT EXISTS (");
        assertThat(migration).contains(
                "SELECT 1 FROM `craft_process` WHERE `process_code` = 'PR001'");
        assertThat(migration).contains(
                "SELECT 1 FROM `craft_process` WHERE `process_code` = 'PR005'");
    }

    @Test
    @DisplayName("设备计数：成功记录、异常池和接口专用幂等唯一键同时落地")
    void deviceCountMigrationContainsRecordsExceptionsAndIdempotencyKey() throws IOException {
        String migration = readMigration("V15__device_count_record_exception.sql");

        assertThat(migration).contains("CREATE TABLE `integration_device_count_record`");
        assertThat(migration).contains("CREATE TABLE `integration_device_count_exception`");
        assertThat(migration).contains("UNIQUE KEY `uk_device_count_idempotency_key`");
        assertThat(migration).contains(
                "CASE WHEN `interface_type` = 'DEVICE_COUNT_WRITE'");
        assertThat(migration).contains("CHARACTER SET ascii COLLATE ascii_bin");
        assertThat(migration).contains(
                "UPPER(CONCAT(`source_system`, '#', `business_key`))");
        assertThat(migration).contains("KEY `idx_device_count_context_id`");
    }

    @Test
    @DisplayName("完工读取：完工单审核索引和逐条读取日志表同时落地")
    void completionReadMigrationContainsApprovedIndexAndReadLog() throws IOException {
        String migration = readMigration("V16__completion_order_read_log.sql");

        assertThat(migration).contains("CREATE TABLE `prod_completion_order`");
        assertThat(migration).contains("CREATE TABLE `integration_completion_read_log`");
        assertThat(migration).contains("UNIQUE KEY `uk_completion_no`");
        assertThat(migration).contains("KEY `idx_completion_audit_time_id`");
        assertThat(migration).contains("KEY `idx_completion_read_source_time_id`");
        assertThat(migration).contains("CONSTRAINT `chk_completion_approved_audit_time`");
        assertThat(migration).contains(
                "CHECK (`audit_status` <> 1 OR `audit_time` IS NOT NULL)");
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
