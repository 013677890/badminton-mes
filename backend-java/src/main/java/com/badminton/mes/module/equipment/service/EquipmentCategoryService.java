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
     * <p><b>前置条件：</b>字段级约束已经由 Controller 校验，类别编码唯一，父级类别为空或存在且
     * 未删除。<b>副作用：</b>写入类别主数据，并在状态为空时按启用状态落库。
     *
     * @param reqVO 创建请求，字段级校验已由 Controller 完成
     * @return 新类别主键 id
     * @throws com.badminton.mes.common.exception.ServiceException 类别编码重复或父级类别不存在时抛出
     */
    Long createEquipmentCategory(EquipmentCategorySaveReqVO reqVO);

    /**
     * 修改设备类别。
     *
     * <p><b>前置条件：</b>类别存在且未删除，编码唯一，新父级有效且不会形成自引用或祖先环；
     * 从启用切换为停用时不得仍被启用工序或生效工艺路线引用。
     * <b>副作用：</b>更新类别树节点，并在事务提交后失效详情缓存；方法无返回值。
     *
     * @param id    类别主键
     * @param reqVO 修改请求
     * @throws com.badminton.mes.common.exception.ServiceException 类别不存在、编码重复、父级无效、形成循环，
     *         或停用时仍有生效工艺引用时抛出
     */
    void updateEquipmentCategory(Long id, EquipmentCategorySaveReqVO reqVO);

    /**
     * 删除设备类别(逻辑删除)。
     *
     * <p><b>前置条件：</b>类别存在且未删除，并且没有子类别、设备、故障原理、启用工序或生效路线引用。
     * <b>副作用：</b>改写编码以释放唯一键、设置逻辑删除标记，并在事务提交后失效详情缓存；
     * 方法无返回值。
     *
     * @param id 类别主键
     * @throws com.badminton.mes.common.exception.ServiceException 类别不存在，或仍被任一受保护资源引用时抛出
     */
    void deleteEquipmentCategory(Long id);

    /**
     * 查询设备类别详情。
     *
     * <p><b>前置条件：</b>主键对应类别存在且未逻辑删除。
     * <b>副作用：</b>优先读取详情缓存；缓存未命中时读取数据库并回填缓存，不修改业务数据。
     *
     * @param id 类别主键
     * @return 类别详情
     * @throws com.badminton.mes.common.exception.ServiceException 类别不存在时抛出
     */
    EquipmentCategoryRespVO getEquipmentCategory(Long id);

    /**
     * 分页查询设备类别列表。
     *
     * <p><b>前置条件：</b>分页与筛选参数已通过入参校验。
     * <b>副作用：</b>只读查询。先 count 后查列表，总数为 0 时直接返回空页；
     * 请求页码超过总页数时按最后一页返回。
     * <b>业务异常：</b>无。
     *
     * @param reqVO 分页筛选条件
     * @return 分页结果，无数据时 list 为空集合而非 null
     */
    PageResult<EquipmentCategoryRespVO> getEquipmentCategoryPage(EquipmentCategoryPageReqVO reqVO);
}
