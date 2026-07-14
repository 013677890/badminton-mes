package com.badminton.mes.module.andon.controller;

import java.util.List;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.module.andon.controller.vo.AndonEventActionReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonEventCreateReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonEventPageReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonEventRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonProcessLogRespVO;
import com.badminton.mes.module.andon.service.AndonEventService;

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

/** 现场安灯异常接口。 */
@RestController
@RequestMapping("/api/andon/events")
public class AndonEventController {

    private final AndonEventService eventService;

    public AndonEventController(AndonEventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR, INSPECTOR})
    public CommonResult<Long> create(@Valid @RequestBody AndonEventCreateReqVO request) {
        return CommonResult.success(eventService.createEvent(request));
    }

    @PutMapping("/{id}/confirm")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR, INSPECTOR})
    public CommonResult<Void> confirm(
            @PathVariable @Positive Long id,
            @Valid @RequestBody AndonEventActionReqVO request) {
        eventService.confirmEvent(id, request);
        return CommonResult.success(null);
    }

    @PutMapping("/{id}/start-processing")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR, INSPECTOR})
    public CommonResult<Void> startProcessing(
            @PathVariable @Positive Long id,
            @Valid @RequestBody AndonEventActionReqVO request) {
        eventService.startProcessing(id, request);
        return CommonResult.success(null);
    }

    @PutMapping("/{id}/transfer")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR, INSPECTOR})
    public CommonResult<Void> transfer(
            @PathVariable @Positive Long id,
            @Valid @RequestBody AndonEventActionReqVO request) {
        eventService.transferEvent(id, request);
        return CommonResult.success(null);
    }

    @PutMapping("/{id}/complete")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR, INSPECTOR})
    public CommonResult<Void> complete(
            @PathVariable @Positive Long id,
            @Valid @RequestBody AndonEventActionReqVO request) {
        eventService.completeEvent(id, request);
        return CommonResult.success(null);
    }

    @PutMapping("/{id}/close")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER})
    public CommonResult<Void> close(
            @PathVariable @Positive Long id,
            @Valid @RequestBody AndonEventActionReqVO request) {
        eventService.closeEvent(id, request);
        return CommonResult.success(null);
    }

    @PutMapping("/{id}/escalate")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR, INSPECTOR})
    public CommonResult<Void> escalate(
            @PathVariable @Positive Long id,
            @Valid @RequestBody AndonEventActionReqVO request) {
        eventService.escalateEvent(id, request);
        return CommonResult.success(null);
    }

    @GetMapping("/{id}")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR, INSPECTOR})
    public CommonResult<AndonEventRespVO> get(@PathVariable @Positive Long id) {
        return CommonResult.success(eventService.getEvent(id));
    }

    @GetMapping("/page")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR, INSPECTOR})
    public CommonResult<PageResult<AndonEventRespVO>> page(@Valid AndonEventPageReqVO request) {
        return CommonResult.success(eventService.getEventPage(request));
    }

    @GetMapping("/{id}/process-logs")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR, INSPECTOR})
    public CommonResult<List<AndonProcessLogRespVO>> processLogs(@PathVariable @Positive Long id) {
        return CommonResult.success(eventService.getProcessLogs(id));
    }
}
