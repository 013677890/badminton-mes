package com.badminton.mes.module.equipment.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.equipment.controller.vo.EquipmentFaultPrinciplePageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentFaultPrincipleRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentFaultPrincipleSaveReqVO;

/**
 * 设备故障原理 Service 接口。
 *
 * <p>维护可复用的故障知识主数据。实现层负责故障编码唯一性、适用设备类别有效性，以及报修单
 * 对故障原理的引用保护；业务规则不满足时统一抛出 {@code ServiceException}。
 *
 * @author 角色C
 * @date 2026/07/10
 */
public interface EquipmentFaultPrincipleService {

    /**
     * 创建设备故障原理。
     *
     * <p><b>前置条件：</b>故障编码唯一，适用类别为空或存在且未删除。
     * <b>副作用：</b>写入故障原理，并补齐启用状态、低故障等级和默认排序号。
     *
     * @param reqVO 创建请求
     * @return 新故障原理主键 id
     * @throws com.badminton.mes.common.exception.ServiceException 编码重复或适用类别不存在时抛出
     */
    Long createEquipmentFaultPrinciple(EquipmentFaultPrincipleSaveReqVO reqVO);

    /**
     * 修改设备故障原理。
     *
     * <p><b>前置条件：</b>故障原理存在且未删除，修改后的编码保持唯一，适用类别有效。
     * <b>副作用：</b>更新故障知识及可选状态、等级和排序字段；方法无返回值。
     *
     * @param id    故障原理主键
     * @param reqVO 修改请求
     * @throws com.badminton.mes.common.exception.ServiceException 故障原理不存在、编码重复或类别不存在时抛出
     */
    void updateEquipmentFaultPrinciple(Long id, EquipmentFaultPrincipleSaveReqVO reqVO);

    /**
     * 删除设备故障原理(逻辑删除)。
     *
     * <p><b>前置条件：</b>故障原理存在且未删除，并且没有未删除报修单引用。
     * <b>副作用：</b>在字段长度限制内改写故障编码以释放唯一键，并设置逻辑删除标记；方法无返回值。
     *
     * @param id 故障原理主键
     * @throws com.badminton.mes.common.exception.ServiceException 故障原理不存在或仍被报修单引用时抛出
     */
    void deleteEquipmentFaultPrinciple(Long id);

    /**
     * 查询设备故障原理详情。
     *
     * <p><b>前置条件：</b>主键对应故障原理存在且未逻辑删除。
     * <b>副作用：</b>只读查询，不修改故障知识数据。
     *
     * @param id 故障原理主键
     * @return 设备故障原理详情
     * @throws com.badminton.mes.common.exception.ServiceException 故障原理不存在时抛出
     */
    EquipmentFaultPrincipleRespVO getEquipmentFaultPrinciple(Long id);

    /**
     * 分页查询设备故障原理列表。
     *
     * <p><b>前置条件：</b>分页及筛选参数已通过入参校验。
     * <b>副作用：</b>只读查询；无数据返回空页，页码越界时归一化到最后一页。
     * <b>业务异常：</b>无。
     *
     * @param reqVO 分页筛选条件
     * @return 按排序号升序、主键倒序排列的分页结果，列表不会为 {@code null}
     */
    PageResult<EquipmentFaultPrincipleRespVO> getEquipmentFaultPrinciplePage(EquipmentFaultPrinciplePageReqVO reqVO);
}
