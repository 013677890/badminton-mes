package com.badminton.mes.stress;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.report.dal.redis.KanbanRedisKeyConstants;
import com.badminton.mes.module.report.service.dto.DefectSourceBatch;
import com.badminton.mes.module.report.service.dto.DefectSourceRecord;
import com.badminton.mes.module.report.service.dto.ReportQueryCriteria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * 报表数据对象和看板缓存键的进程内并发压力测试。
 *
 * @author 范家权
 */
@Tag("stress")
@Timeout(90)
class ReportConcurrencyStressTest {

    private static final int OPERATIONS = Integer.getInteger("mes.stress.operations", 10_000);
    private static final LocalDateTime START_TIME = LocalDateTime.of(2026, 7, 17, 8, 0);

    @Test
    @DisplayName("缺陷来源净数量并发计算保持隔离")
    void defectNetQuantityCalculation() throws Exception {
        ConcurrentStressRunner.run("report-defect-net-quantity", OPERATIONS, index -> {
            long occurrence = index + 10L;
            long reversal = index % 7L;
            DefectSourceRecord record = record(index, occurrence, reversal);

            if (record.netQuantity() != occurrence - reversal) {
                throw new AssertionError("defect net quantity mismatch at " + index);
            }
        });
    }

    @Test
    @DisplayName("缺陷批次并发构造保持防御性复制")
    void defectBatchDefensiveCopy() throws Exception {
        ConcurrentStressRunner.run("report-defect-batch-copy", OPERATIONS, index -> {
            List<DefectSourceRecord> records = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            records.add(record(index, index + 1L, 1L));
            warnings.add("warning-" + index);

            DefectSourceBatch batch = new DefectSourceBatch(records, warnings);
            records.clear();
            warnings.clear();

            if (batch.records().size() != 1
                    || !batch.warnings().equals(List.of("warning-" + index))) {
                throw new AssertionError("defect batch was changed by source mutation at " + index);
            }
        });
    }

    @Test
    @DisplayName("看板全局车间产线缓存键并发生成无串扰")
    void kanbanSnapshotKeyGeneration() throws Exception {
        ConcurrentStressRunner.run("report-kanban-key", OPERATIONS, index -> {
            String scopeType;
            Long scopeId;
            String expected;
            switch (index % 3) {
                case 0 -> {
                    scopeType = "global";
                    scopeId = null;
                    expected = "report:kanban:snapshot:global:all";
                }
                case 1 -> {
                    scopeType = "workshop";
                    scopeId = (long) index;
                    expected = "report:kanban:snapshot:workshop:" + index;
                }
                default -> {
                    scopeType = "line";
                    scopeId = (long) index;
                    expected = "report:kanban:snapshot:line:" + index;
                }
            }

            if (!KanbanRedisKeyConstants.snapshotKey(scopeType, scopeId).equals(expected)) {
                throw new AssertionError("kanban snapshot key mismatch at " + index);
            }
        });
    }

    @Test
    @DisplayName("报表查询条件并发构造和读取保持字段一致")
    void reportQueryCriteriaConstruction() throws Exception {
        ConcurrentStressRunner.run("report-query-criteria", OPERATIONS, index -> {
            long scopeId = index + 1L;
            ReportQueryCriteria criteria = new ReportQueryCriteria(
                    START_TIME, START_TIME.plusHours(8), scopeId, scopeId + 1,
                    scopeId + 2, scopeId + 3, scopeId + 4, scopeId + 5,
                    scopeId + 6, "BATCH-" + index, index % 4);

            if (!criteria.startTime().equals(START_TIME)
                    || !criteria.endTime().equals(START_TIME.plusHours(8))
                    || criteria.workshopId() != scopeId
                    || criteria.lineId() != scopeId + 1
                    || !criteria.batchNo().equals("BATCH-" + index)
                    || criteria.status() != index % 4) {
                throw new AssertionError("report criteria mismatch at " + index);
            }
        });
    }

    private static DefectSourceRecord record(int index, long occurrence, long reversal) {
        return new DefectSourceRecord("SCENE_REPORT", (long) index, null,
                "GROUP-" + index, (long) index, "TASK-" + index,
                "WO-" + index, (long) index, "羽毛球", "BATCH-" + index,
                1L, 2L, 3L, "穿线", "DEFECT-" + index, "断线",
                occurrence, reversal, START_TIME.plusSeconds(index));
    }
}
