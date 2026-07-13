package com.badminton.mes.module.scene.controller;

import com.badminton.mes.common.core.*;
import com.badminton.mes.common.security.*;
import com.badminton.mes.module.scene.controller.vo.*;
import com.badminton.mes.module.scene.service.SceneProductionTaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

/** 生产任务 Controller。 @author 刘涵 */
@RestController @RequestMapping("/api/scene/production_tasks") @org.springframework.validation.annotation.Validated
public class SceneProductionTaskController {
    private final SceneProductionTaskService service;
    public SceneProductionTaskController(SceneProductionTaskService service){this.service=service;}
    @PostMapping @RequiresRoles({RoleCodeConstants.ADMIN,RoleCodeConstants.PMC,RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Long> create(@Valid @RequestBody SceneProductionTaskSaveReqVO req){return CommonResult.success(service.createTask(req));}
    @PutMapping("/{id}") @RequiresRoles({RoleCodeConstants.ADMIN,RoleCodeConstants.PMC,RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Void> update(@PathVariable @Positive Long id,@Valid @RequestBody SceneProductionTaskSaveReqVO req){service.updateTask(id,req);return CommonResult.success(null);}
    @PutMapping("/{id}/audit") @RequiresRoles({RoleCodeConstants.ADMIN,RoleCodeConstants.PMC,RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Void> audit(@PathVariable @Positive Long id){service.auditTask(id);return CommonResult.success(null);}
    @PutMapping("/{id}/release") @RequiresRoles({RoleCodeConstants.ADMIN,RoleCodeConstants.PMC,RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Void> release(@PathVariable @Positive Long id){service.releaseTask(id);return CommonResult.success(null);}
    @PutMapping("/{id}/start") @RequiresRoles({RoleCodeConstants.ADMIN,RoleCodeConstants.WORKSHOP_MANAGER,RoleCodeConstants.TEAM_LEADER})
    public CommonResult<Void> start(@PathVariable @Positive Long id){service.startTask(id);return CommonResult.success(null);}
    @PutMapping("/{id}/pause") @RequiresRoles({RoleCodeConstants.ADMIN,RoleCodeConstants.WORKSHOP_MANAGER,RoleCodeConstants.TEAM_LEADER})
    public CommonResult<Void> pause(@PathVariable @Positive Long id,@Valid @RequestBody SceneTaskReasonReqVO req){service.pauseTask(id,req.getReason());return CommonResult.success(null);}
    @PutMapping("/{id}/resume") @RequiresRoles({RoleCodeConstants.ADMIN,RoleCodeConstants.WORKSHOP_MANAGER,RoleCodeConstants.TEAM_LEADER})
    public CommonResult<Void> resume(@PathVariable @Positive Long id){service.resumeTask(id);return CommonResult.success(null);}
    @PutMapping("/{id}/close") @RequiresRoles({RoleCodeConstants.ADMIN,RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Void> close(@PathVariable @Positive Long id,@Valid @RequestBody SceneTaskReasonReqVO req){service.closeTask(id,req.getReason());return CommonResult.success(null);}
    @GetMapping("/{id}") public CommonResult<SceneProductionTaskRespVO> get(@PathVariable @Positive Long id){return CommonResult.success(service.getTask(id));}
    @GetMapping("/page") public CommonResult<PageResult<SceneProductionTaskRespVO>> page(@Valid SceneProductionTaskPageReqVO req){return CommonResult.success(service.getTaskPage(req));}
    @GetMapping("/{id}/progress") public CommonResult<SceneTaskProgressRespVO> progress(@PathVariable @Positive Long id){return CommonResult.success(service.getTaskProgress(id));}
}
