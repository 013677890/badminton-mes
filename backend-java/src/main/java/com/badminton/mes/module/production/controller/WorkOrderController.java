package com.badminton.mes.module.production.controller;

import java.util.List;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.production.controller.vo.WorkOrderMaterialRespVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderPageReqVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderReasonReqVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderRespVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderSaveReqVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderStatusLogRespVO;
import com.badminton.mes.module.production.service.WorkOrderService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

/**
 * 生产工单 Controller。
 *
 * <p>Web 层职责保持单薄：声明式参数校验、转发 Service、包装统一响应，
 * 不写业务规则(工程结构分层规约)。路径全小写、单词用下划线分隔、资源名词用
 * 复数(API-001)；动作"下达"采用业界惯用的动作子路径 {@code /{id}/release}，
 * 若需严格名词化可改为对状态资源的更新。
 *
 * <p>校验失败由 {@code GlobalExceptionHandler} 统一转为 A0400 响应；
 * 请求体字段校验触发 MethodArgumentNotValidException，路径参数约束触发
 * HandlerMethodValidationException(Spring 6.1+ 内建方法校验)。
 *
 * @author 张竹灏
 * @date 2026/07/07
 */
@RestController
@RequestMapping("/api/production/work_orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    /**
     * 构造器注入，依赖不可变。
     *
     * @param workOrderService 工单 Service
     */
    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    /**
     * 创建生产工单。
     *
     * @param reqVO 创建请求，字段规则见 {@link WorkOrderSaveReqVO}
     * @return 新工单主键 id
     */
    @PostMapping
    public CommonResult<Long> createWorkOrder(@Valid @RequestBody WorkOrderSaveReqVO reqVO) {
        return CommonResult.success(workOrderService.createWorkOrder(reqVO));
    }

    /**
     * 修改生产工单计划信息，仅"已创建"状态允许修改。
     *
     * @param id    工单主键
     * @param reqVO 修改请求，请求中的工单号被忽略
     * @return 空数据成功响应
     */
    @PutMapping("/{id}")
    public CommonResult<Void> updateWorkOrder(@PathVariable("id") @Positive Long id,
                                              @Valid @RequestBody WorkOrderSaveReqVO reqVO) {
        workOrderService.updateWorkOrder(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 删除生产工单(逻辑删除)，仅"已创建"状态允许删除。
     *
     * @param id 工单主键
     * @return 空数据成功响应
     */
    @DeleteMapping("/{id}")
    public CommonResult<Void> deleteWorkOrder(@PathVariable("id") @Positive Long id) {
        workOrderService.deleteWorkOrder(id);
        return CommonResult.success(null);
    }

    /**
     * 下达生产工单：已创建 → 已下达，并按 BOM 生成工单物料需求。
     *
     * @param id 工单主键
     * @return 空数据成功响应
     */
    @PutMapping("/{id}/release")
    public CommonResult<Void> releaseWorkOrder(@PathVariable("id") @Positive Long id) {
        workOrderService.releaseWorkOrder(id);
        return CommonResult.success(null);
    }

    /**
     * 暂停生产工单：已下达/生产中 → 暂停，原因必填。
     *
     * @param id    工单主键
     * @param reqVO 暂停原因
     * @return 空数据成功响应
     */
    @PutMapping("/{id}/pause")
    public CommonResult<Void> pauseWorkOrder(@PathVariable("id") @Positive Long id,
                                             @Valid @RequestBody WorkOrderReasonReqVO reqVO) {
        workOrderService.pauseWorkOrder(id, reqVO.getReason());
        return CommonResult.success(null);
    }

    /**
     * 恢复生产工单：暂停 → 暂停前状态。
     *
     * @param id 工单主键
     * @return 空数据成功响应
     */
    @PutMapping("/{id}/resume")
    public CommonResult<Void> resumeWorkOrder(@PathVariable("id") @Positive Long id) {
        workOrderService.resumeWorkOrder(id);
        return CommonResult.success(null);
    }

    /**
     * 完工生产工单：已下达/生产中 → 已完工。
     *
     * @param id 工单主键
     * @return 空数据成功响应
     */
    @PutMapping("/{id}/finish")
    public CommonResult<Void> finishWorkOrder(@PathVariable("id") @Positive Long id) {
        workOrderService.finishWorkOrder(id);
        return CommonResult.success(null);
    }

    /**
     * 关闭生产工单：已完工 → 已关闭。
     *
     * @param id 工单主键
     * @return 空数据成功响应
     */
    @PutMapping("/{id}/close")
    public CommonResult<Void> closeWorkOrder(@PathVariable("id") @Positive Long id) {
        workOrderService.closeWorkOrder(id);
        return CommonResult.success(null);
    }

    /**
     * 作废生产工单：已创建/已下达 → 已作废，原因必填。
     *
     * @param id    工单主键
     * @param reqVO 作废原因
     * @return 空数据成功响应
     */
    @PutMapping("/{id}/cancel")
    public CommonResult<Void> cancelWorkOrder(@PathVariable("id") @Positive Long id,
                                              @Valid @RequestBody WorkOrderReasonReqVO reqVO) {
        workOrderService.cancelWorkOrder(id, reqVO.getReason());
        return CommonResult.success(null);
    }

    /**
     * 查询工单物料需求明细。
     *
     * @param id 工单主键
     * @return 物料需求列表，未下达时为空集合(API-002)
     */
    @GetMapping("/{id}/materials")
    public CommonResult<List<WorkOrderMaterialRespVO>> getWorkOrderMaterials(
            @PathVariable("id") @Positive Long id) {
        return CommonResult.success(workOrderService.getWorkOrderMaterials(id));
    }

    /**
     * 查询工单状态日志，最新在前。
     *
     * @param id 工单主键
     * @return 状态日志列表，无数据时为空集合(API-002)
     */
    @GetMapping("/{id}/status_logs")
    public CommonResult<List<WorkOrderStatusLogRespVO>> getWorkOrderStatusLogs(
            @PathVariable("id") @Positive Long id) {
        return CommonResult.success(workOrderService.getWorkOrderStatusLogs(id));
    }

    /**
     * 查询工单详情(带 Redis 缓存)。
     *
     * @param id 工单主键
     * @return 工单详情
     */
    @GetMapping("/{id}")
    public CommonResult<WorkOrderRespVO> getWorkOrder(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(workOrderService.getWorkOrder(id));
    }

    /**
     * 分页查询工单列表。
     *
     * @param reqVO 分页筛选条件，GET 查询参数绑定
     * @return 分页结果，无数据时 list 为空集合(API-002)
     */
    @GetMapping("/page")
    public CommonResult<PageResult<WorkOrderRespVO>> getWorkOrderPage(@Valid WorkOrderPageReqVO reqVO) {
        return CommonResult.success(workOrderService.getWorkOrderPage(reqVO));
    }
}
