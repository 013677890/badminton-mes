package com.badminton.mes.module.scene.controller;
import java.util.List;
import com.badminton.mes.common.core.*;import com.badminton.mes.common.security.*;
import com.badminton.mes.module.scene.controller.vo.*;import com.badminton.mes.module.scene.service.SceneDispatchOrderService;
import jakarta.validation.Valid;import jakarta.validation.constraints.Positive;import org.springframework.web.bind.annotation.*;
/** 工序派工 Controller。 @author 刘涵 */
@RestController @RequestMapping("/api/scene/dispatch_orders") @org.springframework.validation.annotation.Validated
public class SceneDispatchOrderController {
 private final SceneDispatchOrderService service;public SceneDispatchOrderController(SceneDispatchOrderService service){this.service=service;}
 @PostMapping("/generate") @RequiresRoles({RoleCodeConstants.ADMIN,RoleCodeConstants.WORKSHOP_MANAGER,RoleCodeConstants.TEAM_LEADER})
 public CommonResult<Long> generate(@Valid @RequestBody SceneDispatchGenerateReqVO req){return CommonResult.success(service.generate(req));}
 @PutMapping("/{id}/confirm") @RequiresRoles({RoleCodeConstants.ADMIN,RoleCodeConstants.WORKSHOP_MANAGER,RoleCodeConstants.TEAM_LEADER})
 public CommonResult<Void> confirm(@PathVariable @Positive Long id){service.confirm(id);return CommonResult.success(null);}
 @PutMapping("/{id}/cancel") @RequiresRoles({RoleCodeConstants.ADMIN,RoleCodeConstants.WORKSHOP_MANAGER,RoleCodeConstants.TEAM_LEADER})
 public CommonResult<Void> cancel(@PathVariable @Positive Long id){service.cancel(id);return CommonResult.success(null);}
 @GetMapping("/{id}") public CommonResult<SceneDispatchOrderRespVO> get(@PathVariable @Positive Long id){return CommonResult.success(service.get(id));}
 @GetMapping("/page") public CommonResult<PageResult<SceneDispatchOrderRespVO>> page(@Valid SceneDispatchPageReqVO req){return CommonResult.success(service.page(req));}
 @GetMapping("/{id}/operations") public CommonResult<List<SceneDispatchDetailRespVO>> operations(@PathVariable @Positive Long id){return CommonResult.success(service.operations(id));}
}
