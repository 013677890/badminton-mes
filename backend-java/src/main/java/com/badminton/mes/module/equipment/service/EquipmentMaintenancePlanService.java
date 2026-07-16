package com.badminton.mes.module.equipment.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenancePlanPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenancePlanRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenancePlanSaveReqVO;

/**
 * 设备保养计划 Service 接口。
 *
 * <p>由保养计划 Controller 调用；计划定义保养周期和目标设备，保养记录 Service
 * 在执行保养时引用计划并回写执行状态。
 *
 * @author MES 开发组
 * @date 2026/07/16
 */
public interface EquipmentMaintenancePlanService {

    /** 创建设备保养计划。 */
    Long createEquipmentMaintenancePlan(EquipmentMaintenancePlanSaveReqVO reqVO);

    /** 修改设备保养计划。 */
    void updateEquipmentMaintenancePlan(Long id, EquipmentMaintenancePlanSaveReqVO reqVO);

    /** 逻辑删除设备保养计划。 */
    void deleteEquipmentMaintenancePlan(Long id);

    /** 查询单条设备保养计划详情。 */
    EquipmentMaintenancePlanRespVO getEquipmentMaintenancePlan(Long id);

    /** 按设备、计划状态和时间条件分页查询保养计划。 */
    PageResult<EquipmentMaintenancePlanRespVO> getEquipmentMaintenancePlanPage(EquipmentMaintenancePlanPageReqVO reqVO);
}
