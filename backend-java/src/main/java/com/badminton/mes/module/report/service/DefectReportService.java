package com.badminton.mes.module.report.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.report.controller.vo.DefectReportRespVO;
import com.badminton.mes.module.report.controller.vo.ReportQueryReqVO;

/**
 * 不良来源明细与综合去重报表服务。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
public interface DefectReportService {

    /** 查询来源明细，不做 defectGroupNo 去重。 */
    PageResult<DefectReportRespVO.Detail> sourceDetails(ReportQueryReqVO reqVO);

    /** 查询按 defectGroupNo 归并后的综合明细。 */
    PageResult<DefectReportRespVO.Detail> comprehensiveDetails(ReportQueryReqVO reqVO);

    /** 查询来源与综合口径汇总。 */
    DefectReportRespVO.Summary summary(ReportQueryReqVO reqVO);

    /** 同步导出不良来源明细。 */
    ReportExportFile export(ReportQueryReqVO reqVO);
}
