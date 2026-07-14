package com.badminton.mes.module.equipment.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenancePlanPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenancePlanRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenancePlanSaveReqVO;

/** 设备保养计划 Service 接口。 */
public interface EquipmentMaintenancePlanService {

    Long createEquipmentMaintenancePlan(EquipmentMaintenancePlanSaveReqVO reqVO);

    void updateEquipmentMaintenancePlan(Long id, EquipmentMaintenancePlanSaveReqVO reqVO);

    void deleteEquipmentMaintenancePlan(Long id);

    EquipmentMaintenancePlanRespVO getEquipmentMaintenancePlan(Long id);

    PageResult<EquipmentMaintenancePlanRespVO> getEquipmentMaintenancePlanPage(EquipmentMaintenancePlanPageReqVO reqVO);
}
