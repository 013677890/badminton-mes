package com.badminton.mes.module.report.controller;

import java.nio.charset.StandardCharsets;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.report.controller.vo.ProductionReportRespVO;
import com.badminton.mes.module.report.controller.vo.ReportQueryReqVO;
import com.badminton.mes.module.report.service.ProductionReportService;
import com.badminton.mes.module.report.service.ReportExportFile;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 车间时段报表接口，复用产量报表查询口径和权限条件。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
@RestController
@RequestMapping("/api/report/workshop_periods")
@Validated
public class WorkshopPeriodReportController {

    private final ProductionReportService reportService;

    public WorkshopPeriodReportController(ProductionReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    public CommonResult<ProductionReportRespVO.Summary> summary(@Valid ReportQueryReqVO reqVO) {
        return CommonResult.success(reportService.summary(reqVO));
    }

    @GetMapping("/details")
    public CommonResult<PageResult<ProductionReportRespVO.Detail>> details(@Valid ReportQueryReqVO reqVO) {
        return CommonResult.success(reportService.details(reqVO));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@Valid ReportQueryReqVO reqVO) {
        ReportExportFile file = reportService.export(reqVO, "workshop_periods");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, file.contentType());
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(file.content());
    }
}
