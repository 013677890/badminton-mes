package com.badminton.mes.module.scene.controller;
import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.security.*;
import com.badminton.mes.module.scene.controller.vo.*;
import com.badminton.mes.module.scene.service.SceneWorkReportService;
import jakarta.validation.Valid;import jakarta.validation.constraints.Positive;import org.springframework.web.bind.annotation.*;
/** 生产报工 Controller。 @author 刘涵 */
@RestController @RequestMapping("/api/scene/work_reports") @org.springframework.validation.annotation.Validated
public class SceneWorkReportController{private final SceneWorkReportService service;public SceneWorkReportController(SceneWorkReportService s){service=s;}
 @PostMapping("/submit") @RequiresRoles({RoleCodeConstants.ADMIN,RoleCodeConstants.WORKSHOP_MANAGER,RoleCodeConstants.TEAM_LEADER,RoleCodeConstants.OPERATOR}) public CommonResult<Long> submit(@Valid @RequestBody SceneWorkReportSubmitReqVO r){return CommonResult.success(service.submit(r,1));}
 @PostMapping("/device_count") @RequiresRoles({RoleCodeConstants.ADMIN,RoleCodeConstants.WORKSHOP_MANAGER}) public CommonResult<Long> device(@Valid @RequestBody SceneWorkReportSubmitReqVO r){return CommonResult.success(service.submit(r,2));}
 @PutMapping("/{id}/reverse") @RequiresRoles({RoleCodeConstants.ADMIN,RoleCodeConstants.WORKSHOP_MANAGER}) public CommonResult<Long> reverse(@PathVariable @Positive Long id,@Valid @RequestBody SceneWorkReportReverseReqVO r){return CommonResult.success(service.reverse(id,r));}}
