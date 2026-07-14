package com.badminton.mes.module.equipment.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenanceRecordPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenanceRecordRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenanceRecordSaveReqVO;
import com.badminton.mes.module.equipment.service.EquipmentMaintenanceRecordService;

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

/** 设备保养记录 Controller。 */
@RestController
@RequestMapping("/api/equipment/maintenance-records")
public class EquipmentMaintenanceRecordController {

    private final EquipmentMaintenanceRecordService maintenanceRecordService;

    public EquipmentMaintenanceRecordController(EquipmentMaintenanceRecordService maintenanceRecordService) {
        this.maintenanceRecordService = maintenanceRecordService;
    }

    @PostMapping
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER,
            RoleCodeConstants.TEAM_LEADER, RoleCodeConstants.OPERATOR})
    public CommonResult<Long> createEquipmentMaintenanceRecord(
            @Valid @RequestBody EquipmentMaintenanceRecordSaveReqVO reqVO) {
        return CommonResult.success(maintenanceRecordService.createEquipmentMaintenanceRecord(reqVO));
    }

    @PutMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER,
            RoleCodeConstants.TEAM_LEADER, RoleCodeConstants.OPERATOR})
    public CommonResult<Void> updateEquipmentMaintenanceRecord(
            @PathVariable("id") @Positive Long id,
            @Valid @RequestBody EquipmentMaintenanceRecordSaveReqVO reqVO) {
        maintenanceRecordService.updateEquipmentMaintenanceRecord(id, reqVO);
        return CommonResult.success(null);
    }

    @DeleteMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Void> deleteEquipmentMaintenanceRecord(@PathVariable("id") @Positive Long id) {
        maintenanceRecordService.deleteEquipmentMaintenanceRecord(id);
        return CommonResult.success(null);
    }

    @GetMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER,
            RoleCodeConstants.TEAM_LEADER, RoleCodeConstants.OPERATOR})
    public CommonResult<EquipmentMaintenanceRecordRespVO> getEquipmentMaintenanceRecord(
            @PathVariable("id") @Positive Long id) {
        return CommonResult.success(maintenanceRecordService.getEquipmentMaintenanceRecord(id));
    }

    @GetMapping("/page")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER,
            RoleCodeConstants.TEAM_LEADER, RoleCodeConstants.OPERATOR})
    public CommonResult<PageResult<EquipmentMaintenanceRecordRespVO>> getEquipmentMaintenanceRecordPage(
            @Valid EquipmentMaintenanceRecordPageReqVO reqVO) {
        return CommonResult.success(maintenanceRecordService.getEquipmentMaintenanceRecordPage(reqVO));
    }
}
