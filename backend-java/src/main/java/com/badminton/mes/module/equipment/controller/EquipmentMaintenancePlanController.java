package com.badminton.mes.module.equipment.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenancePlanPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenancePlanRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenancePlanSaveReqVO;
import com.badminton.mes.module.equipment.service.EquipmentMaintenancePlanService;

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
 * 设备保养计划 HTTP 接口。
 *
 * <p>负责接收计划主数据的创建、修改、逻辑删除、详情和分页请求，并将参数校验后的命令交给
 * {@link EquipmentMaintenancePlanService}。控制器不计算保养周期，也不判断设备、负责人或历史记录
 * 是否可用，这些需要事务和跨聚合一致性的规则统一由 Service 层处理。
 *
 * <p>写操作仅向管理员和车间主任开放；查询操作额外允许班组长和操作工访问。角色注解是接口入口的
 * 权限边界，不能替代 Service 层的数据完整性校验。
 */
@RestController
@RequestMapping("/api/equipment/maintenance-plans")
public class EquipmentMaintenancePlanController {

    /** 保养计划应用服务，承载事务、引用校验和计划时间计算。 */
    private final EquipmentMaintenancePlanService maintenancePlanService;

    /**
     * 使用构造器注入保养计划服务，保证依赖在控制器生命周期内不可变。
     *
     * @param maintenancePlanService 保养计划应用服务
     */
    public EquipmentMaintenancePlanController(EquipmentMaintenancePlanService maintenancePlanService) {
        this.maintenancePlanService = maintenancePlanService;
    }

    /**
     * 创建保养计划。
     *
     * @param reqVO 已通过 Bean Validation 校验的计划数据
     * @return 包含新计划主键的统一成功响应
     */
    @PostMapping
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Long> createEquipmentMaintenancePlan(
            @Valid @RequestBody EquipmentMaintenancePlanSaveReqVO reqVO) {
        return CommonResult.success(maintenancePlanService.createEquipmentMaintenancePlan(reqVO));
    }

    /**
     * 修改指定保养计划；计划已有历史任务时能否更换设备由 Service 校验。
     *
     * @param id 计划主键，必须为正数
     * @param reqVO 修改后的计划数据
     * @return 不携带业务数据的统一成功响应
     */
    @PutMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Void> updateEquipmentMaintenancePlan(
            @PathVariable("id") @Positive Long id,
            @Valid @RequestBody EquipmentMaintenancePlanSaveReqVO reqVO) {
        maintenancePlanService.updateEquipmentMaintenancePlan(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 逻辑删除保养计划；存在保养记录的计划会被业务规则拒绝删除。
     *
     * @param id 计划主键，必须为正数
     * @return 不携带业务数据的统一成功响应
     */
    @DeleteMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Void> deleteEquipmentMaintenancePlan(@PathVariable("id") @Positive Long id) {
        maintenancePlanService.deleteEquipmentMaintenancePlan(id);
        return CommonResult.success(null);
    }

    /**
     * 查询保养计划详情。
     *
     * @param id 计划主键，必须为正数
     * @return 计划详情统一响应
     */
    @GetMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER,
            RoleCodeConstants.TEAM_LEADER, RoleCodeConstants.OPERATOR})
    public CommonResult<EquipmentMaintenancePlanRespVO> getEquipmentMaintenancePlan(
            @PathVariable("id") @Positive Long id) {
        return CommonResult.success(maintenancePlanService.getEquipmentMaintenancePlan(id));
    }

    /**
     * 按关键字、设备、类型、启停状态及下次保养时间区间分页查询计划。
     *
     * @param reqVO GET 查询参数绑定形成的分页条件
     * @return 页码经 Service 归一化后的分页结果
     */
    @GetMapping("/page")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER,
            RoleCodeConstants.TEAM_LEADER, RoleCodeConstants.OPERATOR})
    public CommonResult<PageResult<EquipmentMaintenancePlanRespVO>> getEquipmentMaintenancePlanPage(
            @Valid EquipmentMaintenancePlanPageReqVO reqVO) {
        return CommonResult.success(maintenancePlanService.getEquipmentMaintenancePlanPage(reqVO));
    }
}
