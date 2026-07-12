package com.badminton.mes.module.production;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V13 生产组织基础资料迁移契约测试。
 *
 * <p>使用 H2 内嵌库（MySQL 兼容模式）实际执行 V13 迁移脚本，
 * 验证生成列、逻辑删除唯一键和审计字段真实创建，而非仅断言 SQL 字符串内容。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
class ProductionOrganizationMigrationContractTest {

    private static final String MIGRATION_PATH =
            "db/migration/V13__production_organization_master_data.sql";

    private static final String H2_URL =
            "jdbc:h2:mem:v13_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;IGNORE_UNKNOWN_SETTINGS=TRUE";

    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection(H2_URL);
        createPreV13Schema(connection);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    @DisplayName("V13：车间与产线生成列、唯一键和审计字段真实创建")
    void migrationCreatesOrganizationMasterDataContract() throws IOException, SQLException {
        executeMigration();

        assertThat(columnExists("base_workshop", "active_workshop_code"))
                .as("车间有效编码生成列").isTrue();
        assertThat(columnExists("base_production_line", "active_line_code"))
                .as("产线有效编码生成列").isTrue();
        assertThat(columnExists("base_workshop", "version"))
                .as("车间乐观锁版本列").isTrue();
        assertThat(columnExists("base_production_line", "version"))
                .as("产线乐观锁版本列").isTrue();
        assertThat(columnExists("base_workshop", "create_by"))
                .as("车间创建人审计列").isTrue();
        assertThat(columnExists("base_production_line", "create_by"))
                .as("产线创建人审计列").isTrue();

        assertThat(indexExists("base_workshop", "uk_active_workshop_code"))
                .as("车间有效编码唯一键").isTrue();
        assertThat(indexExists("base_production_line", "uk_active_line_code"))
                .as("产线有效编码唯一键").isTrue();
        assertThat(indexExists("base_workshop", "idx_workshop_code_deleted_id"))
                .as("车间编码查重索引").isTrue();
        assertThat(indexExists("base_production_line", "idx_line_workshop_status_id"))
                .as("车间产线查询索引").isTrue();
        assertThat(indexExists("base_workshop", "uk_workshop_code"))
                .as("旧车间编码唯一键应被删除").isFalse();
        assertThat(indexExists("base_production_line", "uk_line_code"))
                .as("旧产线编码唯一键应被删除").isFalse();

        verifyGeneratedColumnBehavior();
    }

