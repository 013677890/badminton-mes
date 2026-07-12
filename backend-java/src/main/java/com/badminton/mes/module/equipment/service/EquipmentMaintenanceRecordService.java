package com.badminton.mes.module.equipment.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenanceRecordPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenanceRecordRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenanceRecordSaveReqVO;

/** 设备保养记录 Service 接口。 */
public interface EquipmentMaintenanceRecordService {

    Long createEquipmentMaintenanceRecord(EquipmentMaintenanceRecordSaveReqVO reqVO);

    void updateEquipmentMaintenanceRecord(Long id, EquipmentMaintenanceRecordSaveReqVO reqVO);

    void deleteEquipmentMaintenanceRecord(Long id);

    EquipmentMaintenanceRecordRespVO getEquipmentMaintenanceRecord(Long id);

    PageResult<EquipmentMaintenanceRecordRespVO> getEquipmentMaintenanceRecordPage(
            EquipmentMaintenanceRecordPageReqVO reqVO);
}
