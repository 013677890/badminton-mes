package com.badminton.mes.module.report.controller.kanban;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.module.report.service.kanban.KanbanSnapshotService;
import org.springframework.web.bind.annotation.*;

/** 电子看板快照接口。 @author 刘涵 */
@RestController @RequestMapping("/api/report/kanban")
public class KanbanController {
    private final KanbanSnapshotService service;
    public KanbanController(KanbanSnapshotService service) { this.service = service; }
    @GetMapping("/lines/{lineId}") public CommonResult<?> line(@PathVariable Long lineId) { return CommonResult.success(service.get("line", lineId)); }
    @GetMapping("/workshops/{workshopId}") public CommonResult<?> workshop(@PathVariable Long workshopId) { return CommonResult.success(service.get("workshop", workshopId)); }
    @GetMapping("/central") public CommonResult<?> central() { return CommonResult.success(service.get("central", null)); }
}
