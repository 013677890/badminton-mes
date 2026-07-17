package com.badminton.mes.module.report.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.module.report.controller.vo.ProductTraceQueryReqVO;
import com.badminton.mes.module.report.controller.vo.ProductTraceRespVO;
import com.badminton.mes.module.report.service.ProductTraceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 产品批次和条码追溯接口。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
@RestController
@RequestMapping("/api/report/traces")
@Validated
public class TraceController {

    private final ProductTraceService traceService;

    public TraceController(ProductTraceService traceService) {
        this.traceService = traceService;
    }

    @GetMapping("/products")
    public CommonResult<ProductTraceRespVO> products(@Valid ProductTraceQueryReqVO reqVO) {
        return CommonResult.success(traceService.trace(reqVO));
    }

    @GetMapping("/barcodes/{barcodeValue}")
    public CommonResult<ProductTraceRespVO> barcode(
            @PathVariable @Size(max = 128) String barcodeValue) {
        return CommonResult.success(traceService.traceByBarcode(barcodeValue));
    }
}
