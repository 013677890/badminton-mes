package com.badminton.mes.module.equipment.convert;

import java.util.List;

import com.badminton.mes.module.equipment.controller.vo.EquipmentFaultPrincipleRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentFaultPrincipleSaveReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentFaultPrincipleEntity;

/**
 * 设备故障原理 VO 与实体的转换器。
 *
 * <p>以显式字段映射隔离故障知识的接口模型与持久化模型。转换层只传递故障描述、建议方案、等级
 * 和可选类别主键，不判断类别是否有效，也不处理报修引用保护、编码唯一性及逻辑删除改名。
 *
 * @author 角色C
 * @date 2026/07/10
 */
public final class EquipmentFaultPrincipleConvert {

    /**
     * 保存请求 VO 转实体。
     *
     * <p>复制客户端可维护的故障知识字段，保留空等级、空排序或空状态供 Service 应用创建默认值；
     * 主键、审计字段和逻辑删除标记不会由请求写入。
     *
     * @param reqVO 保存请求 VO
     * @return 设备故障原理实体
     */
    public static EquipmentFaultPrincipleEntity toEntity(EquipmentFaultPrincipleSaveReqVO reqVO) {
        EquipmentFaultPrincipleEntity faultPrinciple = new EquipmentFaultPrincipleEntity();
        faultPrinciple.setFaultCode(reqVO.getFaultCode());
        faultPrinciple.setFaultName(reqVO.getFaultName());
        faultPrinciple.setCategoryId(reqVO.getCategoryId());
        faultPrinciple.setFaultLevel(reqVO.getFaultLevel());
        faultPrinciple.setFaultDescription(reqVO.getFaultDescription());
        faultPrinciple.setSuggestedSolution(reqVO.getSuggestedSolution());
        faultPrinciple.setSortOrder(reqVO.getSortOrder());
        faultPrinciple.setRemark(reqVO.getRemark());
        faultPrinciple.setStatus(reqVO.getStatus());
        return faultPrinciple;
    }

    /**
     * 实体转响应 VO。
     *
     * <p>输出故障知识及其适用类别主键的当前快照，同时带出创建、更新时间；不展开类别对象，避免
     * 批量转换触发额外查询，也不暴露逻辑删除控制字段。
     *
     * @param faultPrinciple 设备故障原理实体
     * @return 响应 VO
     */
    public static EquipmentFaultPrincipleRespVO toRespVO(EquipmentFaultPrincipleEntity faultPrinciple) {
        EquipmentFaultPrincipleRespVO respVO = new EquipmentFaultPrincipleRespVO();
        respVO.setId(faultPrinciple.getId());
        respVO.setFaultCode(faultPrinciple.getFaultCode());
        respVO.setFaultName(faultPrinciple.getFaultName());
        respVO.setCategoryId(faultPrinciple.getCategoryId());
        respVO.setFaultLevel(faultPrinciple.getFaultLevel());
        respVO.setFaultDescription(faultPrinciple.getFaultDescription());
        respVO.setSuggestedSolution(faultPrinciple.getSuggestedSolution());
        respVO.setSortOrder(faultPrinciple.getSortOrder());
        respVO.setRemark(faultPrinciple.getRemark());
        respVO.setStatus(faultPrinciple.getStatus());
        respVO.setCreateTime(faultPrinciple.getCreateTime());
        respVO.setUpdateTime(faultPrinciple.getUpdateTime());
        return respVO;
    }

    /**
     * 实体列表转响应 VO 列表。
     *
     * @param list 设备故障原理实体列表
     * @return 响应 VO 列表；入参为空集合时返回空集合
     */
    public static List<EquipmentFaultPrincipleRespVO> toRespVOList(List<EquipmentFaultPrincipleEntity> list) {
        return list.stream().map(EquipmentFaultPrincipleConvert::toRespVO).toList();
    }

    /** 工具类仅提供无状态字段转换，禁止实例化。 */
    private EquipmentFaultPrincipleConvert() {
    }
}
