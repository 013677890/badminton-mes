package com.badminton.mes.module.production.dal.repository;

import com.badminton.mes.module.production.dal.repository.CraftRoutingRelationRepository.RoutingRelationSnapshot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 工艺路线关系 Repository 的外部 MySQL 集成测试。
 *
 * @author 刘涵
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CraftRoutingRelationRepositoryIntegrationTest {

    private static final Long PROCESS_ID = 9_100_001L;
    private static final Long SOP_ID = 9_100_002L;
    private static final Long ROUTING_ID = 9_100_003L;
    private static final Long PRODUCT_ID = 9_100_004L;

    @Autowired
    private CraftRoutingRelationRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpContractData() {
        jdbcTemplate.update("""
                INSERT INTO craft_process
                    (id, process_code, process_name, status, is_deleted)
                VALUES (?, ?, ?, 1, 0)
                """, PROCESS_ID, "M0-PROCESS", "M0验证工序");
        jdbcTemplate.update("""
                INSERT INTO craft_sop
                    (id, sop_code, sop_name, version, effect_date, sop_status, create_by, is_deleted)
                VALUES (?, ?, ?, 'V1', CURRENT_DATE, 1, 1, 0)
                """, SOP_ID, "M0-SOP", "M0验证SOP");
        jdbcTemplate.update("""
                INSERT INTO craft_routing
                    (id, routing_code, routing_name, version, routing_status, create_by, is_deleted)
                VALUES (?, ?, ?, 'V1', 1, 1, 0)
                """, ROUTING_ID, "M0-ROUTING", "M0验证路线");
        jdbcTemplate.update("""
                INSERT INTO craft_routing_detail
                    (routing_id, seq, process_id, sop_id, is_deleted)
                VALUES (?, 1, ?, ?, 0)
                """, ROUTING_ID, PROCESS_ID, SOP_ID);
        jdbcTemplate.update("""
                INSERT INTO craft_routing_product
                    (routing_id, product_id, is_default, is_deleted)
                VALUES (?, ?, 1, 0)
                """, ROUTING_ID, PRODUCT_ID);
    }

    @Test
    @DisplayName("真实数据库：有效路线、产品、工序和SOP关系可被完整读取")
    void findsValidRoutingRelationFromMysql() {
        RoutingRelationSnapshot snapshot = repository.findRelationSnapshot(ROUTING_ID, PRODUCT_ID);

        assertThat(snapshot.routingAvailable()).isTrue();
        assertThat(snapshot.productBound()).isTrue();
        assertThat(snapshot.detailCount()).isEqualTo(1);
        assertThat(snapshot.minimumSequence()).isEqualTo(1);
        assertThat(snapshot.maximumSequence()).isEqualTo(1);
        assertThat(snapshot.unavailableProcessCount()).isZero();
        assertThat(snapshot.unavailableSopCount()).isZero();
    }

    @Test
    @DisplayName("真实数据库：停用SOP会被原生查询识别为不可用")
    void detectsDisabledSopFromMysql() {
        jdbcTemplate.update("UPDATE craft_sop SET sop_status = 0 WHERE id = ?", SOP_ID);

        RoutingRelationSnapshot snapshot = repository.findRelationSnapshot(ROUTING_ID, PRODUCT_ID);

        assertThat(snapshot.unavailableSopCount()).isEqualTo(1);
    }
}
