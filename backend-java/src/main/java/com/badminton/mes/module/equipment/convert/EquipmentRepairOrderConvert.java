package com.badminton.mes.module.equipment.convert;

import java.util.List;

import com.badminton.mes.module.equipment.controller.vo.EquipmentRepairOrderRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentRepairOrderSaveReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentRepairOrderEntity;

/**
 * 设备报修任务 VO 与实体的转换器。
 *
 * @author 角色C
 * @date 2026/07/10
 */
public final class EquipmentRepairOrderConvert {

    /**
     * 保存请求 VO 转实体。
     *
     * @param reqVO 保存请求 VO
     * @return 设备报修任务实体
     */
    public static EquipmentRepairOrderEntity toEntity(EquipmentRepairOrderSaveReqVO reqVO) {
        EquipmentRepairOrderEntity repairOrder = new EquipmentRepairOrderEntity();
        repairOrder.setRepairNo(reqVO.getRepairNo());
        repairOrder.setEquipmentId(reqVO.getEquipmentId());
        repairOrder.setFaultPrincipleId(reqVO.getFaultPrincipleId());
        repairOrder.setFaultDescription(reqVO.getFaultDescription());
        repairOrder.setReportTime(reqVO.getReportTime());
        repairOrder.setReportUserId(reqVO.getReportUserId());
        repairOrder.setRepairUserId(reqVO.getRepairUserId());
        repairOrder.setRepairStartTime(reqVO.getRepairStartTime());
        repairOrder.setRepairEndTime(reqVO.getRepairEndTime());
        repairOrder.setRepairResult(reqVO.getRepairResult());
        repairOrder.setRepairStatus(reqVO.getRepairStatus());
        repairOrder.setRemark(reqVO.getRemark());
        return repairOrder;
    }

    /**
     * 实体转响应 VO。
     *
     * @param repairOrder 设备报修任务实体
     * @return 响应 VO
     */
    public static EquipmentRepairOrderRespVO toRespVO(EquipmentRepairOrderEntity repairOrder) {
        EquipmentRepairOrderRespVO respVO = new EquipmentRepairOrderRespVO();
        respVO.setId(repairOrder.getId());
        respVO.setRepairNo(repairOrder.getRepairNo());
        respVO.setEquipmentId(repairOrder.getEquipmentId());
        respVO.setFaultPrincipleId(repairOrder.getFaultPrincipleId());
        respVO.setFaultDescription(repairOrder.getFaultDescription());
        respVO.setReportTime(repairOrder.getReportTime());
        respVO.setReportUserId(repairOrder.getReportUserId());
        respVO.setRepairUserId(repairOrder.getRepairUserId());
        respVO.setRepairStartTime(repairOrder.getRepairStartTime());
        respVO.setRepairEndTime(repairOrder.getRepairEndTime());
        respVO.setRepairResult(repairOrder.getRepairResult());
        respVO.setRepairStatus(repairOrder.getRepairStatus());
        respVO.setRemark(repairOrder.getRemark());
        respVO.setCreateTime(repairOrder.getCreateTime());
        respVO.setUpdateTime(repairOrder.getUpdateTime());
        return respVO;
    }

    /**
     * 实体列表转响应 VO 列表。
     *
     * @param list 设备报修任务实体列表
     * @return 响应 VO 列表；入参为空集合时返回空集合
     */
    public static List<EquipmentRepairOrderRespVO> toRespVOList(List<EquipmentRepairOrderEntity> list) {
        return list.stream().map(EquipmentRepairOrderConvert::toRespVO).toList();
    }

    private EquipmentRepairOrderConvert() {
    }
}
