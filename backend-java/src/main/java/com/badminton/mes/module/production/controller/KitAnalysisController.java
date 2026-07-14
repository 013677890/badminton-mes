package com.badminton.mes.module.production.controller;

import java.util.List;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.production.controller.vo.KitAnalysisRespVO;
import com.badminton.mes.module.production.controller.vo.ShortageBoardRespVO;
import com.badminton.mes.module.production.controller.vo.ShortageHandleRespVO;
import com.badminton.mes.module.production.controller.vo.ShortageHandleSaveReqVO;
import com.badminton.mes.module.production.controller.vo.ShortageOrderRespVO;
import com.badminton.mes.module.production.service.KitAnalysisService;

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
 * 齐套分析 Controller。
 *
 * <p>触发分析与欠料处理登记限计划/管理职能，查询接口登录即可
 * (权限矩阵见 wiki/16-齐套分析与派工单设计.md)。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@RestController
@RequestMapping("/api/production/kit_analysis")
public class KitAnalysisController {

    private final KitAnalysisService kitAnalysisService;

    /**
     * 构造器注入，依赖不可变。
     *
     * @param kitAnalysisService 齐套分析 Service
     */
    public KitAnalysisController(KitAnalysisService kitAnalysisService) {
        this.kitAnalysisService = kitAnalysisService;
    }

    /**
     * 执行(重新)齐套分析。
     *
     * @param workOrderId 工单主键
     * @return 工单级齐套状态(1 齐套 2 部分齐套 3 欠料)
     */
    @PostMapping("/work_orders/{work_order_id}/analyze")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC, RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Integer> analyzeWorkOrder(@PathVariable("work_order_id") @Positive Long workOrderId) {
        return CommonResult.success(kitAnalysisService.analyzeWorkOrder(workOrderId));
    }

    /**
     * 查询工单最新齐套分析结果。
     *
     * @param workOrderId 工单主键
     * @return 逐物料分析结果，未分析时为空集合(API-002)
     */
    @GetMapping("/work_orders/{work_order_id}")
    public CommonResult<List<KitAnalysisRespVO>> getKitResult(
            @PathVariable("work_order_id") @Positive Long workOrderId) {
        return CommonResult.success(kitAnalysisService.getKitResult(workOrderId));
    }

    /**
     * 欠料看板汇总。
     *
     * @return 按物料聚合的欠料汇总，欠料量降序，无数据时为空集合(API-002)
     */
    @GetMapping("/shortage_board")
    public CommonResult<List<ShortageBoardRespVO>> getShortageBoard() {
        return CommonResult.success(kitAnalysisService.getShortageBoard());
    }

    /**
     * 欠料看板下钻：某物料影响的工单明细。
     *
     * @param materialId 物料主键
     * @return 欠料工单行列表，无数据时为空集合(API-002)
     */
    @GetMapping("/shortage_board/materials/{material_id}/work_orders")
    public CommonResult<List<ShortageOrderRespVO>> getShortageOrdersByMaterial(
            @PathVariable("material_id") @Positive Long materialId) {
        return CommonResult.success(kitAnalysisService.getShortageOrdersByMaterial(materialId));
    }

    /**
     * 新增欠料处理记录。
     *
     * @param reqVO 处理记录请求
     * @return 新记录主键
     */
    @PostMapping("/shortage_handles")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Long> createShortageHandle(@Valid @RequestBody ShortageHandleSaveReqVO reqVO) {
        return CommonResult.success(kitAnalysisService.createShortageHandle(reqVO));
    }

    /**
     * 标记欠料处理记录为已解决。
     *
     * @param id 处理记录主键
     * @return 空数据成功响应
     */
    @PutMapping("/shortage_handles/{id}/resolve")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> resolveShortageHandle(@PathVariable("id") @Positive Long id) {
        kitAnalysisService.resolveShortageHandle(id);
        return CommonResult.success(null);
    }

    /**
     * 查询工单的欠料处理记录。
     *
     * @param workOrderId 工单主键
     * @return 处理记录列表，最新在前，无数据时为空集合(API-002)
     */
    @GetMapping("/work_orders/{work_order_id}/shortage_handles")
    public CommonResult<List<ShortageHandleRespVO>> getShortageHandles(
            @PathVariable("work_order_id") @Positive Long workOrderId) {
        return CommonResult.success(kitAnalysisService.getShortageHandles(workOrderId));
    }
}
