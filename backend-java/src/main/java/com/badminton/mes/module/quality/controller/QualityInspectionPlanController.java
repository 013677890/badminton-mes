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
 * 质量检验标准方案 REST 接口。
 *
 * <p>{@link Valid} 校验请求体/查询对象的字段格式，{@link RequiresRoles} 由鉴权拦截器
 * 校验角色；所有业务状态和版本规则下沉到 {@code QualityInspectionPlanService}。
 *
 * @author MES 开发组
 * @date 2026/07/16
 */
@RestController
@RequestMapping("/api/quality/inspection-plans")
public class QualityInspectionPlanController {

    private final QualityInspectionPlanService planService;

    public QualityInspectionPlanController(QualityInspectionPlanService planService) {
        this.planService = planService;
    }

    /** 创建检验方案草稿。 */
    @PostMapping
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Long> create(@Valid @RequestBody QualityInspectionPlanSaveReqVO request) {
        return CommonResult.success(planService.createPlan(request));
    }

    /** 修改检验方案草稿。 */
    @PutMapping("/{id}")
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Void> update(@PathVariable @Positive Long id,
                                     @Valid @RequestBody QualityInspectionPlanSaveReqVO request) {
        planService.updatePlan(id, request);
        return CommonResult.success(null);
    }

    /** 逻辑删除检验方案。 */
    @DeleteMapping("/{id}")
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Void> delete(@PathVariable @Positive Long id) {
        planService.deletePlan(id);
        return CommonResult.success(null);
    }

    /** 审核并生效检验方案。 */
    @PutMapping("/{id}/audit")
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Void> audit(@PathVariable @Positive Long id) {
        planService.auditPlan(id);
        return CommonResult.success(null);
    }

    /** 停用已生效检验方案。 */
    @PutMapping("/{id}/disable")
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Void> disable(@PathVariable @Positive Long id) {
        planService.disablePlan(id);
        return CommonResult.success(null);
    }

    /** 从已有方案复制创建新版本草稿。 */
    @PostMapping("/{id}/versions")
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Long> createNewVersion(@PathVariable @Positive Long id) {
        return CommonResult.success(planService.createNewVersion(id));
    }

    /** 查询检验方案详情及项目明细。 */
    @GetMapping("/{id}")
    @RequiresRoles({ADMIN, INSPECTOR, PMC, WORKSHOP_MANAGER, TEAM_LEADER})
    public CommonResult<QualityInspectionPlanRespVO> get(@PathVariable @Positive Long id) {
        return CommonResult.success(planService.getPlan(id));
    }

    /** 分页查询检验方案。 */
    @GetMapping("/page")
    @RequiresRoles({ADMIN, INSPECTOR, PMC, WORKSHOP_MANAGER, TEAM_LEADER})
    public CommonResult<PageResult<QualityInspectionPlanRespVO>> page(
            @Valid QualityInspectionPlanPageReqVO request) {
        return CommonResult.success(planService.getPlanPage(request));
    }
}
