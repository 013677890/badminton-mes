package com.badminton.mes.module.scene.controller;
import java.util.List;import com.badminton.mes.common.core.*;import com.badminton.mes.common.security.*;
import com.badminton.mes.module.scene.controller.vo.*;import com.badminton.mes.module.scene.service.SceneOperationJobService;
import jakarta.validation.Valid;import jakarta.validation.constraints.Positive;import org.springframework.web.bind.annotation.*;
/** 工序作业 Controller。 @author 刘涵 */
@RestController @RequestMapping("/api/scene/operation_jobs") @org.springframework.validation.annotation.Validated
public class SceneOperationJobController {
 private final SceneOperationJobService service;public SceneOperationJobController(SceneOperationJobService service){this.service=service;}
 @GetMapping("/page") public CommonResult<PageResult<SceneDispatchDetailRespVO>> page(@Valid SceneOperationJobPageReqVO req){return CommonResult.success(service.page(req));}
 @GetMapping("/my") public CommonResult<List<SceneDispatchDetailRespVO>> my(){return CommonResult.success(service.my());}
 @GetMapping("/{id}") public CommonResult<SceneDispatchDetailRespVO> get(@PathVariable @Positive Long id){return CommonResult.success(service.get(id));}
 @PostMapping("/{id}/scan") @RequiresRoles({RoleCodeConstants.ADMIN,RoleCodeConstants.WORKSHOP_MANAGER,RoleCodeConstants.TEAM_LEADER,RoleCodeConstants.OPERATOR})
 public CommonResult<Void> scan(@PathVariable @Positive Long id,@Valid @RequestBody SceneOperationScanReqVO req){service.scan(id,req);return CommonResult.success(null);}
 @PutMapping("/{id}/start") @RequiresRoles({RoleCodeConstants.ADMIN,RoleCodeConstants.WORKSHOP_MANAGER,RoleCodeConstants.TEAM_LEADER,RoleCodeConstants.OPERATOR})
 public CommonResult<Void> start(@PathVariable @Positive Long id){service.start(id);return CommonResult.success(null);}
 @PutMapping("/{id}/pause") @RequiresRoles({RoleCodeConstants.ADMIN,RoleCodeConstants.WORKSHOP_MANAGER,RoleCodeConstants.TEAM_LEADER,RoleCodeConstants.OPERATOR})
 public CommonResult<Void> pause(@PathVariable @Positive Long id,@Valid @RequestBody SceneTaskReasonReqVO req){service.pause(id,req.getReason());return CommonResult.success(null);}
 @PutMapping("/{id}/finish") @RequiresRoles({RoleCodeConstants.ADMIN,RoleCodeConstants.WORKSHOP_MANAGER,RoleCodeConstants.TEAM_LEADER,RoleCodeConstants.OPERATOR})
 public CommonResult<Void> finish(@PathVariable @Positive Long id){service.finish(id);return CommonResult.success(null);}
}
