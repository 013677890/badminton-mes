package com.badminton.mes.module.wage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 计件工资数据库迁移关键约束测试。 */
class WageMigrationContractTest {

    @Test
    @DisplayName("结算数量合计：V11 将两列扩展为 decimal(18,4)")
    void settlementQuantityTotalsUseExpandedDecimalCapacity() throws IOException {
        String migration = readMigration("V11__wage_settlement_quantity_capacity.sql");

        assertThat(migration).contains(
                "`total_qualified_quantity` decimal(18,4)",
                "`total_defect_quantity` decimal(18,4)");
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
