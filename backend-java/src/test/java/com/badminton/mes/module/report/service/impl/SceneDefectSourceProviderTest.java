package com.badminton.mes.module.report.service.impl;

import com.badminton.mes.module.report.dal.ReportQueryRepository;
import com.badminton.mes.module.report.service.dto.DefectSourceRecord;
import com.badminton.mes.module.report.service.dto.ReportQueryCriteria;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 生产报工不良来源委托和降级警告契约测试。 @author 范家权 */
class SceneDefectSourceProviderTest {

    @Test
    void delegatesStableCriteriaAndLimitWithoutInventingWarnings() {
        ReportQueryRepository repository = mock(ReportQueryRepository.class);
        ReportQueryCriteria criteria = new ReportQueryCriteria(
                LocalDateTime.of(2026, 7, 17, 0, 0),
                LocalDateTime.of(2026, 7, 17, 23, 59),
                1L, 2L, 3L, 4L, 5L, 6L, 7L, "B-1", 1);
        DefectSourceRecord record = new DefectSourceRecord("SCENE_REPORT", 8L, null,
                "G-1", 5L, "T-5", "WO-4", 3L, "羽毛球", "B-1", 1L, 2L,
                6L, "穿线", "D-1", "断线", 2, 1,
                LocalDateTime.of(2026, 7, 17, 9, 0));
        when(repository.listSceneDefects(criteria, 50)).thenReturn(List.of(record));

        var result = new SceneDefectSourceProvider(repository).load(criteria, 50);

        assertThat(result.records()).containsExactly(record);
        assertThat(result.warnings()).isEmpty();
        verify(repository).listSceneDefects(criteria, 50);
    }
}
