package com.badminton.mes.module.equipment.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.equipment.controller.vo.EquipmentRepairOrderPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentRepairOrderRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentRepairOrderSaveReqVO;

/**
 * 设备报修任务 Service 接口。
 *
 * <p>定义设备故障从上报、派工、维修到完成或取消的任务生命周期。实现层在事务内校验设备与
 * 故障原理适配关系、限制状态机流转，并用数据库唯一约束兜底报修单号并发冲突。
 *
 * @author 角色C
 * @date 2026/07/10
 */
public interface EquipmentRepairOrderService {

    /**
     * 创建设备报修任务。
     *
     * <p><b>前置条件：</b>设备存在、未删除且未报废；故障原理非空时必须存在，并适用于设备类别。
     * <b>副作用：</b>写入报修任务，补齐单号、上报时间、上报人、初始状态及与状态对应的时间字段。
     *
     * @param reqVO 创建请求
     * @return 新报修任务主键 id
     * @throws com.badminton.mes.common.exception.ServiceException 设备或故障原理不可用、类别不匹配，
     *         或报修单号重复时抛出
     */
    Long createEquipmentRepairOrder(EquipmentRepairOrderSaveReqVO reqVO);

    /**
     * 修改设备报修任务。
     *
     * <p><b>前置条件：</b>任务、设备及可选故障原理有效，目标状态遵循上报、派工、维修中到完成或
     * 取消的单向流转，终态不可再次迁移。<b>副作用：</b>更新任务业务字段，并按首次进入维修中、
     * 完成或取消状态补齐或清理关键时间；方法无返回值。
     *
     * @param id    报修任务主键
     * @param reqVO 修改请求
     * @throws com.badminton.mes.common.exception.ServiceException 任务或关联对象不可用、状态迁移非法、
     *         类别不匹配或单号重复时抛出
     */
    void updateEquipmentRepairOrder(Long id, EquipmentRepairOrderSaveReqVO reqVO);

    /**
     * 删除设备报修任务(逻辑删除)。
     *
     * <p><b>前置条件：</b>任务存在且未删除，当前不得处于 {@code REPAIRING}。
     * <b>副作用：</b>在字段长度限制内改写单号以释放唯一键，并设置逻辑删除标记；方法无返回值。
     *
     * @param id 报修任务主键
     * @throws com.badminton.mes.common.exception.ServiceException 任务不存在或正在维修、不允许删除时抛出
     */
    void deleteEquipmentRepairOrder(Long id);

    /**
     * 查询设备报修任务详情。
     *
     * <p><b>前置条件：</b>主键对应任务存在且未逻辑删除。
     * <b>副作用：</b>只读查询，不推进任务状态或时间。
     *
     * @param id 报修任务主键
     * @return 报修任务详情
     * @throws com.badminton.mes.common.exception.ServiceException 任务不存在时抛出
     */
    EquipmentRepairOrderRespVO getEquipmentRepairOrder(Long id);

    /**
     * 分页查询设备报修任务列表。
     *
     * <p><b>前置条件：</b>分页及筛选参数已通过入参校验。
     * <b>副作用：</b>只读查询；无数据返回空页，页码越界时归一化到最后一页。
     * <b>业务异常：</b>无。
     *
     * @param reqVO 分页筛选条件
     * @return 按上报时间倒序排列的分页结果，列表不会为 {@code null}
     */
    PageResult<EquipmentRepairOrderRespVO> getEquipmentRepairOrderPage(EquipmentRepairOrderPageReqVO reqVO);
}
