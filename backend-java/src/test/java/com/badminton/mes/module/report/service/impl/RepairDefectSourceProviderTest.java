package com.badminton.mes.module.report.service.impl;

import com.badminton.mes.module.report.service.dto.ReportQueryCriteria;
import com.badminton.mes.module.scene.dal.entity.SceneProductionTaskEntity;
import com.badminton.mes.module.scene.dal.entity.SceneRepairWorkOrderEntity;
import com.badminton.mes.module.scene.dal.repository.SceneProductionTaskRepository;
import com.badminton.mes.module.scene.dal.repository.SceneRepairWorkOrderRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 返修复检不良来源的口径、范围过滤与数量上限测试。 @author 范家权 */
class RepairDefectSourceProviderTest {

    @Test
    void continueRepairAndScrappedResultsContributeOccurrenceQuantity() {
        SceneRepairWorkOrderRepository repairs = mock(SceneRepairWorkOrderRepository.class);
        SceneProductionTaskRepository tasks = mock(SceneProductionTaskRepository.class);
        SceneRepairWorkOrderEntity continueRepair = repair(1L, "CONTINUE_REPAIR", 4);
        SceneRepairWorkOrderEntity scrapped = repair(2L, "SCRAPPED", 3);
        when(repairs.findAllByDeletedFalseOrderByCreatedTimeAsc())
                .thenReturn(List.of(continueRepair, scrapped));
        when(tasks.findByIdAndDeletedFalse(101L)).thenReturn(Optional.of(task()));

        var batch = new RepairDefectSourceProvider(repairs, tasks).load(criteria(), 10);

        assertThat(batch.records()).extracting("sourceType", "occurrenceQuantity")
                .containsExactly(tuple("REPAIR_RECHECK", 4L), tuple("REPAIR_RECHECK", 3L));
        assertThat(batch.records()).allSatisfy(record -> {
            assertThat(record.defectGroupNo()).startsWith("REPAIR:");
            assertThat(record.netQuantity()).isEqualTo(record.occurrenceQuantity());
        });
        assertThat(batch.warnings()).isEmpty();
    }

    @Test
    void passedRecheckIsRetainedWithZeroDefectOccurrence() {
        SceneRepairWorkOrderRepository repairs = mock(SceneRepairWorkOrderRepository.class);
        SceneProductionTaskRepository tasks = mock(SceneProductionTaskRepository.class);
        when(repairs.findAllByDeletedFalseOrderByCreatedTimeAsc())
                .thenReturn(List.of(repair(1L, "PASSED", 5)));
        when(tasks.findByIdAndDeletedFalse(101L)).thenReturn(Optional.of(task()));

        var record = new RepairDefectSourceProvider(repairs, tasks)
                .load(criteria(), 10).records().getFirst();

        assertThat(record.occurrenceQuantity()).isZero();
        assertThat(record.defectName()).isEqualTo("PASSED");
    }

    @Test
    void filtersMissingTasksOutOfScopeRowsAndHonorsLimit() {
        SceneRepairWorkOrderRepository repairs = mock(SceneRepairWorkOrderRepository.class);
        SceneProductionTaskRepository tasks = mock(SceneProductionTaskRepository.class);
        SceneRepairWorkOrderEntity missingTask = repair(1L, "SCRAPPED", 1);
        missingTask.setTaskId(999L);
        SceneRepairWorkOrderEntity first = repair(2L, "SCRAPPED", 2);
        SceneRepairWorkOrderEntity second = repair(3L, "SCRAPPED", 3);
        when(repairs.findAllByDeletedFalseOrderByCreatedTimeAsc())
                .thenReturn(List.of(missingTask, first, second));
        when(tasks.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());
        when(tasks.findByIdAndDeletedFalse(101L)).thenReturn(Optional.of(task()));

        var records = new RepairDefectSourceProvider(repairs, tasks).load(criteria(), 1).records();

        assertThat(records).singleElement().satisfies(record -> assertThat(record.sourceId()).isEqualTo(2L));
    }

    private static ReportQueryCriteria criteria() {
        return new ReportQueryCriteria(LocalDateTime.of(2026, 7, 17, 0, 0),
                LocalDateTime.of(2026, 7, 17, 23, 59), 10L, 20L, 30L,
                null, 101L, null, null, "BATCH-1", null);
    }

    private static SceneRepairWorkOrderEntity repair(Long id, String result, int quantity) {
        SceneRepairWorkOrderEntity repair = new SceneRepairWorkOrderEntity();
        repair.setId(id);
        repair.setTaskId(101L);
        repair.setBatchNo("BATCH-1");
        repair.setRecheckResult(result);
        repair.setRecheckQuantity(quantity);
        repair.setUpdatedTime(LocalDateTime.of(2026, 7, 17, 10, 0));
        return repair;
    }

    private static SceneProductionTaskEntity task() {
        SceneProductionTaskEntity task = new SceneProductionTaskEntity();
        task.setId(101L);
        task.setTaskNo("TASK-101");
        task.setWorkOrderNo("WO-101");
        task.setProductId(30L);
        task.setProductName("羽毛球");
        task.setWorkshopId(10L);
        task.setLineId(20L);
        return task;
    }

    private static org.assertj.core.groups.Tuple tuple(Object... values) {
        return org.assertj.core.groups.Tuple.tuple(values);
    }
}
