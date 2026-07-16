package com.badminton.mes.module.quality.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionCategoryPageReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionCategoryRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionCategorySaveReqVO;

/**
 * 检验分类应用服务契约。
 *
 * <p>分类是检验项目的上级主数据。写操作会维护分类编码唯一性，并在事务提交后清理相关详情缓存；
 * 删除采用逻辑删除，且仅允许删除尚未被有效检验项目引用的分类。</p>
 */
public interface QualityInspectionCategoryService {

    /**
     * 创建检验分类。
     *
     * <p>前置条件：分类编码在未删除数据中唯一。未指定启用状态时按启用处理。</p>
     * <p>副作用：持久化分类并记录当前操作人。</p>
     *
     * @param request 分类可编辑字段
     * @return 新分类主键
     * @throws com.badminton.mes.common.exception.ServiceException 分类编码重复时抛出
     */
    Long createCategory(QualityInspectionCategorySaveReqVO request);

    /**
     * 更新检验分类。
     *
     * <p>前置条件：分类存在且未删除，新编码不得与其他有效分类重复。</p>
     * <p>副作用：锁定并更新分类；事务提交后清理分类缓存，以及所有引用该分类的检验项目缓存，
     * 防止项目响应中的分类名称、编码冗余信息过期。</p>
     *
     * @param id 分类主键
     * @param request 分类可编辑字段
     * @throws com.badminton.mes.common.exception.ServiceException 分类不存在或编码重复时抛出
     */
    void updateCategory(Long id, QualityInspectionCategorySaveReqVO request);

    /**
     * 逻辑删除检验分类。
     *
     * <p>前置条件：分类存在，且没有未删除的检验项目引用该分类。</p>
     * <p>副作用：替换原分类编码、停用并标记删除，事务提交后清理分类详情缓存。</p>
     *
     * @param id 分类主键
     * @throws com.badminton.mes.common.exception.ServiceException 分类不存在、仍被项目引用或删除占位编码冲突时抛出
     */
    void deleteCategory(Long id);

    /**
     * 查询分类详情。
     *
     * <p>优先读取详情缓存；缓存未命中时查询未删除实体并回填缓存。</p>
     *
     * @param id 分类主键
     * @return 分类详情
     * @throws com.badminton.mes.common.exception.ServiceException 分类不存在时抛出
     */
    QualityInspectionCategoryRespVO getCategory(Long id);

    /**
     * 分页查询未删除的检验分类。
     *
     * <p>页码超过末页时收敛到最后一页；无数据时返回保留请求分页参数的空结果。</p>
     *
     * @param request 分页及筛选条件
     * @return 分类分页结果
     */
    PageResult<QualityInspectionCategoryRespVO> getCategoryPage(QualityInspectionCategoryPageReqVO request);
}
