package com.badminton.mes.module.wage.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.wage.controller.vo.PieceRateRulePageReqVO;
import com.badminton.mes.module.wage.controller.vo.PieceRateRuleRespVO;
import com.badminton.mes.module.wage.controller.vo.PieceRateRuleSaveReqVO;
import com.badminton.mes.module.wage.controller.vo.PieceRateRuleStatusReqVO;
import com.badminton.mes.module.wage.controller.vo.PieceRateRuleUpdateReqVO;
import com.badminton.mes.module.wage.controller.vo.WageRuleChangeLogPageReqVO;
import com.badminton.mes.module.wage.controller.vo.WageRuleChangeLogRespVO;
import com.badminton.mes.module.wage.service.PieceRateRuleService;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** 计件规则管理 Controller。 */
@Validated
@RestController
@RequestMapping("/api/wage/rules")
@RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER, RoleCodeConstants.TEAM_LEADER})
public class PieceRateRuleController {

    private final PieceRateRuleService ruleService;

    /** 构造器注入。 */
    public PieceRateRuleController(PieceRateRuleService ruleService) {
        this.ruleService = ruleService;
    }

    /** 创建计件规则。 */
    @PostMapping
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Long> createRule(@Valid @RequestBody PieceRateRuleSaveReqVO reqVO) {
        return CommonResult.success(ruleService.createRule(reqVO));
    }

    /** 修改计件规则。 */
    @PutMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Void> updateRule(@PathVariable("id") @Positive Long id,
                                         @Valid @RequestBody PieceRateRuleUpdateReqVO reqVO) {
        ruleService.updateRule(id, reqVO);
        return CommonResult.success(null);
    }

    /** 删除计件规则。 */
    @DeleteMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Void> deleteRule(@PathVariable("id") @Positive Long id,
                                         @RequestParam("version") @PositiveOrZero Integer version) {
        ruleService.deleteRule(id, version);
        return CommonResult.success(null);
    }

    /** 启用或停用计件规则。 */
    @PutMapping("/{id}/status")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Void> updateRuleStatus(@PathVariable("id") @Positive Long id,
                                               @Valid @RequestBody PieceRateRuleStatusReqVO reqVO) {
        ruleService.updateRuleStatus(id, reqVO);
        return CommonResult.success(null);
    }

    /** 查询计件规则详情。 */
    @GetMapping("/{id}")
    public CommonResult<PieceRateRuleRespVO> getRule(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(ruleService.getRule(id));
    }

    /** 分页查询计件规则。 */
    @GetMapping("/page")
    public CommonResult<PageResult<PieceRateRuleRespVO>> getRulePage(
            @Valid PieceRateRulePageReqVO reqVO) {
        return CommonResult.success(ruleService.getRulePage(reqVO));
    }

    /** 分页查询计件规则变更日志。 */
    @GetMapping("/{id}/change_logs")
    public CommonResult<PageResult<WageRuleChangeLogRespVO>> getRuleChangeLogPage(
            @PathVariable("id") @Positive Long id, @Valid WageRuleChangeLogPageReqVO reqVO) {
        return CommonResult.success(ruleService.getRuleChangeLogPage(id, reqVO));
    }
}
