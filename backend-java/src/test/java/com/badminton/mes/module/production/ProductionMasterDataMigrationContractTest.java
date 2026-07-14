package com.badminton.mes.module.production;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V12 生产基础资料迁移关键约束测试。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
class ProductionMasterDataMigrationContractTest {

    @Test
    @DisplayName("逻辑删除唯一性：产品、物料与 BOM 使用有效记录生成列唯一键")
    void activeBusinessKeysUseGeneratedColumnUniqueIndexes() throws IOException {
        String migration = readMigration();

        assertThat(migration).contains(
                "`active_product_code` varchar(32) GENERATED ALWAYS AS",
                "`active_material_code` varchar(32) GENERATED ALWAYS AS",
                "`active_bom_code` varchar(32) GENERATED ALWAYS AS",
                "`active_product_version` varchar(80) GENERATED ALWAYS AS",
                "`active_bom_material` varchar(80) GENERATED ALWAYS AS");
        assertThat(migration).contains(
                "UNIQUE KEY `uk_active_product_code`",
                "UNIQUE KEY `uk_active_material_code`",
                "UNIQUE KEY `uk_active_bom_code`",
                "UNIQUE KEY `uk_active_product_version`",
                "UNIQUE KEY `uk_active_bom_material`");
    }

    /**
     * 读取 V12 迁移脚本。
     *
     * @return 迁移脚本文本
     * @throws IOException 读取失败
     */
    private String readMigration() throws IOException {
        try (var inputStream = getClass().getResourceAsStream(
                "/db/migration/V12__production_master_data.sql")) {
            assertThat(inputStream).isNotNull();
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
