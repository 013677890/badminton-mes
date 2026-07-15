package com.badminton.mes.module.report.controller;

import java.util.List;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.module.report.controller.vo.RealtimeProductionRespVO;
import com.badminton.mes.module.report.controller.vo.RealtimeReportQueryReqVO;
import com.badminton.mes.module.report.service.RealtimeProductionService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 实时生产信息接口。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
@RestController
@RequestMapping("/api/report/realtime_production")
@Validated
public class RealtimeProductionController {

    private final RealtimeProductionService realtimeProductionService;

    public RealtimeProductionController(RealtimeProductionService realtimeProductionService) {
        this.realtimeProductionService = realtimeProductionService;
    }

    @GetMapping("/overview")
    public CommonResult<RealtimeProductionRespVO.Overview> overview(
            @Valid RealtimeReportQueryReqVO reqVO) {
        return CommonResult.success(realtimeProductionService.overview(reqVO));
    }

    @GetMapping("/tasks")
    public CommonResult<List<RealtimeProductionRespVO.Task>> tasks(
            @Valid RealtimeReportQueryReqVO reqVO) {
        return CommonResult.success(realtimeProductionService.tasks(reqVO));
    }
}
