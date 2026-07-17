package com.badminton.mes.module.equipment.convert;

import java.util.List;

import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenanceRecordRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenanceRecordSaveReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentMaintenanceRecordEntity;

/**
 * 设备保养记录对象转换器。
 *
 * <p>转换器忠实复制客户端可编辑字段，不决定任务初始状态，也不复制由计划派生的设备主键。
 * Service 会在转换后覆盖创建阶段不可信的执行时间、结果和状态，确保状态机入口唯一。响应转换
 * 输出任务执行事实快照，但刻意隐藏设备原状态快照和逻辑删除标记；前者仅用于状态机结束时恢复
 * 设备，后者属于持久层控制信息，均不应成为客户端可回写字段。
 */
public final class EquipmentMaintenanceRecordConvert {

    /**
     * 将保养记录请求转换为新实体。
     *
     * <p>执行时间、结果和状态在此照录是为了复用统一字段映射；创建场景会由 Service 清空并固定
     * 初始态，更新场景则由 Service 按状态机逐字段处理，不能直接用该新实体覆盖持久化记录。
     *
     * @param reqVO 保养记录请求数据
     * @return 等待 Service 补齐设备、审计和状态字段的实体
     */
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

    /**
     * 将保养记录实体转换为对外响应对象。
     *
     * <p>响应保留计划、设备、执行人、时间和结果等已落库事实，供详情和列表展示同一时点的任务
     * 快照；不暴露恢复设备状态所需的内部字段，避免客户端篡改跨聚合联动依据。
     *
     * @param record 保养记录实体
     * @return 隐藏设备原状态快照和逻辑删除标记的响应对象
     */
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

    /**
     * 批量转换保养记录并保持持久层排序结果。
     *
     * @param records 保养记录实体列表
     * @return 响应对象列表
     */
    public static List<EquipmentMaintenanceRecordRespVO> toRespVOList(List<EquipmentMaintenanceRecordEntity> records) {
        return records.stream().map(EquipmentMaintenanceRecordConvert::toRespVO).toList();
    }

    /** 工具类不允许实例化。 */
    private EquipmentMaintenanceRecordConvert() {
    }
}
