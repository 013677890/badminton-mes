package com.badminton.mes.module.equipment.convert;

import java.util.List;

import com.badminton.mes.module.equipment.controller.vo.EquipmentFaultPrincipleRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentFaultPrincipleSaveReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentFaultPrincipleEntity;

/**
 * 设备故障原理 VO 与实体的转换器。
 *
 * @author 角色C
 * @date 2026/07/10
 */
public final class EquipmentFaultPrincipleConvert {

    /**
     * 保存请求 VO 转实体。
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

    private EquipmentFaultPrincipleConvert() {
    }
}
