package com.badminton.mes.module.andon.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.module.andon.controller.vo.AndonTypePageReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonTypeRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonTypeSaveReqVO;
import com.badminton.mes.module.andon.service.AndonTypeService;

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

/** 安灯类型接口。 */
@RestController
@RequestMapping("/api/andon/types")
public class AndonTypeController {

    private final AndonTypeService typeService;

    public AndonTypeController(AndonTypeService typeService) {
        this.typeService = typeService;
    }

    @PostMapping
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER})
    public CommonResult<Long> create(@Valid @RequestBody AndonTypeSaveReqVO request) {
        return CommonResult.success(typeService.createType(request));
    }

    @PutMapping("/{id}")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER})
    public CommonResult<Void> update(@PathVariable @Positive Long id,
                                     @Valid @RequestBody AndonTypeSaveReqVO request) {
        typeService.updateType(id, request);
        return CommonResult.success(null);
    }

    @DeleteMapping("/{id}")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER})
    public CommonResult<Void> delete(@PathVariable @Positive Long id) {
        typeService.deleteType(id);
        return CommonResult.success(null);
    }

    @GetMapping("/{id}")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR, INSPECTOR})
    public CommonResult<AndonTypeRespVO> get(@PathVariable @Positive Long id) {
        return CommonResult.success(typeService.getType(id));
    }

    @GetMapping("/page")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR, INSPECTOR})
    public CommonResult<PageResult<AndonTypeRespVO>> page(@Valid AndonTypePageReqVO request) {
        return CommonResult.success(typeService.getTypePage(request));
    }
}
