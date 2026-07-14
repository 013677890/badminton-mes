package com.badminton.mes.module.equipment.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.equipment.controller.vo.EquipmentRepairOrderPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentRepairOrderRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentRepairOrderSaveReqVO;

/**
 * 设备报修任务 Service 接口。
 *
 * @author 角色C
 * @date 2026/07/10
 */
public interface EquipmentRepairOrderService {

    /**
     * 创建设备报修任务。
     *
     * @param reqVO 创建请求
     * @return 新报修任务主键 id
     */
    Long createEquipmentRepairOrder(EquipmentRepairOrderSaveReqVO reqVO);

    /**
     * 修改设备报修任务。
     *
     * @param id    报修任务主键
     * @param reqVO 修改请求
     */
    void updateEquipmentRepairOrder(Long id, EquipmentRepairOrderSaveReqVO reqVO);

    /**
     * 删除设备报修任务(逻辑删除)。
     *
     * @param id 报修任务主键
     */
    void deleteEquipmentRepairOrder(Long id);

    /**
     * 查询设备报修任务详情。
     *
     * @param id 报修任务主键
     * @return 报修任务详情
     */
    EquipmentRepairOrderRespVO getEquipmentRepairOrder(Long id);

    /**
     * 分页查询设备报修任务列表。
     *
     * @param reqVO 分页筛选条件
     * @return 分页结果
     */
    PageResult<EquipmentRepairOrderRespVO> getEquipmentRepairOrderPage(EquipmentRepairOrderPageReqVO reqVO);
}
