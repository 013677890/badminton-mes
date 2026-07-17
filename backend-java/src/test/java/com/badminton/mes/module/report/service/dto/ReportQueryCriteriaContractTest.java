package com.badminton.mes.module.report.service.dto;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 报表查询条件的字段传递、可选条件和值对象语义测试。
 *
 * @author 范家权
 */
class ReportQueryCriteriaContractTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 7, 1, 8, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 7, 1, 20, 0);

    @Test
    void preservesEveryFilterDimension() {
        ReportQueryCriteria criteria = criteria("BATCH-1", 1);

        assertThat(criteria.startTime()).isEqualTo(START);
        assertThat(criteria.endTime()).isEqualTo(END);
        assertThat(criteria.workshopId()).isEqualTo(1L);
        assertThat(criteria.lineId()).isEqualTo(2L);
        assertThat(criteria.productId()).isEqualTo(3L);
        assertThat(criteria.workOrderId()).isEqualTo(4L);
        assertThat(criteria.taskId()).isEqualTo(5L);
        assertThat(criteria.processId()).isEqualTo(6L);
        assertThat(criteria.shiftId()).isEqualTo(7L);
        assertThat(criteria.batchNo()).isEqualTo("BATCH-1");
        assertThat(criteria.status()).isEqualTo(1);
    }

    @Test
    void acceptsAbsentOptionalBatchAndStatusFilters() {
        ReportQueryCriteria criteria = criteria(null, null);

        assertThat(criteria.batchNo()).isNull();
        assertThat(criteria.status()).isNull();
    }

    @Test
    void equalValuesHaveRecordEqualityAndStableHashCode() {
        ReportQueryCriteria first = criteria("BATCH-1", 1);
        ReportQueryCriteria second = criteria("BATCH-1", 1);

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    private static ReportQueryCriteria criteria(String batchNo, Integer status) {
        return new ReportQueryCriteria(START, END, 1L, 2L, 3L, 4L, 5L,
                6L, 7L, batchNo, status);
    }
}
