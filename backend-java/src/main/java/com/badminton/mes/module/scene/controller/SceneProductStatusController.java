package com.badminton.mes.module.scene.controller;
import java.util.List;import com.badminton.mes.common.core.*;import com.badminton.mes.module.scene.controller.vo.*;
import com.badminton.mes.module.scene.service.SceneProductStatusService;import jakarta.validation.Valid;import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
/** 产品生产状态 Controller。 @author 刘涵 */
@RestController @RequestMapping("/api/scene/product_statuses") @org.springframework.validation.annotation.Validated
public class SceneProductStatusController {
 private final SceneProductStatusService service;public SceneProductStatusController(SceneProductStatusService service){this.service=service;}
 @GetMapping("/by_batch/{batchCode}") public CommonResult<SceneProductStatusRespVO> byBatch(@PathVariable @Size(max=64) String batchCode){return CommonResult.success(service.getByBatch(batchCode));}
 @GetMapping("/page") public CommonResult<PageResult<SceneProductStatusRespVO>> page(@Valid SceneProductStatusPageReqVO req){return CommonResult.success(service.page(req));}
 @GetMapping("/{id}/histories") public CommonResult<List<SceneStatusHistoryRespVO>> histories(@PathVariable @Positive Long id){return CommonResult.success(service.histories(id));}
 @GetMapping("/{id}/operation_histories") public CommonResult<List<SceneProcessHistoryRespVO>> operationHistories(@PathVariable @Positive Long id){return CommonResult.success(service.processHistories(id));}
}
