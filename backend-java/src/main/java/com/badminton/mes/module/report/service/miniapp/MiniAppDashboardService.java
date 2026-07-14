package com.badminton.mes.module.report.service.miniapp;

import com.badminton.mes.module.report.controller.vo.*;
import java.util.Map;

/** 微信小程序展示聚合服务。 @author 刘涵 */
public interface MiniAppDashboardService {
    Map<String, Object> realtimeDashboard(RealtimeReportQueryReqVO request);
    ProductionReportRespVO.Summary productionAnalysis(ReportQueryReqVO request);
    ProductTraceRespVO productTrace(ProductTraceQueryReqVO request);
}
