package com.badminton.mes.module.equipment.convert;

import java.util.List;

import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenancePlanRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenancePlanSaveReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentMaintenancePlanEntity;

/** 设备保养计划转换器。 */
public final class EquipmentMaintenancePlanConvert {

    public static EquipmentMaintenancePlanEntity toEntity(EquipmentMaintenancePlanSaveReqVO reqVO) {
        EquipmentMaintenancePlanEntity plan = new EquipmentMaintenancePlanEntity();
        plan.setPlanCode(reqVO.getPlanCode());
        plan.setPlanName(reqVO.getPlanName());
        plan.setEquipmentId(reqVO.getEquipmentId());
        plan.setMaintenanceType(reqVO.getMaintenanceType());
        plan.setCycleDays(reqVO.getCycleDays());
        plan.setMaintenanceContent(reqVO.getMaintenanceContent());
        plan.setResponsibleUserId(reqVO.getResponsibleUserId());
        plan.setNextMaintenanceTime(reqVO.getNextMaintenanceTime());
        plan.setRemark(reqVO.getRemark());
        plan.setStatus(reqVO.getStatus());
        return plan;
    }

    public static EquipmentMaintenancePlanRespVO toRespVO(EquipmentMaintenancePlanEntity plan) {
        EquipmentMaintenancePlanRespVO respVO = new EquipmentMaintenancePlanRespVO();
        respVO.setId(plan.getId());
        respVO.setPlanCode(plan.getPlanCode());
        respVO.setPlanName(plan.getPlanName());
        respVO.setEquipmentId(plan.getEquipmentId());
        respVO.setMaintenanceType(plan.getMaintenanceType());
        respVO.setCycleDays(plan.getCycleDays());
        respVO.setMaintenanceContent(plan.getMaintenanceContent());
        respVO.setResponsibleUserId(plan.getResponsibleUserId());
        respVO.setLastMaintenanceTime(plan.getLastMaintenanceTime());
        respVO.setNextMaintenanceTime(plan.getNextMaintenanceTime());
        respVO.setRemark(plan.getRemark());
        respVO.setStatus(plan.getStatus());
        respVO.setCreateTime(plan.getCreateTime());
        respVO.setUpdateTime(plan.getUpdateTime());
        return respVO;
    }

    public static List<EquipmentMaintenancePlanRespVO> toRespVOList(List<EquipmentMaintenancePlanEntity> plans) {
        return plans.stream().map(EquipmentMaintenancePlanConvert::toRespVO).toList();
    }

    private EquipmentMaintenancePlanConvert() {
    }
}
