package com.badminton.mes.module.equipment.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.equipment.controller.vo.EquipmentCategoryPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentCategoryRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentCategorySaveReqVO;

/**
 * 设备类别 Service 接口，承载设备类别全部业务规则。
 *
 * <p>业务规则不通过时统一抛 {@code ServiceException}，
 * 错误码见 {@code EquipmentErrorCodeConstants}。
 *
 * @author 角色C
 * @date 2026/07/09
 */
public interface EquipmentCategoryService {

    /**
     * 创建设备类别。
     *
     * <p>校验类别编码唯一性、父级类别存在性后落库。
     *
     * @param reqVO 创建请求，字段级校验已由 Controller 完成
     * @return 新类别主键 id
     * @throws com.badminton.mes.common.exception.ServiceException 类别编码重复或父级类别不存在时抛出
     */
    Long createEquipmentCategory(EquipmentCategorySaveReqVO reqVO);

    /**
     * 修改设备类别。
     *
     * @param id    类别主键
     * @param reqVO 修改请求
     * @throws com.badminton.mes.common.exception.ServiceException 类别不存在、编码重复或父级类别不存在时抛出
     */
    void updateEquipmentCategory(Long id, EquipmentCategorySaveReqVO reqVO);

    /**
     * 删除设备类别(逻辑删除)。
     *
     * <p>存在下级分类或该类别下存在设备时不允许删除。
     *
     * @param id 类别主键
     * @throws com.badminton.mes.common.exception.ServiceException 类别不存在或存在下级分类/设备时抛出
     */
    void deleteEquipmentCategory(Long id);

    /**
     * 查询设备类别详情。
     *
     * @param id 类别主键
     * @return 类别详情
     * @throws com.badminton.mes.common.exception.ServiceException 类别不存在时抛出
     */
    EquipmentCategoryRespVO getEquipmentCategory(Long id);

    /**
     * 分页查询设备类别列表。
     *
     * <p>先 count 后查列表，总数为 0 时直接返回空页；
     * 请求页码超过总页数时按最后一页返回。
     *
     * @param reqVO 分页筛选条件
     * @return 分页结果，无数据时 list 为空集合而非 null
     */
    PageResult<EquipmentCategoryRespVO> getEquipmentCategoryPage(EquipmentCategoryPageReqVO reqVO);
}
