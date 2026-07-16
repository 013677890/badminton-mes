package com.badminton.mes.module.equipment.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenanceRecordPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenanceRecordRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenanceRecordSaveReqVO;

/**
 * 设备保养任务记录应用服务接口。
 *
 * <p>负责保养任务从待处理、执行中到完成或取消的生命周期。实现层同时维护设备运行状态和保养计划
 * 时间，因而所有写操作都必须在事务中完成跨聚合校验与联动更新。
 */
public interface EquipmentMaintenanceRecordService {

    /**
     * 创建保养任务；新任务只能从待处理状态开始。
     *
     * <p><b>前置条件：</b>计划必须存在且启用，计划关联设备不得报废；执行人非空时必须为启用用户，
     * 请求状态只能为空或 {@code PENDING}。<b>副作用：</b>写入一条待处理记录，缺省单号由服务生成，
     * 创建阶段不会改变设备状态和计划周期时间。
     *
     * @param reqVO 任务创建数据
     * @return 新保养记录主键
     * @throws com.badminton.mes.common.exception.ServiceException 计划、设备或执行人不可用，初始状态非法，
     *         或保养单号重复时抛出
     */
    Long createEquipmentMaintenanceRecord(EquipmentMaintenanceRecordSaveReqVO reqVO);

    /**
     * 更新任务内容并执行合法状态迁移，必要时同步设备和计划。
     *
     * <p><b>前置条件：</b>记录必须存在且未进入完成/取消终态，计划绑定不可变，目标状态须符合
     * {@code PENDING -> IN_PROGRESS/CANCELLED -> COMPLETED/CANCELLED} 的单向流转规则。
     * <b>副作用：</b>更新记录；进入或离开执行中状态时同步设备状态并在事务提交后失效设备缓存，
     * 首次完成时推进计划的最近及下次保养时间。方法无返回值。
     *
     * @param id 保养记录主键
     * @param reqVO 本次更新数据
     * @throws com.badminton.mes.common.exception.ServiceException 记录或关联主数据不存在、状态流转非法、
     *         时间/结果字段与状态不一致、设备状态冲突或单号重复时抛出
     */
    void updateEquipmentMaintenanceRecord(Long id, EquipmentMaintenanceRecordSaveReqVO reqVO);

    /**
     * 逻辑删除尚未开始的任务；终态和执行中记录必须保留审计轨迹。
     *
     * <p><b>前置条件：</b>记录存在、未删除且仍为 {@code PENDING}。
     * <b>副作用：</b>将单号改写为删除态保留值后设置逻辑删除标记，以释放原业务单号；方法无返回值。
     *
     * @param id 保养记录主键
     * @throws com.badminton.mes.common.exception.ServiceException 记录不存在、记录已开始或已进入终态，
     *         或删除态单号发生冲突时抛出
     */
    void deleteEquipmentMaintenanceRecord(Long id);

    /**
     * 查询未逻辑删除的保养记录详情。
     *
     * <p><b>前置条件：</b>主键对应记录必须存在且未逻辑删除。
     * <b>副作用：</b>只读查询，不修改记录、设备或计划。
     *
     * @param id 保养记录主键
     * @return 保养记录详情
     * @throws com.badminton.mes.common.exception.ServiceException 记录不存在时抛出
     */
    EquipmentMaintenanceRecordRespVO getEquipmentMaintenanceRecord(Long id);

    /**
     * 按计划、设备、状态、结果和计划时间分页查询保养记录。
     *
     * <p><b>前置条件：</b>分页参数及筛选字段已通过入参校验。
     * <b>副作用：</b>只读查询；无数据时返回空页，越界页码归一化到最后一页。
     * <b>业务异常：</b>无。
     *
     * @param reqVO 分页与筛选条件
     * @return 保持计划时间倒序、主键倒序的保养记录分页结果，列表不会为 {@code null}
     */
    PageResult<EquipmentMaintenanceRecordRespVO> getEquipmentMaintenanceRecordPage(
            EquipmentMaintenanceRecordPageReqVO reqVO);
}
