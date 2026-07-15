package com.badminton.mes.module.report.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import com.badminton.mes.module.report.dal.ReportQueryRepository;
import com.badminton.mes.module.report.service.dto.DefectSourceRecord;
import com.badminton.mes.module.report.service.dto.ReportQueryCriteria;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * C 组质量不良来源适配测试。
 *
 * @author 刘涵
 * @date 2026/07/14
 */
class QualityDefectSourceProviderTest {

    @Test
    void providerReturnsRepositoryQualityFactsWithoutFallbackWarning() {
        ReportQueryRepository repository = mock(ReportQueryRepository.class);
        QualityDefectSourceProvider provider = new QualityDefectSourceProvider(repository);
        ReportQueryCriteria criteria = new ReportQueryCriteria(LocalDateTime.now().minusDays(1),
                LocalDateTime.now(), null, null, null, null, null, null, null, null, null);
        DefectSourceRecord record = new DefectSourceRecord("QUALITY_INSPECTION", 1L, null,
                "D001", 2L, "TASK", "WO", 3L, "产品", "BATCH", 4L, 5L,
                6L, "工序", "REWORK", "外观破损", 3L, 0L, LocalDateTime.now());
        when(repository.listQualityDefects(criteria, 101)).thenReturn(List.of(record));

        var result = provider.load(criteria, 101);

        assertThat(result.records()).containsExactly(record);
        assertThat(result.warnings()).isEmpty();
    }
}
