package com.badminton.mes.module.report.controller;

import java.nio.charset.StandardCharsets;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.report.controller.vo.DefectReportRespVO;
import com.badminton.mes.module.report.controller.vo.ReportQueryReqVO;
import com.badminton.mes.module.report.service.DefectReportService;
import com.badminton.mes.module.report.service.ReportExportFile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 不良来源明细和综合聚合接口。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
@RestController
@RequestMapping("/api/report/defects")
@Validated
public class DefectQueryController {

    private final DefectReportService defectReportService;

    public DefectQueryController(DefectReportService defectReportService) {
        this.defectReportService = defectReportService;
    }

    @GetMapping("/page")
    public CommonResult<PageResult<DefectReportRespVO.Detail>> page(
            @Valid ReportQueryReqVO reqVO,
            @RequestParam(defaultValue = "SOURCE")
            @Pattern(regexp = "SOURCE|COMPREHENSIVE") String view) {
        PageResult<DefectReportRespVO.Detail> result = "COMPREHENSIVE".equals(view)
                ? defectReportService.comprehensiveDetails(reqVO)
                : defectReportService.sourceDetails(reqVO);
        return CommonResult.success(result);
    }

    @GetMapping("/summary")
    public CommonResult<DefectReportRespVO.Summary> summary(@Valid ReportQueryReqVO reqVO) {
        return CommonResult.success(defectReportService.summary(reqVO));
    }

    @GetMapping("/export")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC, RoleCodeConstants.WORKSHOP_MANAGER})
    public ResponseEntity<byte[]> export(@Valid ReportQueryReqVO reqVO) {
        ReportExportFile file = defectReportService.export(reqVO);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, file.contentType());
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(file.content());
    }
}
