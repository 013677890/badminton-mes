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

/**
 * 设备保养任务记录 HTTP 接口。
 *
 * <p>保养记录既是计划的执行任务，也是设备保养过程的审计凭据。控制器只处理参数校验、角色授权、
 * 服务调用和统一响应包装；状态流转、执行时间、设备状态联动以及终态记录不可变等规则由
 * {@link EquipmentMaintenanceRecordService} 在事务中保证。
 *
 * <p>普通操作角色可以创建、执行和查询任务，但逻辑删除属于管理动作，仅管理员和车间主任可调用。
 */
@RestController
@RequestMapping("/api/equipment/maintenance-records")
public class EquipmentMaintenanceRecordController {

    /** 保养记录应用服务，负责状态机及设备、计划之间的一致性。 */
    private final EquipmentMaintenanceRecordService maintenanceRecordService;

    /**
     * 构造保养记录控制器。
     *
     * @param maintenanceRecordService 保养记录应用服务
     */
    public EquipmentMaintenanceRecordController(EquipmentMaintenanceRecordService maintenanceRecordService) {
        this.maintenanceRecordService = maintenanceRecordService;
    }

    /**
     * 创建待处理的保养任务，客户端传入的执行态字段不会绕过 Service 的初始化规则。
     *
     * @param reqVO 保养任务创建数据
     * @return 包含新记录主键的统一成功响应
     */
    @PostMapping
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER,
            RoleCodeConstants.TEAM_LEADER, RoleCodeConstants.OPERATOR})
    public CommonResult<Long> createEquipmentMaintenanceRecord(
            @Valid @RequestBody EquipmentMaintenanceRecordSaveReqVO reqVO) {
        return CommonResult.success(maintenanceRecordService.createEquipmentMaintenanceRecord(reqVO));
    }

    /**
     * 修改保养任务并驱动合法的状态流转。
     *
     * @param id 保养记录主键，必须为正数
     * @param reqVO 本次更新数据
     * @return 不携带业务数据的统一成功响应
     */
    @PutMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER,
            RoleCodeConstants.TEAM_LEADER, RoleCodeConstants.OPERATOR})
    public CommonResult<Void> updateEquipmentMaintenanceRecord(
            @PathVariable("id") @Positive Long id,
            @Valid @RequestBody EquipmentMaintenanceRecordSaveReqVO reqVO) {
        maintenanceRecordService.updateEquipmentMaintenanceRecord(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 逻辑删除尚未开始的保养任务；已开始或进入终态的记录由 Service 拒绝删除。
     *
     * @param id 保养记录主键，必须为正数
     * @return 不携带业务数据的统一成功响应
     */
    @DeleteMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER})
    public CommonResult<Void> deleteEquipmentMaintenanceRecord(@PathVariable("id") @Positive Long id) {
        maintenanceRecordService.deleteEquipmentMaintenanceRecord(id);
        return CommonResult.success(null);
    }

    /**
     * 查询单条保养记录详情。
     *
     * @param id 保养记录主键，必须为正数
     * @return 保养记录详情统一响应
     */
    @GetMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER,
            RoleCodeConstants.TEAM_LEADER, RoleCodeConstants.OPERATOR})
    public CommonResult<EquipmentMaintenanceRecordRespVO> getEquipmentMaintenanceRecord(
            @PathVariable("id") @Positive Long id) {
        return CommonResult.success(maintenanceRecordService.getEquipmentMaintenanceRecord(id));
    }

    /**
     * 按计划、设备、状态、结果和计划执行时间区间分页查询保养记录。
     *
     * @param reqVO GET 查询参数绑定形成的分页条件
     * @return 保养记录分页结果
     */
    @GetMapping("/page")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER,
            RoleCodeConstants.TEAM_LEADER, RoleCodeConstants.OPERATOR})
    public CommonResult<PageResult<EquipmentMaintenanceRecordRespVO>> getEquipmentMaintenanceRecordPage(
            @Valid EquipmentMaintenanceRecordPageReqVO reqVO) {
        return CommonResult.success(maintenanceRecordService.getEquipmentMaintenanceRecordPage(reqVO));
    }
}
