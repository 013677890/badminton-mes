package com.badminton.mes.module.report.service.impl.miniapp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badminton.mes.module.report.controller.vo.*;
import com.badminton.mes.module.report.service.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class MiniAppDashboardServiceImplTest {
    @Test
    void shouldReuseExistingReportServices() {
        RealtimeProductionService realtime = mock(RealtimeProductionService.class);
        ProductionReportService production = mock(ProductionReportService.class);
        ProductTraceService trace = mock(ProductTraceService.class);
        var overview = new RealtimeProductionRespVO.Overview();
        when(realtime.overview(any())).thenReturn(overview); when(realtime.tasks(any())).thenReturn(List.of());
        var service = new MiniAppDashboardServiceImpl(realtime, production, trace);
        var result = service.realtimeDashboard(new RealtimeReportQueryReqVO());
        assertSame(overview, result.get("overview")); assertEquals(List.of(), result.get("tasks"));
        verify(realtime).overview(any()); verify(realtime).tasks(any());
    }
}
