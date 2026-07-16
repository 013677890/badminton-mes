package com.badminton.mes.module.equipment.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenanceRecordPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenanceRecordRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenanceRecordSaveReqVO;

/**
 * 设备保养执行记录 Service 接口。
 *
 * <p>由保养记录 Controller 调用，记录实际执行人、时间、结果和异常；设备台账页面
 * 可据此查询履历，计划服务则提供待执行计划来源。
 *
 * @author MES 开发组
 * @date 2026/07/16
 */
public interface EquipmentMaintenanceRecordService {

    /** 创建一条设备保养执行记录。 */
    Long createEquipmentMaintenanceRecord(EquipmentMaintenanceRecordSaveReqVO reqVO);

    /** 修改保养执行记录。 */
    void updateEquipmentMaintenanceRecord(Long id, EquipmentMaintenanceRecordSaveReqVO reqVO);

    /** 逻辑删除保养执行记录。 */
    void deleteEquipmentMaintenanceRecord(Long id);

    /** 查询单条保养执行记录。 */
    EquipmentMaintenanceRecordRespVO getEquipmentMaintenanceRecord(Long id);

    /** 分页查询设备保养执行记录。 */
    PageResult<EquipmentMaintenanceRecordRespVO> getEquipmentMaintenanceRecordPage(
            EquipmentMaintenanceRecordPageReqVO reqVO);
}
