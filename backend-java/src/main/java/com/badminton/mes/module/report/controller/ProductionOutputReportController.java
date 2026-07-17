package com.badminton.mes.module.report.controller;

import java.nio.charset.StandardCharsets;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
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
 * 产量报表接口。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
@RestController
@RequestMapping("/api/report/production_outputs")
@Validated
public class ProductionOutputReportController {

    private final ProductionReportService reportService;

    public ProductionOutputReportController(ProductionReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * 查询指定时间范围内的生产汇总数据。
     *
     * @param reqVO 报表查询条件，包含车间、产品和时间范围等筛选项
     * @return 生产汇总结果
     */
    @GetMapping("/summary")
    public CommonResult<ProductionReportRespVO.Summary> summary(@Valid ReportQueryReqVO reqVO) {
        return CommonResult.success(reportService.summary(reqVO));
    }

    /**
     * 分页查询生产明细数据，供报表明细表格展示。
     *
     * @param reqVO 分页及筛选条件
     * @return 生产明细分页结果
     */
    @GetMapping("/details")
    public CommonResult<PageResult<ProductionReportRespVO.Detail>> details(@Valid ReportQueryReqVO reqVO) {
        return CommonResult.success(reportService.details(reqVO));
    }

    /**
     * 导出当前查询条件对应的生产报表文件。
     *
     * @param reqVO 报表查询条件
     * @return 带有附件响应头的报表字节流
     */
    @GetMapping("/export")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC, RoleCodeConstants.WORKSHOP_MANAGER})
    public ResponseEntity<byte[]> export(@Valid ReportQueryReqVO reqVO) {
        return download(reportService.export(reqVO, "production_outputs"));
    }

    /**
     * 将导出文件模型转换为 HTTP 下载响应，并设置正确的文件名和媒体类型。
     *
     * @param file 已生成的导出文件
     * @return 可供浏览器下载的响应实体
     */
    private ResponseEntity<byte[]> download(ReportExportFile file) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, file.contentType());
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(file.content());
    }
}
