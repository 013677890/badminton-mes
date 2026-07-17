package com.badminton.mes.module.report.service.impl;

import com.badminton.mes.module.report.controller.vo.RealtimeReportQueryReqVO;
import com.badminton.mes.module.report.dal.ReportQueryRepository;
import com.badminton.mes.module.report.dal.ReportQueryRows.RealtimeSupport;
import com.badminton.mes.module.report.dal.ReportQueryRows.RealtimeTask;
import com.badminton.mes.module.report.service.ReportDataScopeService;
import com.badminton.mes.module.report.service.ReportDataScopeService.ReportDataScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 实时生产总览、任务映射和空值聚合单元测试。 @author 范家权 */
@ExtendWith(MockitoExtension.class)
class RealtimeProductionServiceImplTest {

    @Mock
    private ReportQueryRepository repository;

    @Mock
    private ReportDataScopeService dataScopeService;

    private RealtimeProductionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RealtimeProductionServiceImpl(repository, dataScopeService);
    }

    @Test
    void overviewAggregatesTaskQuantitiesStatusesAndSupportMetrics() {
        RealtimeReportQueryReqVO request = request();
        when(dataScopeService.resolve(10L, 20L)).thenReturn(new ReportDataScope(10L, 20L));
        when(repository.listRealtimeTasks(10L, 20L, 30L)).thenReturn(List.of(
                task(1L, 3, true, 100, null, 80, 5),
                task(2L, 4, false, 50, 40, 35, 3)));
        when(repository.loadRealtimeSupport(10L, 20L))
                .thenReturn(new RealtimeSupport(8, 6, 2, 3, 1));

        var result = service.overview(request);

        assertThat(result.getActiveTaskCount()).isEqualTo(1);
        assertThat(result.getPausedTaskCount()).isEqualTo(1);
        assertThat(result.getAbnormalBatchCount()).isEqualTo(1);
        assertThat(result.getPlanQuantity()).isEqualTo(150);
        assertThat(result.getInputQuantity()).isEqualTo(40);
        assertThat(result.getGoodQuantity()).isEqualTo(115);
        assertThat(result.getDefectQuantity()).isEqualTo(8);
        assertThat(result.getEquipmentTotalCount()).isEqualTo(8);
        assertThat(result.getRunningEquipmentCount()).isEqualTo(6);
        assertThat(result.getUnavailableEquipmentCount()).isEqualTo(2);
        assertThat(result.getOpenAndonCount()).isEqualTo(3);
        assertThat(result.getCriticalAndonCount()).isEqualTo(1);
        assertThat(result.getLastRefreshTime()).isNotNull();
        assertThat(result.getDataStatus()).isEqualTo("PARTIAL");
        assertThat(result.getWarnings()).singleElement().asString().contains("OEE");
        verify(dataScopeService, times(2)).resolve(10L, 20L);
    }

    @Test
    void tasksMapRepositoryRowsWithoutExposingProjectionObjects() {
        RealtimeReportQueryReqVO request = request();
        when(dataScopeService.resolve(10L, 20L)).thenReturn(new ReportDataScope(10L, 20L));
        when(repository.listRealtimeTasks(10L, 20L, 30L))
                .thenReturn(List.of(task(9L, 3, true, 120, 60, 55, 5)));

        var result = service.tasks(request);

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.getTaskId()).isEqualTo(9L);
            assertThat(item.getTaskNo()).isEqualTo("TASK-9");
            assertThat(item.getWorkOrderNo()).isEqualTo("WO-9");
            assertThat(item.getProductId()).isEqualTo(30L);
            assertThat(item.getWorkshopId()).isEqualTo(10L);
            assertThat(item.getLineId()).isEqualTo(20L);
            assertThat(item.getPlanQuantity()).isEqualTo(120);
            assertThat(item.isAbnormal()).isTrue();
        });
    }

    @Test
    void emptyOverviewReturnsZerosInsteadOfNullAggregates() {
        RealtimeReportQueryReqVO request = request();
        when(dataScopeService.resolve(10L, 20L)).thenReturn(new ReportDataScope(10L, 20L));
        when(repository.listRealtimeTasks(10L, 20L, 30L)).thenReturn(List.of());
        when(repository.loadRealtimeSupport(10L, 20L))
                .thenReturn(new RealtimeSupport(0, 0, 0, 0, 0));

        var result = service.overview(request);

        assertThat(result.getActiveTaskCount()).isZero();
        assertThat(result.getPlanQuantity()).isZero();
        assertThat(result.getInputQuantity()).isZero();
        assertThat(result.getGoodQuantity()).isZero();
        assertThat(result.getDefectQuantity()).isZero();
    }

    private static RealtimeReportQueryReqVO request() {
        RealtimeReportQueryReqVO request = new RealtimeReportQueryReqVO();
        request.setWorkshopId(10L);
        request.setLineId(20L);
        request.setProductId(30L);
        return request;
    }

    private static RealtimeTask task(Long id, Integer status, boolean abnormal,
                                     Integer plan, Integer input, Integer good, Integer defect) {
        LocalDateTime time = LocalDateTime.of(2026, 7, 17, 8, 0);
        return new RealtimeTask(id, "TASK-" + id, "WO-" + id, 30L, "羽毛球",
                "BATCH-" + id, 10L, "一车间", 20L, "一号线", plan, input,
                good, defect, good, status, abnormal, time, time);
    }
}
