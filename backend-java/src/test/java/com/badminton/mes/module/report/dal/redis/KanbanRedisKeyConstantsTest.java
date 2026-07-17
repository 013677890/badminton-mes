package com.badminton.mes.module.report.dal.redis;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 看板快照缓存键的范围隔离和过期策略契约测试。
 *
 * @author 范家权
 */
class KanbanRedisKeyConstantsTest {

    @Test
    void nullScopeIdUsesStableAllMarker() {
        assertThat(KanbanRedisKeyConstants.snapshotKey("global", null))
                .isEqualTo("report:kanban:snapshot:global:all");
    }

    @Test
    void workshopAndLineScopesUseIndependentNamespaces() {
        assertThat(KanbanRedisKeyConstants.snapshotKey("workshop", 12L))
                .isEqualTo("report:kanban:snapshot:workshop:12");
        assertThat(KanbanRedisKeyConstants.snapshotKey("line", 12L))
                .isEqualTo("report:kanban:snapshot:line:12");
    }

    @Test
    void snapshotTtlRemainsNinetySeconds() {
        assertThat(KanbanRedisKeyConstants.SNAPSHOT_TTL).isEqualTo(Duration.ofSeconds(90));
    }
}
