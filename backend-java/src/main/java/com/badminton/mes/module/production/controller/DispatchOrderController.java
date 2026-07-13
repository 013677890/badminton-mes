package com.badminton.mes.module.production.controller;

import java.time.LocalDate;
import java.util.List;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.production.controller.vo.DispatchAdjustLogRespVO;
import com.badminton.mes.module.production.controller.vo.DispatchPageReqVO;
import com.badminton.mes.module.production.controller.vo.DispatchRespVO;
import com.badminton.mes.module.production.controller.vo.DispatchSaveReqVO;
import com.badminton.mes.module.production.controller.vo.DispatchSuggestRespVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderReasonReqVO;
import com.badminton.mes.module.production.service.DispatchOrderService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

/**
 * 派工单 Controller。
 *
 * <p>创建/修改/取消放开到车间主管(执行侧参与排产调整)，审核与下发限
 * 管理员与车间主管，查询登录即可(权限矩阵见 wiki/16-齐套分析与派工单设计.md)。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@RestController
@RequestMapping("/api/production/dispatch_orders")
public class DispatchOrderController {

    private final DispatchOrderService dispatchOrderService;

    /**
     * 构造器注入，依赖不可变。
     *
     * @param dispatchOrderService 派工单 Service
     */
    public DispatchOrderController(DispatchOrderService dispatchOrderService) {
        this.dispatchOrderService = dispatchOrderService;
    }

    /**
     * 创建派工单。
     *
     * @param reqVO 创建请求，字段规则见 {@link DispatchSaveReqVO}
     * @return 新派工单主键 id
     */
    @PostMapping
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC, RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Long> createDispatch(@Valid @RequestBody DispatchSaveReqVO reqVO) {
        return CommonResult.success(dispatchOrderService.createDispatch(reqVO));
    }

    /**
     * 排产建议(只读)：按交期内工作日与产线班次剩余产能贪心填充。
     *
     * @param workOrderId 工单主键
     * @return 建议列表，无可行排产时为空集合(API-002)
     */
    @GetMapping("/suggest")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC, RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<List<DispatchSuggestRespVO>> suggestDispatch(
            @RequestParam("work_order_id") @Positive Long workOrderId) {
        return CommonResult.success(dispatchOrderService.suggestDispatch(workOrderId));
    }

    /**
     * 分页查询派工单列表。
     *
     * @param reqVO 分页筛选条件，GET 查询参数绑定
     * @return 分页结果，无数据时 list 为空集合(API-002)
     */
    @GetMapping("/page")
    public CommonResult<PageResult<DispatchRespVO>> getDispatchPage(@Valid DispatchPageReqVO reqVO) {
        return CommonResult.success(dispatchOrderService.getDispatchPage(reqVO));
    }

    /**
     * 产线排程视图：产线在日期区间内的派工单(排除已取消)。
     *
     * @param lineId    产线主键
     * @param startDate 起始日期(含)
     * @param endDate   结束日期(含)
     * @return 派工单列表，按日期/班次升序，无数据时为空集合(API-002)
     */
    @GetMapping("/schedule")
    public CommonResult<List<DispatchRespVO>> getLineSchedule(
            @RequestParam("line_id") @Positive Long lineId,
            @RequestParam("start_date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam("end_date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return CommonResult.success(dispatchOrderService.getLineSchedule(lineId, startDate, endDate));
    }

    /**
     * 查询派工单详情。
     *
     * @param id 派工单主键
     * @return 派工单详情
     */
    @GetMapping("/{id}")
    public CommonResult<DispatchRespVO> getDispatch(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(dispatchOrderService.getDispatch(id));
    }

    /**
     * 查询派工单调整日志，最新在前。
     *
     * @param id 派工单主键
     * @return 调整日志列表，无数据时为空集合(API-002)
     */
    @GetMapping("/{id}/adjust_logs")
    public CommonResult<List<DispatchAdjustLogRespVO>> getAdjustLogs(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(dispatchOrderService.getAdjustLogs(id));
    }

    /**
     * 修改派工单：待审核/已审核直接改，已下发必填调整原因。
     *
     * @param id    派工单主键
     * @param reqVO 修改请求，请求中的工单 id 被忽略(不允许换工单)
     * @return 空数据成功响应
     */
    @PutMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC, RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Void> updateDispatch(@PathVariable("id") @Positive Long id,
                                             @Valid @RequestBody DispatchSaveReqVO reqVO) {
        dispatchOrderService.updateDispatch(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 审核派工单：待审核 → 已审核。
     *
     * @param id 派工单主键
     * @return 空数据成功响应
     */
    @PutMapping("/{id}/audit")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Void> auditDispatch(@PathVariable("id") @Positive Long id) {
        dispatchOrderService.auditDispatch(id);
        return CommonResult.success(null);
    }

    /**
     * 下发派工单：已审核 → 已下发。
     *
     * @param id 派工单主键
     * @return 空数据成功响应
     */
    @PutMapping("/{id}/issue")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Void> issueDispatch(@PathVariable("id") @Positive Long id) {
        dispatchOrderService.issueDispatch(id);
        return CommonResult.success(null);
    }

    /**
     * 取消派工单：待审核/已审核/已下发 → 已取消，回退工单已派数量，原因必填。
     *
     * @param id    派工单主键
     * @param reqVO 取消原因
     * @return 空数据成功响应
     */
    @PutMapping("/{id}/cancel")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC, RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Void> cancelDispatch(@PathVariable("id") @Positive Long id,
                                             @Valid @RequestBody WorkOrderReasonReqVO reqVO) {
        dispatchOrderService.cancelDispatch(id, reqVO.getReason());
        return CommonResult.success(null);
    }
}
