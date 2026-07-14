package com.badminton.mes.module.andon.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.module.andon.controller.vo.AndonReasonPageReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonReasonRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonReasonSaveReqVO;
import com.badminton.mes.module.andon.service.AndonReasonService;

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
import static com.badminton.mes.common.security.RoleCodeConstants.OPERATOR;
import static com.badminton.mes.common.security.RoleCodeConstants.TEAM_LEADER;
import static com.badminton.mes.common.security.RoleCodeConstants.WORKSHOP_MANAGER;

/** 安灯异常原因接口。 */
@RestController
@RequestMapping("/api/andon/reasons")
public class AndonReasonController {

    private final AndonReasonService reasonService;

    public AndonReasonController(AndonReasonService reasonService) {
        this.reasonService = reasonService;
    }

    @PostMapping
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER})
    public CommonResult<Long> create(@Valid @RequestBody AndonReasonSaveReqVO request) {
        return CommonResult.success(reasonService.createReason(request));
    }

    @PutMapping("/{id}")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER})
    public CommonResult<Void> update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody AndonReasonSaveReqVO request) {
        reasonService.updateReason(id, request);
        return CommonResult.success(null);
    }

    @DeleteMapping("/{id}")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER})
    public CommonResult<Void> delete(@PathVariable @Positive Long id) {
        reasonService.deleteReason(id);
        return CommonResult.success(null);
    }

    @GetMapping("/{id}")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR, INSPECTOR})
    public CommonResult<AndonReasonRespVO> get(@PathVariable @Positive Long id) {
        return CommonResult.success(reasonService.getReason(id));
    }

    @GetMapping("/page")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR, INSPECTOR})
    public CommonResult<PageResult<AndonReasonRespVO>> page(@Valid AndonReasonPageReqVO request) {
        return CommonResult.success(reasonService.getReasonPage(request));
    }
}
