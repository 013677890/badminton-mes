package com.badminton.mes.module.report.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import com.badminton.mes.module.report.controller.vo.DefectReportRespVO;
import com.badminton.mes.module.report.controller.vo.ProductionReportRespVO;
import com.badminton.mes.module.report.controller.vo.ReportQueryReqVO;
import com.badminton.mes.module.report.service.DefectSourceProvider;
import com.badminton.mes.module.report.service.ProductionReportService;
import com.badminton.mes.module.report.service.dto.DefectSourceBatch;
import com.badminton.mes.module.report.service.dto.DefectSourceRecord;
import com.badminton.mes.module.report.service.dto.ReportQueryCriteria;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * M4 报工冲销净额和 defectGroupNo 跨来源归并测试。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
class DefectReportServiceImplTest {

    private final ReportQuerySupport querySupport = mock(ReportQuerySupport.class);
    private final ProductionReportService productionReportService = mock(ProductionReportService.class);
    private final ReportQueryCriteria criteria = new ReportQueryCriteria(null, null, null, null,
            null, null, null, null, null, null, null);

    @Test
    void summaryUsesSceneNetAndDefectGroupDeduplication() {
        LocalDateTime time = LocalDateTime.of(2026, 7, 13, 10, 0);
        DefectSourceProvider scene = (ignored, limit) -> new DefectSourceBatch(List.of(
                record("SCENE_WORK_REPORT", 1L, "D001", 10, 0, time),
                record("SCENE_WORK_REPORT", 2L, "D001", 0, 10, time.plusMinutes(1)),
                record("SCENE_WORK_REPORT", 3L, null, 4, 0, time.plusMinutes(2))), List.of());
        DefectSourceProvider quality = (ignored, limit) -> new DefectSourceBatch(List.of(
                record("QUALITY_INSPECTION", 4L, "D001", 10, 0, time.plusMinutes(3))),
                List.of("REPAIR_RECHECK unavailable"));
        DefectReportServiceImpl service = new DefectReportServiceImpl(List.of(scene, quality),
                querySupport, productionReportService);
        ReportQueryReqVO reqVO = new ReportQueryReqVO();
        when(querySupport.criteria(reqVO)).thenReturn(criteria);
        ProductionReportRespVO.Summary production = new ProductionReportRespVO.Summary();
        production.setInputQuantity(100);
        when(productionReportService.summary(reqVO)).thenReturn(production);

        DefectReportRespVO.Summary result = service.summary(reqVO);

        assertThat(result.getSceneDefectQuantity()).isEqualTo(4);
        assertThat(result.getQualityDefectQuantity()).isEqualTo(10);
        assertThat(result.getComprehensiveDefectQuantity()).isEqualTo(14);
        assertThat(result.getSourceRecordCount()).isEqualTo(4);
        assertThat(result.getComprehensiveEventCount()).isEqualTo(2);
        assertThat(result.getMergedDuplicateCount()).isEqualTo(2);
        assertThat(result.getSceneDefectRate()).isEqualByComparingTo("0.0400");
        assertThat(result.getComprehensiveDefectRate()).isEqualByComparingTo("0.1400");
        assertThat(result.getWarnings()).containsExactly("REPAIR_RECHECK unavailable");
    }

    @Test
    void recordsWithoutDefectGroupAreNeverAutoMerged() {
        DefectSourceProvider provider = (ignored, limit) -> new DefectSourceBatch(List.of(
                record("SCENE_WORK_REPORT", 1L, null, 2, 0, LocalDateTime.now()),
                record("QUALITY_INSPECTION", 2L, null, 2, 0, LocalDateTime.now())), List.of());
        DefectReportServiceImpl service = new DefectReportServiceImpl(List.of(provider),
                querySupport, productionReportService);
        ReportQueryReqVO reqVO = new ReportQueryReqVO();
        when(querySupport.criteria(reqVO)).thenReturn(criteria);

        assertThat(service.comprehensiveDetails(reqVO).getTotal()).isEqualTo(2L);
    }

    private DefectSourceRecord record(String sourceType, Long sourceId, String groupNo,
                                      long occurrence, long reversal, LocalDateTime time) {
        return new DefectSourceRecord(sourceType, sourceId, sourceId, groupNo, 10L, "T1", "W1",
                20L, "产品", "B1", 30L, 40L, 50L, "工序", null, "未分类",
                occurrence, reversal, time);
    }
}
