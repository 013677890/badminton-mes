package com.badminton.mes.module.equipment.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.equipment.controller.vo.EquipmentRepairOrderPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentRepairOrderRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentRepairOrderSaveReqVO;
import com.badminton.mes.module.equipment.service.EquipmentRepairOrderService;

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
 * 设备报修任务 Controller。
 *
 * @author 角色C
 * @date 2026/07/10
 */
@RestController
@RequestMapping("/api/equipment/repair-orders")
public class EquipmentRepairOrderController {

    private final EquipmentRepairOrderService repairOrderService;

    /**
     * 构造器注入，依赖不可变。
     *
     * @param repairOrderService 设备报修任务 Service
     */
    public EquipmentRepairOrderController(EquipmentRepairOrderService repairOrderService) {
        this.repairOrderService = repairOrderService;
    }

    /**
     * 创建设备报修任务。
     *
     * @param reqVO 创建请求
     * @return 新报修任务主键 id
     */
    @PostMapping
    public CommonResult<Long> createEquipmentRepairOrder(@Valid @RequestBody EquipmentRepairOrderSaveReqVO reqVO) {
        return CommonResult.success(repairOrderService.createEquipmentRepairOrder(reqVO));
    }

    /**
     * 修改设备报修任务。
     *
     * @param id    报修任务主键
     * @param reqVO 修改请求
     * @return 空数据成功响应
     */
    @PutMapping("/{id}")
    public CommonResult<Void> updateEquipmentRepairOrder(@PathVariable("id") @Positive Long id,
                                                          @Valid @RequestBody EquipmentRepairOrderSaveReqVO reqVO) {
        repairOrderService.updateEquipmentRepairOrder(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 删除设备报修任务(逻辑删除)。
     *
     * @param id 报修任务主键
     * @return 空数据成功响应
     */
    @DeleteMapping("/{id}")
    public CommonResult<Void> deleteEquipmentRepairOrder(@PathVariable("id") @Positive Long id) {
        repairOrderService.deleteEquipmentRepairOrder(id);
        return CommonResult.success(null);
    }

    /**
     * 查询设备报修任务详情。
     *
     * @param id 报修任务主键
     * @return 报修任务详情
     */
    @GetMapping("/{id}")
    public CommonResult<EquipmentRepairOrderRespVO> getEquipmentRepairOrder(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(repairOrderService.getEquipmentRepairOrder(id));
    }

    /**
     * 分页查询设备报修任务列表。
     *
     * @param reqVO 分页筛选条件，GET 查询参数绑定
     * @return 分页结果
     */
    @GetMapping("/page")
    public CommonResult<PageResult<EquipmentRepairOrderRespVO>> getEquipmentRepairOrderPage(
            @Valid EquipmentRepairOrderPageReqVO reqVO) {
        return CommonResult.success(repairOrderService.getEquipmentRepairOrderPage(reqVO));
    }
}
