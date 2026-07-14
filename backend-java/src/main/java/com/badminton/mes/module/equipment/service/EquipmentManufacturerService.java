package com.badminton.mes.module.equipment.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.equipment.controller.vo.EquipmentManufacturerPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentManufacturerRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentManufacturerSaveReqVO;

/**
 * 设备制造商 Service 接口，承载设备制造商全部业务规则。
 *
 * <p>业务规则不通过时统一抛 {@code ServiceException}，
 * 错误码见 {@code EquipmentErrorCodeConstants}。
 *
 * @author 角色C
 * @date 2026/07/09
 */
public interface EquipmentManufacturerService {

    /**
     * 创建设备制造商。
     *
     * <p>校验制造商编码唯一性后落库。
     *
     * @param reqVO 创建请求，字段级校验已由 Controller 完成
     * @return 新制造商主键 id
     * @throws com.badminton.mes.common.exception.ServiceException 制造商编码重复时抛出
     */
    Long createEquipmentManufacturer(EquipmentManufacturerSaveReqVO reqVO);

    /**
     * 修改设备制造商。
     *
     * @param id    制造商主键
     * @param reqVO 修改请求
     * @throws com.badminton.mes.common.exception.ServiceException 制造商不存在或编码重复时抛出
     */
    void updateEquipmentManufacturer(Long id, EquipmentManufacturerSaveReqVO reqVO);

    /**
     * 删除设备制造商(逻辑删除)。
     *
     * <p>该制造商下存在设备时不允许删除。
     *
     * @param id 制造商主键
     * @throws com.badminton.mes.common.exception.ServiceException 制造商不存在或存在设备时抛出
     */
    void deleteEquipmentManufacturer(Long id);

    /**
     * 查询设备制造商详情。
     *
     * @param id 制造商主键
     * @return 制造商详情
     * @throws com.badminton.mes.common.exception.ServiceException 制造商不存在时抛出
     */
    EquipmentManufacturerRespVO getEquipmentManufacturer(Long id);

    /**
     * 分页查询设备制造商列表。
     *
     * <p>先 count 后查列表，总数为 0 时直接返回空页；
     * 请求页码超过总页数时按最后一页返回。
     *
     * @param reqVO 分页筛选条件
     * @return 分页结果，无数据时 list 为空集合而非 null
     */
    PageResult<EquipmentManufacturerRespVO> getEquipmentManufacturerPage(EquipmentManufacturerPageReqVO reqVO);
}
