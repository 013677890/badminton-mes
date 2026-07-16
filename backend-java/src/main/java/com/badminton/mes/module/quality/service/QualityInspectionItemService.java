package com.badminton.mes.module.quality.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionItemPageReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionItemRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionItemSaveReqVO;

/**
 * 检验项目应用服务契约。
 *
 * <p>检验项目隶属于一个有效分类，并定义采集值类型与判定规则。数值型项目要求单位和有序上下限，
 * 文本型项目不得携带数值上下限；区间判定仅适用于数值型，标准值判定必须提供标准值。</p>
 */
public interface QualityInspectionItemService {

    /**
     * 创建检验项目。
     *
     * <p>前置条件：项目编码唯一、所属分类存在且启用、值类型与判定规则组合合法。</p>
     * <p>副作用：持久化项目，补齐必检与启用默认值，并记录当前操作人。</p>
     *
     * @param request 项目定义及判定规则
     * @return 新项目主键
     * @throws com.badminton.mes.common.exception.ServiceException 编码、分类或规则校验失败时抛出
     */
    Long createItem(QualityInspectionItemSaveReqVO request);

    /**
     * 更新检验项目。
     *
     * <p>前置条件与创建一致，且目标项目必须存在并未删除。</p>
     * <p>副作用：悲观锁定并更新项目；事务提交后清理项目详情缓存，并级联清理引用该项目的方案缓存，
     * 因为方案详情会冗余展示项目编码、名称、值类型和单位。</p>
     *
     * @param id 项目主键
     * @param request 项目定义及判定规则
     * @throws com.badminton.mes.common.exception.ServiceException 项目不存在或业务校验失败时抛出
     */
    void updateItem(Long id, QualityInspectionItemSaveReqVO request);

    /**
     * 逻辑删除检验项目。
     *
     * <p>前置条件：项目存在，且没有任何方案项引用该项目。</p>
     * <p>副作用：替换原项目编码、停用并标记删除，事务提交后清理项目详情缓存。</p>
     *
     * @param id 项目主键
     * @throws com.badminton.mes.common.exception.ServiceException 项目不存在、仍被方案引用或删除占位编码冲突时抛出
     */
    void deleteItem(Long id);

    /**
     * 查询项目详情，并附带所属分类的当前编码和名称。
     *
     * @param id 项目主键
     * @return 项目及分类冗余信息
     * @throws com.badminton.mes.common.exception.ServiceException 项目或所属分类不存在时抛出
     */
    QualityInspectionItemRespVO getItem(Long id);

    /**
     * 分页查询检验项目，并批量装配当前分类主数据以避免逐项查询。
     *
     * @param request 分页及筛选条件
     * @return 项目分页结果
     * @throws com.badminton.mes.common.exception.ServiceException 页面中的项目引用无效分类时抛出
     */
    PageResult<QualityInspectionItemRespVO> getItemPage(QualityInspectionItemPageReqVO request);
}
