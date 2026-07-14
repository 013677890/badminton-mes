package com.badminton.mes.module.wage.controller;

import java.util.List;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.wage.controller.vo.EmployeeWageSummaryRespVO;
import com.badminton.mes.module.wage.controller.vo.ProcessWageSummaryRespVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementActionReqVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementAdjustReqVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementAuditLogPageReqVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementAuditLogRespVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementCalculateReqVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementDetailPageReqVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementDetailRespVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementPageReqVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementRespVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementVersionReqVO;
import com.badminton.mes.module.wage.controller.vo.WageSummaryReqVO;
import com.badminton.mes.module.wage.service.WageSettlementService;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

/** 计件工资结算 Controller。 */
@Validated
@RestController
@RequestMapping("/api/wage/settlements")
@RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER, RoleCodeConstants.TEAM_LEADER})
public class WageSettlementController {

    private final WageSettlementService settlementService;

    /** 构造器注入。 */
    public WageSettlementController(WageSettlementService settlementService) {
        this.settlementService = settlementService;
    }

    /** 计算新工资结算批次。 */
    @PostMapping("/calculate")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Long> calculateSettlement(
            @Valid @RequestBody WageSettlementCalculateReqVO reqVO) {
        return CommonResult.success(settlementService.calculateSettlement(reqVO));
    }

    /** 按原范围重新计算结算。 */
    @PostMapping("/{id}/recalculate")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Void> recalculateSettlement(
            @PathVariable("id") @Positive Long id,
            @Valid @RequestBody WageSettlementVersionReqVO reqVO) {
        settlementService.recalculateSettlement(id, reqVO);
        return CommonResult.success(null);
    }

    /** 提交结算审核。 */
    @PutMapping("/{id}/submit")
    public CommonResult<Void> submitSettlement(
            @PathVariable("id") @Positive Long id,
            @Valid @RequestBody WageSettlementActionReqVO reqVO) {
        settlementService.submitSettlement(id, reqVO);
        return CommonResult.success(null);
    }

    /** 审核通过结算。 */
    @PutMapping("/{id}/approve")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Void> approveSettlement(
            @PathVariable("id") @Positive Long id,
            @Valid @RequestBody WageSettlementActionReqVO reqVO) {
        settlementService.approveSettlement(id, reqVO);
        return CommonResult.success(null);
    }

    /** 驳回结算。 */
    @PutMapping("/{id}/reject")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Void> rejectSettlement(
            @PathVariable("id") @Positive Long id,
            @Valid @RequestBody WageSettlementActionReqVO reqVO) {
        settlementService.rejectSettlement(id, reqVO);
        return CommonResult.success(null);
    }

    /** 调整草稿结算明细金额。 */
    @PutMapping("/{id}/details/{detailId}/adjust")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Void> adjustDetail(
            @PathVariable("id") @Positive Long id,
            @PathVariable("detailId") @Positive Long detailId,
            @Valid @RequestBody WageSettlementAdjustReqVO reqVO) {
        settlementService.adjustDetail(id, detailId, reqVO);
        return CommonResult.success(null);
    }

    /** 查询结算批次详情。 */
    @GetMapping("/{id}")
    public CommonResult<WageSettlementRespVO> getSettlement(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(settlementService.getSettlement(id));
    }

    /** 分页查询结算批次。 */
    @GetMapping("/page")
    public CommonResult<PageResult<WageSettlementRespVO>> getSettlementPage(
            @Valid WageSettlementPageReqVO reqVO) {
        return CommonResult.success(settlementService.getSettlementPage(reqVO));
    }

    /** 分页查询当前有效结算明细。 */
    @GetMapping("/{id}/details")
    public CommonResult<PageResult<WageSettlementDetailRespVO>> getDetailPage(
            @PathVariable("id") @Positive Long id,
            @Valid WageSettlementDetailPageReqVO reqVO) {
        return CommonResult.success(settlementService.getDetailPage(id, reqVO));
    }

    /** 分页查询结算审计日志。 */
    @GetMapping("/{id}/audit_logs")
    public CommonResult<PageResult<WageSettlementAuditLogRespVO>> getAuditLogPage(
            @PathVariable("id") @Positive Long id,
            @Valid WageSettlementAuditLogPageReqVO reqVO) {
        return CommonResult.success(settlementService.getAuditLogPage(id, reqVO));
    }

    /** 按员工汇总已审核工资。 */
    @GetMapping("/summaries/employees")
    public CommonResult<List<EmployeeWageSummaryRespVO>> summarizeEmployees(
            @Valid WageSummaryReqVO reqVO) {
        return CommonResult.success(settlementService.summarizeEmployees(reqVO));
    }

    /** 按工序汇总已审核工资。 */
    @GetMapping("/summaries/processes")
    public CommonResult<List<ProcessWageSummaryRespVO>> summarizeProcesses(
            @Valid WageSummaryReqVO reqVO) {
        return CommonResult.success(settlementService.summarizeProcesses(reqVO));
    }
}
