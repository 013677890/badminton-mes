package com.badminton.mes.module.equipment.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.equipment.controller.vo.EquipmentLedgerPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentLedgerRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentLedgerSaveReqVO;

/**
 * 设备台账 Service 接口，承载设备台账全部业务规则。
 *
 * <p>台账是保养、报修和工艺能力引用的设备主数据边界。实现层负责校验类别与制造商、约束设备
 * 运行状态、保护跨模块引用，并协调逻辑删除和详情缓存一致性。
 *
 * @author 角色C
 * @date 2026/07/09
 */
public interface EquipmentLedgerService {

    /**
     * 创建设备台账。
     *
     * <p><b>前置条件：</b>设备编码唯一，类别存在，制造商为空或存在且均未逻辑删除。
     * <b>副作用：</b>写入设备主数据；未指定时采用启用、空闲默认状态。
     *
     * @param reqVO 创建请求
     * @return 新设备主键 id
     * @throws com.badminton.mes.common.exception.ServiceException 编码重复，或类别、制造商不可用时抛出
     */
    Long createEquipmentLedger(EquipmentLedgerSaveReqVO reqVO);

    /**
     * 修改设备台账。
     *
     * <p><b>前置条件：</b>设备存在且未删除，编码及关联主数据有效；存在执行中保养任务时设备必须
     * 保持 {@code MAINTAINING}，没有执行中任务时不得伪造该状态。
     * <b>副作用：</b>更新台账业务字段，并在事务提交后失效详情缓存；方法无返回值。
     *
     * @param id    设备主键
     * @param reqVO 修改请求
     * @throws com.badminton.mes.common.exception.ServiceException 设备或关联主数据不存在、编码重复，
     *         或请求状态与保养执行事实冲突时抛出
     */
    void updateEquipmentLedger(Long id, EquipmentLedgerSaveReqVO reqVO);

    /**
     * 删除设备台账(逻辑删除)。
     *
     * <p><b>前置条件：</b>设备存在且未删除、当前不在运行中，并且不存在处理中报修任务、保养计划
     * 或保养记录引用。<b>副作用：</b>以保留编码释放原唯一键、设置逻辑删除标记，并在事务提交后
     * 失效详情缓存；方法无返回值。
     *
     * @param id 设备主键
     * @throws com.badminton.mes.common.exception.ServiceException 设备不存在、状态不允许删除、仍被业务引用，
     *         或删除态编码冲突时抛出
     */
    void deleteEquipmentLedger(Long id);

    /**
     * 查询设备台账详情。
     *
     * <p><b>前置条件：</b>主键对应设备存在且未逻辑删除。
     * <b>副作用：</b>优先读取详情缓存；缓存未命中时读取数据库并回填缓存，不修改业务数据。
     *
     * @param id 设备主键
     * @return 设备台账详情
     * @throws com.badminton.mes.common.exception.ServiceException 设备不存在时抛出
     */
    EquipmentLedgerRespVO getEquipmentLedger(Long id);

    /**
     * 分页查询设备台账列表。
     *
     * <p><b>前置条件：</b>分页及筛选参数已通过入参校验。
     * <b>副作用：</b>只读查询；无数据返回空页，页码越界时归一化到最后一页。
     * <b>业务异常：</b>无。
     *
     * @param reqVO 分页筛选条件
     * @return 按主键倒序排列的分页结果，列表不会为 {@code null}
     */
    PageResult<EquipmentLedgerRespVO> getEquipmentLedgerPage(EquipmentLedgerPageReqVO reqVO);
}
