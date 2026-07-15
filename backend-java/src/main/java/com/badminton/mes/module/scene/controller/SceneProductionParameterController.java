package com.badminton.mes.module.scene.controller;

import java.util.List;
import com.badminton.mes.common.core.*;
import com.badminton.mes.common.security.*;
import com.badminton.mes.module.scene.controller.vo.*;
import com.badminton.mes.module.scene.service.SceneProductionParameterService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

/** 生产参数 Controller。 @author 刘涵 */
@RestController @RequestMapping("/api/scene/production_parameters") @org.springframework.validation.annotation.Validated
public class SceneProductionParameterController {
    private final SceneProductionParameterService service;
    public SceneProductionParameterController(SceneProductionParameterService service){this.service=service;}
    @PostMapping @RequiresRoles({RoleCodeConstants.ADMIN,RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Long> create(@Valid @RequestBody SceneProductionParameterSaveReqVO req){return CommonResult.success(service.createParameter(req));}
    @PutMapping("/{id}") @RequiresRoles({RoleCodeConstants.ADMIN,RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Void> update(@PathVariable @Positive Long id,@Valid @RequestBody SceneProductionParameterSaveReqVO req){service.updateParameter(id,req);return CommonResult.success(null);}
    @PutMapping("/{id}/enable") @RequiresRoles({RoleCodeConstants.ADMIN,RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Void> enable(@PathVariable @Positive Long id,@Valid @RequestBody SceneParameterChangeReqVO req){service.enableParameter(id,req.getReason());return CommonResult.success(null);}
    @PutMapping("/{id}/disable") @RequiresRoles({RoleCodeConstants.ADMIN,RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Void> disable(@PathVariable @Positive Long id,@Valid @RequestBody SceneParameterChangeReqVO req){service.disableParameter(id,req.getReason());return CommonResult.success(null);}
    @GetMapping("/{id}") public CommonResult<SceneProductionParameterRespVO> get(@PathVariable @Positive Long id){return CommonResult.success(service.getParameter(id));}
    @GetMapping("/page") public CommonResult<PageResult<SceneProductionParameterRespVO>> page(@Valid SceneProductionParameterPageReqVO req){return CommonResult.success(service.getParameterPage(req));}
    @GetMapping("/effective") public CommonResult<SceneProductionParameterRespVO> effective(@Valid SceneEffectiveParameterReqVO req){return CommonResult.success(service.getEffectiveParameter(req));}
    @GetMapping("/{id}/change_logs") public CommonResult<List<SceneParameterChangeLogRespVO>> logs(@PathVariable @Positive Long id){return CommonResult.success(service.getChangeLogs(id));}
}
