package com.badminton.mes.module.equipment.convert;

import java.util.List;

import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenanceRecordRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenanceRecordSaveReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentMaintenanceRecordEntity;

/** 设备保养记录转换器。 */
public final class EquipmentMaintenanceRecordConvert {

    public static EquipmentMaintenanceRecordEntity toEntity(EquipmentMaintenanceRecordSaveReqVO reqVO) {
        EquipmentMaintenanceRecordEntity record = new EquipmentMaintenanceRecordEntity();
        record.setRecordNo(reqVO.getRecordNo());
        record.setPlanId(reqVO.getPlanId());
        record.setScheduledTime(reqVO.getScheduledTime());
        record.setStartTime(reqVO.getStartTime());
        record.setFinishTime(reqVO.getFinishTime());
        record.setExecutorUserId(reqVO.getExecutorUserId());
        record.setMaintenanceContent(reqVO.getMaintenanceContent());
        record.setMaintenanceResult(reqVO.getMaintenanceResult());
        record.setRecordStatus(reqVO.getRecordStatus());
        record.setAbnormalDescription(reqVO.getAbnormalDescription());
        record.setRemark(reqVO.getRemark());
        return record;
    }

    public static EquipmentMaintenanceRecordRespVO toRespVO(EquipmentMaintenanceRecordEntity record) {
        EquipmentMaintenanceRecordRespVO respVO = new EquipmentMaintenanceRecordRespVO();
        respVO.setId(record.getId());
        respVO.setRecordNo(record.getRecordNo());
        respVO.setPlanId(record.getPlanId());
        respVO.setEquipmentId(record.getEquipmentId());
        respVO.setScheduledTime(record.getScheduledTime());
        respVO.setStartTime(record.getStartTime());
        respVO.setFinishTime(record.getFinishTime());
        respVO.setExecutorUserId(record.getExecutorUserId());
        respVO.setMaintenanceContent(record.getMaintenanceContent());
        respVO.setMaintenanceResult(record.getMaintenanceResult());
        respVO.setRecordStatus(record.getRecordStatus());
        respVO.setAbnormalDescription(record.getAbnormalDescription());
        respVO.setRemark(record.getRemark());
        respVO.setCreateTime(record.getCreateTime());
        respVO.setUpdateTime(record.getUpdateTime());
        return respVO;
    }

    public static List<EquipmentMaintenanceRecordRespVO> toRespVOList(List<EquipmentMaintenanceRecordEntity> records) {
        return records.stream().map(EquipmentMaintenanceRecordConvert::toRespVO).toList();
    }

    private EquipmentMaintenanceRecordConvert() {
    }
}
