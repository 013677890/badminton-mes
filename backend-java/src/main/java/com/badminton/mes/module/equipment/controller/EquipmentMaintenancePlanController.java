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

/** 设备保养计划 Controller。 */
@RestController
@RequestMapping("/api/equipment/maintenance-plans")
public class EquipmentMaintenancePlanController {

    private final EquipmentMaintenancePlanService maintenancePlanService;

    public EquipmentMaintenancePlanController(EquipmentMaintenancePlanService maintenancePlanService) {
        this.maintenancePlanService = maintenancePlanService;
    }

    @PostMapping
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Long> createEquipmentMaintenancePlan(
            @Valid @RequestBody EquipmentMaintenancePlanSaveReqVO reqVO) {
        return CommonResult.success(maintenancePlanService.createEquipmentMaintenancePlan(reqVO));
    }

    @PutMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Void> updateEquipmentMaintenancePlan(
            @PathVariable("id") @Positive Long id,
            @Valid @RequestBody EquipmentMaintenancePlanSaveReqVO reqVO) {
        maintenancePlanService.updateEquipmentMaintenancePlan(id, reqVO);
        return CommonResult.success(null);
    }

    @DeleteMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Void> deleteEquipmentMaintenancePlan(@PathVariable("id") @Positive Long id) {
        maintenancePlanService.deleteEquipmentMaintenancePlan(id);
        return CommonResult.success(null);
    }

    @GetMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER,
            RoleCodeConstants.TEAM_LEADER, RoleCodeConstants.OPERATOR})
    public CommonResult<EquipmentMaintenancePlanRespVO> getEquipmentMaintenancePlan(
            @PathVariable("id") @Positive Long id) {
        return CommonResult.success(maintenancePlanService.getEquipmentMaintenancePlan(id));
    }

    @GetMapping("/page")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER,
            RoleCodeConstants.TEAM_LEADER, RoleCodeConstants.OPERATOR})
    public CommonResult<PageResult<EquipmentMaintenancePlanRespVO>> getEquipmentMaintenancePlanPage(
            @Valid EquipmentMaintenancePlanPageReqVO reqVO) {
        return CommonResult.success(maintenancePlanService.getEquipmentMaintenancePlanPage(reqVO));
    }
}
