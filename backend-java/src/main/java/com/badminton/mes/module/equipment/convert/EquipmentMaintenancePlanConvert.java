package com.badminton.mes.module.equipment.convert;

import java.util.List;

import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenancePlanRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenancePlanSaveReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentMaintenancePlanEntity;

/**
 * 设备保养计划对象转换器。
 *
 * <p>只执行请求 VO、持久化实体和响应 VO 之间的字段复制，不注入默认值、不查询关联对象，也不计算
 * 周期时间；这些带业务语义的操作必须留在 Service 中，避免转换层绕过事务规则。响应转换同时
 * 输出最近和下次保养时间，形成计划周期快照，但这些派生字段只来自实体，不从保存请求反向覆盖。
 */
public final class EquipmentMaintenancePlanConvert {

    /**
     * 将创建/修改请求转换为新的计划实体。
     *
     * <p>仅复制客户端可维护的计划定义和建议下次时间；最近完成时间属于保养记录聚合出的事实，
     * 故意不从请求产生。Service 会根据是否已有完成记录决定采用请求时间还是重新计算周期。
     *
     * @param reqVO 计划请求数据
     * @return 尚未补充主键、审计字段和默认值的实体
     */
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

    /**
     * 将计划实体转换为对外响应对象。
     *
     * <p>同时映射计划定义、启停状态和已计算的周期时间，使调用方得到持久化时点的一致快照；
     * 逻辑删除与审计操作者等内部控制字段不进入响应。
     *
     * @param plan 计划实体
     * @return 不包含逻辑删除等内部控制字段的响应对象
     */
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

    /**
     * 批量转换计划实体，保持输入列表的顺序。
     *
     * @param plans 计划实体列表
     * @return 响应对象列表
     */
    public static List<EquipmentMaintenancePlanRespVO> toRespVOList(List<EquipmentMaintenancePlanEntity> plans) {
        return plans.stream().map(EquipmentMaintenancePlanConvert::toRespVO).toList();
    }

    /** 工具类不允许实例化。 */
    private EquipmentMaintenancePlanConvert() {
    }
}
