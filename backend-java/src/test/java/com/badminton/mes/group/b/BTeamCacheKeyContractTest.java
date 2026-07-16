package com.badminton.mes.group.b;

import com.badminton.mes.module.barcode.dal.redis.BarcodeRedisKeyConstants;
import com.badminton.mes.module.report.dal.redis.KanbanRedisKeyConstants;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** B 组条码流水与看板快照缓存 Key、TTL 契约测试。 @author 范家权 */
class BTeamCacheKeyContractTest {

    @Test
    void barcodeSerialKeySeparatesRuleAndScope() {
        assertThat(BarcodeRedisKeyConstants.barcodeSerialKey(7L, "20260716:P-01"))
                .isEqualTo("mes:barcode:serial:7:20260716:P-01");
        assertThat(BarcodeRedisKeyConstants.barcodeSerialKey(8L, "20260716:P-01"))
                .isNotEqualTo(BarcodeRedisKeyConstants.barcodeSerialKey(7L, "20260716:P-01"));
    }

    @Test
    void barcodeResetTtlsCoverTwoResetPeriods() {
        assertThat(BarcodeRedisKeyConstants.SERIAL_DAILY_TTL).isEqualTo(Duration.ofDays(2));
        assertThat(BarcodeRedisKeyConstants.SERIAL_MONTHLY_TTL).isEqualTo(Duration.ofDays(62));
    }

    @Test
    void kanbanGlobalAndScopedSnapshotsNeverCollide() {
        assertThat(KanbanRedisKeyConstants.snapshotKey("line", null))
                .isEqualTo("report:kanban:snapshot:line:all");
        assertThat(KanbanRedisKeyConstants.snapshotKey("line", 1L))
                .isEqualTo("report:kanban:snapshot:line:1")
                .isNotEqualTo(KanbanRedisKeyConstants.snapshotKey("workshop", 1L));
    }

    @Test
    void kanbanSnapshotTtlMatchesShortLivedDashboardContract() {
        assertThat(KanbanRedisKeyConstants.SNAPSHOT_TTL).isEqualTo(Duration.ofSeconds(90));
    }
}
