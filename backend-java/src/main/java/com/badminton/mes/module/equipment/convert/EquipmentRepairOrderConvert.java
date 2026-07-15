package com.badminton.mes.module.equipment.convert;

import java.util.List;

import com.badminton.mes.module.equipment.controller.vo.EquipmentRepairOrderRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentRepairOrderSaveReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentRepairOrderEntity;

/**
 * 设备报修任务 VO 与实体的转换器。
 *
 * <p>显式映射报修任务的设备、故障原理、人员、时间、结果和状态字段，形成清晰的任务快照边界。
 * 转换器不推进状态机、不补默认时间，也不校验故障原理与设备类别是否匹配；这些需要锁和事务的
 * 规则统一留在 Service 中执行。
 *
 * @author 角色C
 * @date 2026/07/10
 */
public final class EquipmentRepairOrderConvert {

    /**
     * 保存请求 VO 转实体。
     *
     * <p>请求中的状态和时间在此原样搬运，便于创建流程统一接收字段；Service 随后负责生成缺省
     * 单号和上报信息，并按状态补齐关键时间。该新实体不用于绕过状态校验覆盖既有任务。
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
     * <p>输出已持久化的任务生命周期快照，包括关联主键、人员、关键时间和维修结果；不加载设备
     * 或故障原理详情，也不暴露逻辑删除与审计操作者等内部字段。
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

    /** 工具类仅提供无状态字段转换，禁止实例化。 */
    private EquipmentRepairOrderConvert() {
    }
}
