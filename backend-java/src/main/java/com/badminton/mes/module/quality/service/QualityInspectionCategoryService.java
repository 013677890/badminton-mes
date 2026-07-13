package com.badminton.mes.module.quality.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionCategoryPageReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionCategoryRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionCategorySaveReqVO;

/** 检验分类 Service。 */
public interface QualityInspectionCategoryService {

    Long createCategory(QualityInspectionCategorySaveReqVO request);

    void updateCategory(Long id, QualityInspectionCategorySaveReqVO request);

    void deleteCategory(Long id);

    QualityInspectionCategoryRespVO getCategory(Long id);

    PageResult<QualityInspectionCategoryRespVO> getCategoryPage(QualityInspectionCategoryPageReqVO request);
}
