package com.badminton.mes.module.quality.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionPlanPageReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionPlanRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionPlanSaveReqVO;
import com.badminton.mes.module.quality.service.QualityInspectionPlanService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import static com.badminton.mes.common.security.RoleCodeConstants.ADMIN;
import static com.badminton.mes.common.security.RoleCodeConstants.INSPECTOR;
import static com.badminton.mes.common.security.RoleCodeConstants.PMC;
import static com.badminton.mes.common.security.RoleCodeConstants.TEAM_LEADER;
import static com.badminton.mes.common.security.RoleCodeConstants.WORKSHOP_MANAGER;

/** 质量检验标准方案接口。 */
@RestController
@RequestMapping("/api/quality/inspection-plans")
public class QualityInspectionPlanController {

    private final QualityInspectionPlanService planService;

    public QualityInspectionPlanController(QualityInspectionPlanService planService) {
        this.planService = planService;
    }

    @PostMapping
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Long> create(@Valid @RequestBody QualityInspectionPlanSaveReqVO request) {
        return CommonResult.success(planService.createPlan(request));
    }

    @PutMapping("/{id}")
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Void> update(@PathVariable @Positive Long id,
                                     @Valid @RequestBody QualityInspectionPlanSaveReqVO request) {
        planService.updatePlan(id, request);
        return CommonResult.success(null);
    }

    @DeleteMapping("/{id}")
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Void> delete(@PathVariable @Positive Long id) {
        planService.deletePlan(id);
        return CommonResult.success(null);
    }

    @PutMapping("/{id}/audit")
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Void> audit(@PathVariable @Positive Long id) {
        planService.auditPlan(id);
        return CommonResult.success(null);
    }

    @PutMapping("/{id}/disable")
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Void> disable(@PathVariable @Positive Long id) {
        planService.disablePlan(id);
        return CommonResult.success(null);
    }

    @PostMapping("/{id}/versions")
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Long> createNewVersion(@PathVariable @Positive Long id) {
        return CommonResult.success(planService.createNewVersion(id));
    }

    @GetMapping("/{id}")
    @RequiresRoles({ADMIN, INSPECTOR, PMC, WORKSHOP_MANAGER, TEAM_LEADER})
    public CommonResult<QualityInspectionPlanRespVO> get(@PathVariable @Positive Long id) {
        return CommonResult.success(planService.getPlan(id));
    }

    @GetMapping("/page")
    @RequiresRoles({ADMIN, INSPECTOR, PMC, WORKSHOP_MANAGER, TEAM_LEADER})
    public CommonResult<PageResult<QualityInspectionPlanRespVO>> page(
            @Valid QualityInspectionPlanPageReqVO request) {
        return CommonResult.success(planService.getPlanPage(request));
    }
}
