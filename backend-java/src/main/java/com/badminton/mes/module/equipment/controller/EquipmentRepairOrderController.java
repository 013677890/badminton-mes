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
 * 设备报修任务 HTTP 接口。
 *
 * <p>报修任务记录设备从故障上报、派工、维修到结束或取消的处理过程。本控制器只承担参数绑定、
 * Bean Validation 校验、调用 {@link EquipmentRepairOrderService} 和统一响应包装；单号生成、设备与
 * 故障原理校验、状态流转、维修时间约束及设备状态联动由 Service 在事务内完成。
 *
 * @author 角色C
 * @date 2026/07/10
 */
@RestController
@RequestMapping("/api/equipment/repair-orders")
public class EquipmentRepairOrderController {

    /** 设备报修应用服务，负责报修状态机和设备状态的一致性。 */
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
     * 创建处于初始上报阶段的设备报修任务。
     *
     * @param reqVO 已通过字段校验的报修任务创建数据
     * @return 包含新报修任务主键的统一成功响应
     */
    @PostMapping
    public CommonResult<Long> createEquipmentRepairOrder(@Valid @RequestBody EquipmentRepairOrderSaveReqVO reqVO) {
        return CommonResult.success(repairOrderService.createEquipmentRepairOrder(reqVO));
    }

    /**
     * 修改报修任务并由 Service 校验目标状态、维修人员和时间字段之间的业务关系。
     *
     * @param id 报修任务主键，必须为正数
     * @param reqVO 已通过字段校验的报修任务更新数据
     * @return 不携带业务数据的统一成功响应
     */
    @PutMapping("/{id}")
    public CommonResult<Void> updateEquipmentRepairOrder(@PathVariable("id") @Positive Long id,
                                                          @Valid @RequestBody EquipmentRepairOrderSaveReqVO reqVO) {
        repairOrderService.updateEquipmentRepairOrder(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 逻辑删除指定报修任务；可删除状态及设备状态恢复规则由 Service 控制。
     *
     * @param id 报修任务主键，必须为正数
     * @return 不携带业务数据的统一成功响应
     */
    @DeleteMapping("/{id}")
    public CommonResult<Void> deleteEquipmentRepairOrder(@PathVariable("id") @Positive Long id) {
        repairOrderService.deleteEquipmentRepairOrder(id);
        return CommonResult.success(null);
    }

    /**
     * 按主键查询设备报修任务的上报与维修处理详情。
     *
     * @param id 报修任务主键，必须为正数
     * @return 设备报修任务详情统一响应
     */
    @GetMapping("/{id}")
    public CommonResult<EquipmentRepairOrderRespVO> getEquipmentRepairOrder(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(repairOrderService.getEquipmentRepairOrder(id));
    }

    /**
     * 按关键字、设备、故障原理、报修状态和报修时间区间分页查询任务。
     *
     * @param reqVO 由 GET 查询参数绑定形成的分页筛选条件
     * @return 设备报修任务分页结果
     */
    @GetMapping("/page")
    public CommonResult<PageResult<EquipmentRepairOrderRespVO>> getEquipmentRepairOrderPage(
            @Valid EquipmentRepairOrderPageReqVO reqVO) {
        return CommonResult.success(repairOrderService.getEquipmentRepairOrderPage(reqVO));
    }
}
