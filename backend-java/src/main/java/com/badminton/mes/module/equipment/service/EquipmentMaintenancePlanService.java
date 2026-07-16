package com.badminton.mes.module.equipment.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenancePlanPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenancePlanRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenancePlanSaveReqVO;

/**
 * 设备保养计划应用服务接口。
 *
 * <p>定义计划主数据的完整生命周期。实现层需要在同一事务内校验计划编码唯一性、设备与负责人
 * 可用性，并保证计划周期、最近完成时间和下次保养时间之间的一致性。
 */
public interface EquipmentMaintenancePlanService {

    /**
     * 创建保养计划并补齐默认类型、启用状态和创建人。
     *
     * <p><b>前置条件：</b>计划编码未被未删除数据占用，设备存在且未报废，负责人非空时必须为
     * 启用用户。<b>副作用：</b>写入计划主数据；空类型和空状态分别采用例行保养、启用默认值。
     *
     * @param reqVO 计划创建数据
     * @return 新计划主键
     * @throws com.badminton.mes.common.exception.ServiceException 编码重复，或设备、负责人不可用时抛出
     */
    Long createEquipmentMaintenancePlan(EquipmentMaintenancePlanSaveReqVO reqVO);

    /**
     * 修改保养计划；已有保养记录时不得变更所绑定的设备。
     *
     * <p><b>前置条件：</b>计划存在且未删除，编码保持唯一，目标设备可用且负责人有效；若计划已有
     * 记录则设备绑定必须保持不变。<b>副作用：</b>更新计划；存在已完成记录时以最近完成时间为基准
     * 重新计算最近及下次保养时间。方法无返回值。
     *
     * @param id 计划主键
     * @param reqVO 修改后的计划数据
     * @throws com.badminton.mes.common.exception.ServiceException 计划不存在、编码重复、关联主数据不可用，
     *         或已有记录时试图换绑设备时抛出
     */
    void updateEquipmentMaintenancePlan(Long id, EquipmentMaintenancePlanSaveReqVO reqVO);

    /**
     * 逻辑删除无保养记录引用的计划，并释放原计划编码。
     *
     * <p><b>前置条件：</b>计划存在且未删除，并且没有未删除保养记录引用。
     * <b>副作用：</b>以包含主键的保留编码替换原编码后设置逻辑删除标记；方法无返回值。
     *
     * @param id 计划主键
     * @throws com.badminton.mes.common.exception.ServiceException 计划不存在、仍被记录引用，
     *         或删除态编码冲突时抛出
     */
    void deleteEquipmentMaintenancePlan(Long id);

    /**
     * 查询未逻辑删除的计划详情。
     *
     * <p><b>前置条件：</b>主键对应计划必须存在且未逻辑删除。
     * <b>副作用：</b>只读查询，不推进任何计划周期时间。
     *
     * @param id 计划主键
     * @return 计划详情
     * @throws com.badminton.mes.common.exception.ServiceException 计划不存在时抛出
     */
    EquipmentMaintenancePlanRespVO getEquipmentMaintenancePlan(Long id);

    /**
     * 按组合条件分页查询计划，越界页码由实现层归一化到最后一页。
     *
     * <p><b>前置条件：</b>分页参数及筛选字段已通过入参校验。
     * <b>副作用：</b>只读查询，不修改计划；无匹配数据时直接返回空页。
     * <b>业务异常：</b>无。
     *
     * @param reqVO 分页与筛选条件
     * @return 按下次保养时间升序、主键倒序排列的计划分页结果，列表不会为 {@code null}
     */
    PageResult<EquipmentMaintenancePlanRespVO> getEquipmentMaintenancePlanPage(EquipmentMaintenancePlanPageReqVO reqVO);
}
