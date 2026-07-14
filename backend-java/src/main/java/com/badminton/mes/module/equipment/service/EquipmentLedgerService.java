package com.badminton.mes.module.equipment.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.equipment.controller.vo.EquipmentLedgerPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentLedgerRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentLedgerSaveReqVO;

/**
 * 设备台账 Service 接口，承载设备台账全部业务规则。
 *
 * @author 角色C
 * @date 2026/07/09
 */
public interface EquipmentLedgerService {

    /**
     * 创建设备台账。
     *
     * @param reqVO 创建请求
     * @return 新设备主键 id
     */
    Long createEquipmentLedger(EquipmentLedgerSaveReqVO reqVO);

    /**
     * 修改设备台账。
     *
     * @param id    设备主键
     * @param reqVO 修改请求
     */
    void updateEquipmentLedger(Long id, EquipmentLedgerSaveReqVO reqVO);

    /**
     * 删除设备台账(逻辑删除)。
     *
     * @param id 设备主键
     */
    void deleteEquipmentLedger(Long id);

    /**
     * 查询设备台账详情。
     *
     * @param id 设备主键
     * @return 设备台账详情
     */
    EquipmentLedgerRespVO getEquipmentLedger(Long id);

    /**
     * 分页查询设备台账列表。
     *
     * @param reqVO 分页筛选条件
     * @return 分页结果
     */
    PageResult<EquipmentLedgerRespVO> getEquipmentLedgerPage(EquipmentLedgerPageReqVO reqVO);
}
