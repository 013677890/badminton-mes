package com.badminton.mes.module.report.service.impl;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.report.controller.vo.ProductTraceQueryReqVO;
import com.badminton.mes.module.report.controller.vo.ProductTraceRespVO;
import com.badminton.mes.module.report.dal.ReportQueryRepository;
import com.badminton.mes.module.report.dal.ReportQueryRows.TraceTask;
import com.badminton.mes.module.report.service.ReportDataScopeService;
import com.badminton.mes.module.scene.dal.repository.SceneRepairWorkOrderRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M4 追溯主链路与跨组来源查询测试。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
class ProductTraceServiceImplTest {

    private final ReportQueryRepository repository = mock(ReportQueryRepository.class);
    private final ReportDataScopeService dataScopeService = mock(ReportDataScopeService.class);
    private final SceneRepairWorkOrderRepository repairRepository = mock(SceneRepairWorkOrderRepository.class);
    private final ProductTraceServiceImpl service = new ProductTraceServiceImpl(repository, dataScopeService, repairRepository);

    @Test
    void traceReturnsCoreDataAndQueriesAvailableCGroupSources() {
        ProductTraceQueryReqVO reqVO = new ProductTraceQueryReqVO();
        reqVO.setBatchCode("B001");
        TraceTask task = new TraceTask(1L, "T1", 2L, "W1", 3L, "P1", "产品", "B001",
                4L, "车间", 5L, "产线", 100, 20, 18, 2, 0, 10, 3, null, null);
        when(repository.findTraceTask("B001", null, null, null)).thenReturn(Optional.of(task));
        when(repository.findWorkOrder(2L)).thenReturn(Optional.empty());
        when(repository.listTraceBarcodes(task)).thenReturn(List.of());
        when(repository.listTraceBarcodeUses(1L)).thenReturn(List.of());
        when(repository.listTraceProcessHistories(1L)).thenReturn(List.of());
        when(repository.listTraceReports(1L)).thenReturn(List.of());
        when(repository.listTraceMaterials(2L)).thenReturn(List.of());

        ProductTraceRespVO result = service.trace(reqVO);

        assertThat(result.getTask().getBatchNo()).isEqualTo("B001");
        assertThat(result.getDataCompleteness()).isEqualTo("PARTIAL");
        assertThat(result.getWarnings()).hasSize(3)
                .noneMatch(warning -> warning.startsWith("QUALITY_INSPECTION")
                        || warning.startsWith("EQUIPMENT") || warning.startsWith("ANDON"));
        verify(dataScopeService).checkObject(4L, 5L);
        verify(repository).listTraceQualityDefects(task);
        verify(repository).listTraceEquipmentStatuses(task);
        verify(repository).listTraceAndonEvents(task);
    }
}
