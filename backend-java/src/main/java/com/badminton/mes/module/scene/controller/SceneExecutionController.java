package com.badminton.mes.module.scene.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.scene.controller.vo.ActionRemarkReqVO;
import com.badminton.mes.module.scene.controller.vo.CompletionOrderSaveReqVO;
import com.badminton.mes.module.scene.controller.vo.WorkReportApproveReqVO;
import com.badminton.mes.module.scene.service.SceneCompletionOrderService;
import com.badminton.mes.module.scene.service.SceneWorkReportService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * 现场报工确认和生产完工单接口。
 *
 * @author Codex
 * @date 2026/07/13
 */
@RestController
@RequestMapping("/api/scene")
@RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER,
        RoleCodeConstants.TEAM_LEADER})
public class SceneExecutionController {

    private final SceneWorkReportService workReportService;
    private final SceneCompletionOrderService completionOrderService;

    public SceneExecutionController(SceneWorkReportService workReportService,
                                    SceneCompletionOrderService completionOrderService) {
        this.workReportService = workReportService;
        this.completionOrderService = completionOrderService;
    }

    /** 审核待确认报工并同步计件工资快照。 */
    @PutMapping("/work_reports/{id}/approve")
    public CommonResult<Boolean> approveWorkReport(
            @PathVariable Long id,
            @Valid @RequestBody WorkReportApproveReqVO reqVO) {
        workReportService.approveReport(id, reqVO.getEmployeeId());
        return CommonResult.success(true);
    }

    /** 创建待审核生产完工单。 */
    @PostMapping("/completion_orders")
    public CommonResult<Long> createCompletionOrder(
            @Valid @RequestBody CompletionOrderSaveReqVO reqVO) {
        return CommonResult.success(completionOrderService.createCompletionOrder(reqVO));
    }

    /** 审核生产完工单。 */
    @PutMapping("/completion_orders/{id}/approve")
    public CommonResult<Boolean> approveCompletionOrder(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ActionRemarkReqVO reqVO) {
        completionOrderService.approveCompletionOrder(id, reqVO == null ? null : reqVO.getRemark());
        return CommonResult.success(true);
    }

    /** 作废待审核生产完工单。 */
    @PutMapping("/completion_orders/{id}/void")
    public CommonResult<Boolean> voidCompletionOrder(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ActionRemarkReqVO reqVO) {
        completionOrderService.voidCompletionOrder(id, reqVO == null ? null : reqVO.getRemark());
        return CommonResult.success(true);
    }
}
