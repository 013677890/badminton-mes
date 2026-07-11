package com.badminton.mes.module.equipment.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.equipment.controller.vo.EquipmentFaultPrinciplePageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentFaultPrincipleRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentFaultPrincipleSaveReqVO;

/**
 * 设备故障原理 Service 接口。
 *
 * @author 角色C
 * @date 2026/07/10
 */
public interface EquipmentFaultPrincipleService {

    /**
     * 创建设备故障原理。
     *
     * @param reqVO 创建请求
     * @return 新故障原理主键 id
     */
    Long createEquipmentFaultPrinciple(EquipmentFaultPrincipleSaveReqVO reqVO);

    /**
     * 修改设备故障原理。
     *
     * @param id    故障原理主键
     * @param reqVO 修改请求
     */
    void updateEquipmentFaultPrinciple(Long id, EquipmentFaultPrincipleSaveReqVO reqVO);

    /**
     * 删除设备故障原理(逻辑删除)。
     *
     * @param id 故障原理主键
     */
    void deleteEquipmentFaultPrinciple(Long id);

    /**
     * 查询设备故障原理详情。
     *
     * @param id 故障原理主键
     * @return 设备故障原理详情
     */
    EquipmentFaultPrincipleRespVO getEquipmentFaultPrinciple(Long id);

    /**
     * 分页查询设备故障原理列表。
     *
     * @param reqVO 分页筛选条件
     * @return 分页结果
     */
    PageResult<EquipmentFaultPrincipleRespVO> getEquipmentFaultPrinciplePage(EquipmentFaultPrinciplePageReqVO reqVO);
}
