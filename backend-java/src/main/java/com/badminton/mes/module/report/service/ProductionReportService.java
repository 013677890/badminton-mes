package com.badminton.mes.module.report.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.report.controller.vo.ProductionReportRespVO;
import com.badminton.mes.module.report.controller.vo.ReportQueryReqVO;

/**
 * 产量和车间时段报表服务。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
public interface ProductionReportService {

    /** 查询产量净额汇总。 */
    ProductionReportRespVO.Summary summary(ReportQueryReqVO reqVO);

    /** 分页查询报工发生、冲销和净额明细。 */
    PageResult<ProductionReportRespVO.Detail> details(ReportQueryReqVO reqVO);

    /** 同步导出产量明细 CSV。 */
    ReportExportFile export(ReportQueryReqVO reqVO, String filePrefix);
}
