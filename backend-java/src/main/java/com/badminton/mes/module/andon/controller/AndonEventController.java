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

/**
 * 现场安灯异常事件接口。
 *
 * <p>负责异常发起、确认、开始处理、转派、完成、关闭与升级等完整生命周期动作，并提供事件详情、
 * 分页列表和过程日志查询。写操作由服务层校验当前状态与实际处理权限，控制器上的角色注解限定可进入端点的角色范围。
 */
@RestController
@RequestMapping("/api/andon/events")
public class AndonEventController {

    /** 承载状态机迁移、处理人校验、超时升级、通知记录及灯控状态变更的事件服务。 */
    private final AndonEventService eventService;

    /**
     * 注入安灯事件服务。
     *
     * @param eventService 安灯事件业务服务
     */
    public AndonEventController(AndonEventService eventService) {
        this.eventService = eventService;
    }

    /**
     * 发起安灯异常事件。
     *
     * <p>管理员、车间主管、班组长、操作员和检验员均可发起。事件初始状态、默认指派、响应/升级期限、
     * 通知渠道及是否触发灯控由安灯类型和匹配到的处理配置共同决定。
     *
     * @param request 事件来源、异常类型、业务关联对象及异常描述
     * @return 新建事件的主键
     */
    @PostMapping
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR, INSPECTOR})
    public CommonResult<Long> create(@Valid @RequestBody AndonEventCreateReqVO request) {
        return CommonResult.success(eventService.createEvent(request));
    }

    /**
     * 确认待确认事件。
     *
     * <p>事件必须处于 {@code PENDING_CONFIRMATION}，且当前用户须满足具体处理人、处理角色或管理角色规则；
     * 确认后进入 {@code CONFIRMED}，记录实际原因、确认人和确认时间，并停止响应期限计时。
     *
     * @param id 事件主键
     * @param request 实际原因与确认说明
     * @return 无业务数据的成功结果
     */
    @PutMapping("/{id}/confirm")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR, INSPECTOR})
    public CommonResult<Void> confirm(
            @PathVariable @Positive Long id,
            @Valid @RequestBody AndonEventActionReqVO request) {
        eventService.confirmEvent(id, request);
        return CommonResult.success(null);
    }

    /**
     * 开始处理已确认事件。
     *
     * <p>事件必须处于 {@code CONFIRMED} 且当前用户具备实际处理权限；成功后迁移至 {@code PROCESSING}，
     * 同步写入过程日志并发送状态通知。
     *
     * @param id 事件主键
     * @param request 本次开始处理的操作说明
     * @return 无业务数据的成功结果
     */
    @PutMapping("/{id}/start-processing")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR, INSPECTOR})
    public CommonResult<Void> startProcessing(
            @PathVariable @Positive Long id,
            @Valid @RequestBody AndonEventActionReqVO request) {
        eventService.startProcessing(id, request);
        return CommonResult.success(null);
    }

    /**
     * 转派确认后或处理中的事件。
     *
     * <p>转派不会改变事件状态，只更新当前指派用户和/或指派角色；服务层要求调用者具备处理权限，
     * 并校验目标用户、目标角色有效，同时要求填写操作说明以保留责任流转依据。
     *
     * @param id 事件主键
     * @param request 新处理人、新处理角色及转派说明
     * @return 无业务数据的成功结果
     */
    @PutMapping("/{id}/transfer")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR, INSPECTOR})
    public CommonResult<Void> transfer(
            @PathVariable @Positive Long id,
            @Valid @RequestBody AndonEventActionReqVO request) {
        eventService.transferEvent(id, request);
        return CommonResult.success(null);
    }

    /**
     * 提交事件处理完成结果。
     *
     * <p>事件必须处于 {@code PROCESSING} 且当前用户具备处理权限；成功后进入 {@code WAITING_CLOSE}，
     * 固化实际原因、处理结果、影响时长和影响数量，并清除未到期的响应及升级期限。
     *
     * @param id 事件主键
     * @param request 处理结论、影响数据及操作说明
     * @return 无业务数据的成功结果
     */
    @PutMapping("/{id}/complete")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR, INSPECTOR})
    public CommonResult<Void> complete(
            @PathVariable @Positive Long id,
            @Valid @RequestBody AndonEventActionReqVO request) {
        eventService.completeEvent(id, request);
        return CommonResult.success(null);
    }

    /**
     * 管理确认并关闭待关闭事件。
     *
     * <p>仅管理员、车间主管和班组长可调用，事件必须处于 {@code WAITING_CLOSE}；关闭后进入
     * {@code CLOSED}，记录关闭人和关闭时间，清除超时期限，并在灯已开启时同步标记为关闭。
     *
     * @param id 事件主键
     * @param request 关闭说明
     * @return 无业务数据的成功结果
     */
    @PutMapping("/{id}/close")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER})
    public CommonResult<Void> close(
            @PathVariable @Positive Long id,
            @Valid @RequestBody AndonEventActionReqVO request) {
        eventService.closeEvent(id, request);
        return CommonResult.success(null);
    }

    /**
     * 人工升级确认后或处理中的事件。
     *
     * <p>调用者须具备事件处理权限且事件尚未升级。请求可显式指定升级用户/角色；未指定时由产线配置、
     * 全局配置或类型默认规则补齐。升级保持业务状态不变，将超时状态置为 {@code ESCALATED} 并重新指派责任主体。
     *
     * @param id 事件主键
     * @param request 可选升级对象及升级说明
     * @return 无业务数据的成功结果
     */
    @PutMapping("/{id}/escalate")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR, INSPECTOR})
    public CommonResult<Void> escalate(
            @PathVariable @Positive Long id,
            @Valid @RequestBody AndonEventActionReqVO request) {
        eventService.escalateEvent(id, request);
        return CommonResult.success(null);
    }

    /**
     * 查询单个安灯事件详情。
     *
     * <p>所有参与安灯业务的角色均可读取；响应包含事件状态、当前指派、期限、超时状态、灯控结果、
     * 关键时间点以及完整过程日志和通知记录。
     *
     * @param id 事件主键
     * @return 事件聚合详情
     */
    @GetMapping("/{id}")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR, INSPECTOR})
    public CommonResult<AndonEventRespVO> get(@PathVariable @Positive Long id) {
        return CommonResult.success(eventService.getEvent(id));
    }

    /**
     * 按组合条件分页查询安灯事件。
     *
     * <p>所有参与安灯业务的角色均可查询，可按类型、产线、设备、发起人、处理人/角色、来源渠道、
     * 事件状态、异常级别和超时状态筛选；分页项不携带过程日志与通知明细。
     *
     * @param request 分页参数与事件筛选条件
     * @return 事件分页结果
     */
    @GetMapping("/page")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR, INSPECTOR})
    public CommonResult<PageResult<AndonEventRespVO>> page(@Valid AndonEventPageReqVO request) {
        return CommonResult.success(eventService.getEventPage(request));
    }

    /**
     * 查询事件过程日志。
     *
     * <p>日志按发生顺序返回，用于还原发起、确认、处理、转派、完成、升级和关闭过程中每一次状态及指派变化。
     *
     * @param id 事件主键
     * @return 事件过程日志列表
     */
    @GetMapping("/{id}/process-logs")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR, INSPECTOR})
    public CommonResult<List<AndonProcessLogRespVO>> processLogs(@PathVariable @Positive Long id) {
        return CommonResult.success(eventService.getProcessLogs(id));
    }
}
