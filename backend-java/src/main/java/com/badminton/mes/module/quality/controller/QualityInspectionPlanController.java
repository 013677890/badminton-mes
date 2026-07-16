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

/**
 * 质量检验标准方案管理接口。
 *
 * <p>方案按“草稿（DRAFT）—生效（EFFECTIVE）—停用（DISABLED）”流转；维护类操作仅向质量管理员和
 * 检验员开放，生产计划、车间管理及班组岗位仅可查询已经按业务规则开放的方案数据。
 */
@RestController
@RequestMapping("/api/quality/inspection-plans")
public class QualityInspectionPlanController {

    private final QualityInspectionPlanService planService;

    public QualityInspectionPlanController(QualityInspectionPlanService planService) {
        this.planService = planService;
    }

    /** 创建包含检验项目快照的方案草稿，并返回新方案主键。 */
    @PostMapping
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Long> create(@Valid @RequestBody QualityInspectionPlanSaveReqVO request) {
        return CommonResult.success(planService.createPlan(request));
    }

    /** 修改指定的草稿方案；方案主键必须为正整数，生效或停用版本不可直接改写。 */
    @PutMapping("/{id}")
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Void> update(@PathVariable @Positive Long id,
                                     @Valid @RequestBody QualityInspectionPlanSaveReqVO request) {
        planService.updatePlan(id, request);
        return CommonResult.success(null);
    }

    /** 删除指定的草稿方案；已进入后续生命周期的版本由服务层拒绝删除。 */
    @DeleteMapping("/{id}")
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Void> delete(@PathVariable @Positive Long id) {
        planService.deletePlan(id);
        return CommonResult.success(null);
    }

    /** 审核草稿并使其生效，同时固化版本、默认方案及计划生效日期等规则。 */
    @PutMapping("/{id}/audit")
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Void> audit(@PathVariable @Positive Long id) {
        planService.auditPlan(id);
        return CommonResult.success(null);
    }

    /** 停用指定的生效方案，使其不再用于后续检验单建单。 */
    @PutMapping("/{id}/disable")
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Void> disable(@PathVariable @Positive Long id) {
        planService.disablePlan(id);
        return CommonResult.success(null);
    }

    /** 从指定历史版本复制内容并创建下一版本草稿，返回新版本主键。 */
    @PostMapping("/{id}/versions")
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Long> createNewVersion(@PathVariable @Positive Long id) {
        return CommonResult.success(planService.createNewVersion(id));
    }

    /** 按正整数主键查询方案基本信息及其检验项目明细。 */
    @GetMapping("/{id}")
    @RequiresRoles({ADMIN, INSPECTOR, PMC, WORKSHOP_MANAGER, TEAM_LEADER})
    public CommonResult<QualityInspectionPlanRespVO> get(@PathVariable @Positive Long id) {
        return CommonResult.success(planService.getPlan(id));
    }

    /** 按关键字、适用范围、检验类型和方案状态分页筛选方案。 */
    @GetMapping("/page")
    @RequiresRoles({ADMIN, INSPECTOR, PMC, WORKSHOP_MANAGER, TEAM_LEADER})
    public CommonResult<PageResult<QualityInspectionPlanRespVO>> page(
            @Valid QualityInspectionPlanPageReqVO request) {
        return CommonResult.success(planService.getPlanPage(request));
    }
}
