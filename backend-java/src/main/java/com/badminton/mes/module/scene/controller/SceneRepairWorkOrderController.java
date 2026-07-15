package com.badminton.mes.module.scene.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.module.scene.controller.vo.*;
import com.badminton.mes.module.scene.service.SceneRepairWorkOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 返修工单接口。 @author 刘涵 */
@Validated @RestController @RequestMapping("/api/scene/repair_work_orders")
public class SceneRepairWorkOrderController {
    private final SceneRepairWorkOrderService service;
    public SceneRepairWorkOrderController(SceneRepairWorkOrderService service) { this.service = service; }
    @PostMapping public CommonResult<?> create(@Valid @RequestBody SceneRepairCreateReqVO request) { return CommonResult.success(service.create(request)); }
    @PutMapping("/{id}/assign") public CommonResult<?> assign(@PathVariable @NotNull Long id, @RequestParam Long assigneeId) { service.assign(id, assigneeId); return CommonResult.success(null); }
    @PutMapping("/{id}/start") public CommonResult<?> start(@PathVariable @NotNull Long id) { service.start(id); return CommonResult.success(null); }
    @PostMapping("/{id}/records") public CommonResult<?> record(@PathVariable @NotNull Long id, @Valid @RequestBody SceneRepairRecordCreateReqVO request) { service.addRecord(id, request); return CommonResult.success(null); }
    @PostMapping("/{id}/recheck") public CommonResult<?> recheck(@PathVariable @NotNull Long id, @Valid @RequestBody SceneRepairRecheckReqVO request) { service.recheck(id, request); return CommonResult.success(null); }
    @PutMapping("/{id}/close") public CommonResult<?> close(@PathVariable @NotNull Long id) { service.close(id); return CommonResult.success(null); }
    @GetMapping("/{id}") public CommonResult<?> get(@PathVariable @NotNull Long id) { return CommonResult.success(service.get(id)); }
}