    /**
     * 创建 V13 之前的基线表结构，模拟 V1/V5 已执行后的状态。
     *
     * @param conn H2 数据库连接
     * @throws SQLException 建表失败
     */
    private void createPreV13Schema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE `base_workshop` (
                      `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                      `workshop_code` varchar(32) NOT NULL,
                      `workshop_name` varchar(64) NOT NULL,
                      `manager_id` bigint unsigned NULL DEFAULT NULL,
                      `status` tinyint unsigned NOT NULL DEFAULT 1,
                      `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      `is_deleted` tinyint unsigned NOT NULL DEFAULT 0,
                      PRIMARY KEY (`id`),
                      UNIQUE KEY `uk_workshop_code` (`workshop_code`)
                    )
                    """);
            stmt.execute("""
                    CREATE TABLE `base_production_line` (
                      `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                      `line_code` varchar(32) NOT NULL,
                      `line_name` varchar(64) NOT NULL,
                      `workshop_id` bigint unsigned NOT NULL,
                      `standard_capacity` int unsigned NULL DEFAULT NULL,
                      `status` tinyint unsigned NOT NULL DEFAULT 1,
                      `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      `is_deleted` tinyint unsigned NOT NULL DEFAULT 0,
                      PRIMARY KEY (`id`),
                      UNIQUE KEY `uk_line_code` (`line_code`),
                      KEY `idx_workshop_id` (`workshop_id`)
                    )
                    """);
        }
    }

    /**
     * 读取 V13 迁移脚本，去除 H2 不支持的语法后逐条执行。
     *
     * <p>H2 不支持 MySQL 的多动作 ALTER TABLE（逗号分隔多个 ADD/MODIFY/DROP），
     * 需要拆分为单动作语句。同时移除 H2 不支持的索引 COMMENT 子句。
     *
     * @throws IOException  读取迁移文件失败
     * @throws SQLException 执行迁移失败
     */
    private void executeMigration() throws IOException, SQLException {
        String sql;
        try (var inputStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(MIGRATION_PATH)) {
            assertThat(inputStream).as("V13 migration must exist").isNotNull();
            sql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        // 移除行注释和 H2 不支持的 MySQL 特有语法
        sql = sql.replaceAll("--[^\\n]*", "")
                .replaceAll("COMMENT\\s+'[^']*'", "")
                .replaceAll("\\s+STORED\\b", "")
                .replaceAll("\\s+AFTER\\s+`[^`]+`", "");

        try (Statement stmt = connection.createStatement()) {
            for (String statement : sql.split(";")) {
                String trimmed = statement.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                for (String single : splitAlterTableActions(trimmed)) {
                    stmt.execute(single);
                }
            }
        }
    }

    /**
     * 将多动作 ALTER TABLE 拆分为单动作语句，适配 H2。
     *
     * @param statement 原始 SQL 语句
     * @return 拆分后的语句列表
     */
    private List<String> splitAlterTableActions(String statement) {
        String upper = statement.toUpperCase(Locale.ROOT).trim();
        if (!upper.startsWith("ALTER TABLE")) {
            return List.of(statement);
        }

        int firstBacktick = statement.indexOf('`');
        if (firstBacktick < 0) {
            return List.of(statement);
        }
        int secondBacktick = statement.indexOf('`', firstBacktick + 1);
        if (secondBacktick < 0) {
            return List.of(statement);
        }

        String prefix = statement.substring(0, secondBacktick + 1);
        String actionsPart = statement.substring(secondBacktick + 1).trim();
        if (actionsPart.isEmpty()) {
            return List.of(statement);
        }

        List<String> result = new ArrayList<>();
        for (String action : splitTopLevelCommas(actionsPart)) {
            String trimmedAction = action.trim();
            if (!trimmedAction.isEmpty()) {
                result.add(prefix + " " + trimmedAction);
            }
        }
        return result;
    }

    /**
     * 按顶层逗号（括号外）拆分 SQL 片段。
     *
     * @param sql SQL 片段
     * @return 拆分后的片段列表
     */
    private List<String> splitTopLevelCommas(String sql) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < sql.length(); i++) {
            char character = sql.charAt(i);
            if (character == '(') {
                depth++;
            } else if (character == ')') {
                depth--;
            } else if (character == ',' && depth == 0) {
                parts.add(sql.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(sql.substring(start));
        return parts;
    }

    /**
     * 验证生成列在未删除记录上生成编码、在已删除记录上生成 NULL。
     *
     * @throws SQLException 查询失败
     */
    private void verifyGeneratedColumnBehavior() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("INSERT INTO `base_workshop` "
                    + "(`workshop_code`, `workshop_name`, `status`, `is_deleted`, `create_by`, `update_by`) "
                    + "VALUES ('WS-ACTIVE', '活跃车间', 1, 0, 1, 1)");
            stmt.execute("INSERT INTO `base_workshop` "
                    + "(`workshop_code`, `workshop_name`, `status`, `is_deleted`, `create_by`, `update_by`) "
                    + "VALUES ('WS-DELETED', '已删除车间', 1, 1, 1, 1)");

            try (ResultSet rs = stmt.executeQuery(
                    "SELECT `active_workshop_code` FROM `base_workshop` "
                            + "WHERE `workshop_code` = 'WS-ACTIVE'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo("WS-ACTIVE");
            }
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT `active_workshop_code` FROM `base_workshop` "
                            + "WHERE `workshop_code` = 'WS-DELETED'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isNull();
            }
        }
    }

    /**
     * 判断指定表的列是否存在。
     *
     * @param tableName  表名
     * @param columnName 列名
     * @return true 表示列存在
     * @throws SQLException 查询失败
     */
    private boolean columnExists(String tableName, String columnName) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE LOWER(TABLE_NAME) = ? AND LOWER(COLUMN_NAME) = ?")) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /**
     * 判断指定表的索引或唯一约束是否存在。
     *
     * <p>H2 将唯一键存入 INFORMATION_SCHEMA.TABLE_CONSTRAINTS，普通索引存入 INDEXES，
     * 需要同时检查两张视图。
     *
     * @param tableName 表名
     * @param indexName 索引或约束名
     * @return true 表示存在
     * @throws SQLException 查询失败
     */
    private boolean indexExists(String tableName, String indexName) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES "
                        + "WHERE LOWER(TABLE_NAME) = ? AND LOWER(INDEX_NAME) = ?")) {
            ps.setString(1, tableName);
            ps.setString(2, indexName);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    return true;
                }
            }
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS "
                        + "WHERE LOWER(TABLE_NAME) = ? AND LOWER(CONSTRAINT_NAME) = ?")) {
            ps.setString(1, tableName);
            ps.setString(2, indexName);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }
}
