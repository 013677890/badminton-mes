package com.badminton.mes.module.report.service.miniapp;

import com.badminton.mes.module.report.controller.vo.*;
import com.badminton.mes.module.report.controller.vo.MiniAppRealtimeDashboardRespVO;

/**
 * 微信小程序展示聚合服务。
 *
 * <p>由小程序 Controller 调用，统一编排实时生产、生产分析和产品追溯查询，避免
 * 小程序端直接依赖多个后台模块的内部 Service。
 *
 * @author 刘涵
 * @date 2026/07/16
 */
public interface MiniAppDashboardService {
    /** 查询小程序首页实时生产概览。 */
    MiniAppRealtimeDashboardRespVO realtimeDashboard(RealtimeReportQueryReqVO request);
    /** 查询指定时间范围的生产分析摘要。 */
    ProductionReportRespVO.Summary productionAnalysis(ReportQueryReqVO request);
    /** 查询产品批次的追溯链路。 */
    ProductTraceRespVO productTrace(ProductTraceQueryReqVO request);
}
