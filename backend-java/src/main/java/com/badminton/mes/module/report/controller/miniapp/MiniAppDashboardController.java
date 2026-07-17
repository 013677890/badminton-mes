package com.badminton.mes.module.report.controller.miniapp;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.module.report.controller.vo.*;
import com.badminton.mes.module.report.service.miniapp.MiniAppDashboardService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 微信小程序只读聚合接口。 @author 刘涵 */
@Validated @RestController @RequestMapping("/api/report/mini_app")
public class MiniAppDashboardController {
    private final MiniAppDashboardService dashboardService;

    public MiniAppDashboardController(MiniAppDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/realtime_dashboard")
    public CommonResult<MiniAppRealtimeDashboardRespVO> realtime(@Valid RealtimeReportQueryReqVO request) {
        return CommonResult.success(dashboardService.realtimeDashboard(request));
    }

    @GetMapping("/production_analysis")
    public CommonResult<ProductionReportRespVO.Summary> production(@Valid ReportQueryReqVO request) {
        return CommonResult.success(dashboardService.productionAnalysis(request));
    }

    @GetMapping("/product_trace")
    public CommonResult<ProductTraceRespVO> trace(@Valid ProductTraceQueryReqVO request) {
        return CommonResult.success(dashboardService.productTrace(request));
    }
}
