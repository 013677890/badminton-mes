package com.badminton.mes.module.quality.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionCategoryPageReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionCategoryRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionCategorySaveReqVO;

/**
 * 质量检验分类档案 Service，由分类 Controller 调用，并被检验项目和检验方案引用。
 *
 * @author MES 开发组
 * @date 2026/07/16
 */
public interface QualityInspectionCategoryService {

    /** 新增检验分类。 */
    Long createCategory(QualityInspectionCategorySaveReqVO request);

    /** 修改检验分类名称、编码或启用状态。 */
    void updateCategory(Long id, QualityInspectionCategorySaveReqVO request);

    /** 逻辑删除检验分类，避免破坏历史检验数据。 */
    void deleteCategory(Long id);

    /** 查询单条检验分类详情。 */
    QualityInspectionCategoryRespVO getCategory(Long id);

    /** 按关键字和启用状态分页查询检验分类。 */
    PageResult<QualityInspectionCategoryRespVO> getCategoryPage(QualityInspectionCategoryPageReqVO request);
}
