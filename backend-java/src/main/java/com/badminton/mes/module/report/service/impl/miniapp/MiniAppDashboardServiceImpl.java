package com.badminton.mes.module.report.service.impl.miniapp;

import com.badminton.mes.module.report.controller.vo.*;
import com.badminton.mes.module.report.service.*;
import com.badminton.mes.module.report.service.miniapp.MiniAppDashboardService;
import java.util.List;
import org.springframework.stereotype.Service;

/** 微信小程序展示聚合服务实现。 @author 刘涵 */
@Service
public class MiniAppDashboardServiceImpl implements MiniAppDashboardService {
    private final RealtimeProductionService realtimeService;
    private final ProductionReportService productionService;
    private final ProductTraceService traceService;

    public MiniAppDashboardServiceImpl(RealtimeProductionService realtimeService,
                                       ProductionReportService productionService,
                                       ProductTraceService traceService) {
        this.realtimeService = realtimeService;
        this.productionService = productionService;
        this.traceService = traceService;
    }

    @Override
    public MiniAppRealtimeDashboardRespVO realtimeDashboard(RealtimeReportQueryReqVO request) {
        MiniAppRealtimeDashboardRespVO result = new MiniAppRealtimeDashboardRespVO();
        result.setOverview(realtimeService.overview(request));
        result.setTasks(List.copyOf(realtimeService.tasks(request)));
        return result;
    }

    @Override
    public ProductionReportRespVO.Summary productionAnalysis(ReportQueryReqVO request) {
        return productionService.summary(request);
    }

    @Override
    public ProductTraceRespVO productTrace(ProductTraceQueryReqVO request) {
        return traceService.trace(request);
    }
}
