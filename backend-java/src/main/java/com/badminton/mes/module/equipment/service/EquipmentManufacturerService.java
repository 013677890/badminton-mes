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
     * <p><b>前置条件：</b>字段级约束已经由 Controller 校验，制造商编码未被未删除数据占用。
     * <b>副作用：</b>写入制造商主数据，并在状态为空时按启用状态落库。
     *
     * @param reqVO 创建请求，字段级校验已由 Controller 完成
     * @return 新制造商主键 id
     * @throws com.badminton.mes.common.exception.ServiceException 制造商编码重复时抛出
     */
    Long createEquipmentManufacturer(EquipmentManufacturerSaveReqVO reqVO);

    /**
     * 修改设备制造商。
     *
     * <p><b>前置条件：</b>制造商存在且未逻辑删除，修改后的编码在其他有效数据中唯一。
     * <b>副作用：</b>更新制造商业务字段，并在事务提交后失效对应详情缓存；方法无返回值。
     *
     * @param id    制造商主键
     * @param reqVO 修改请求
     * @throws com.badminton.mes.common.exception.ServiceException 制造商不存在或编码重复时抛出
     */
    void updateEquipmentManufacturer(Long id, EquipmentManufacturerSaveReqVO reqVO);

    /**
     * 删除设备制造商(逻辑删除)。
     *
     * <p><b>前置条件：</b>制造商存在且未删除，并且没有未删除设备引用。
     * <b>副作用：</b>改写编码以释放唯一键、设置逻辑删除标记，并在事务提交后失效详情缓存；
     * 方法无返回值。
     *
     * @param id 制造商主键
     * @throws com.badminton.mes.common.exception.ServiceException 制造商不存在或存在设备时抛出
     */
    void deleteEquipmentManufacturer(Long id);

    /**
     * 查询设备制造商详情。
     *
     * <p><b>前置条件：</b>主键对应制造商存在且未逻辑删除。
     * <b>副作用：</b>优先读取详情缓存；缓存未命中时读取数据库并回填缓存，不修改业务数据。
     *
     * @param id 制造商主键
     * @return 制造商详情
     * @throws com.badminton.mes.common.exception.ServiceException 制造商不存在时抛出
     */
    EquipmentManufacturerRespVO getEquipmentManufacturer(Long id);

    /**
     * 分页查询设备制造商列表。
     *
     * <p><b>前置条件：</b>分页与筛选参数已通过入参校验。
     * <b>副作用：</b>只读查询。先 count 后查列表，总数为 0 时直接返回空页；
     * 请求页码超过总页数时按最后一页返回。
     * <b>业务异常：</b>无。
     *
     * @param reqVO 分页筛选条件
     * @return 分页结果，无数据时 list 为空集合而非 null
     */
    PageResult<EquipmentManufacturerRespVO> getEquipmentManufacturerPage(EquipmentManufacturerPageReqVO reqVO);
}
