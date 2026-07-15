package com.badminton.mes.module.quality.service.impl;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.quality.controller.vo.QualityInspectionCategorySaveReqVO;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionCategoryEntity;
import com.badminton.mes.module.quality.dal.redis.QualityCache;
import com.badminton.mes.module.quality.dal.redis.QualityRedisKeyConstants;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionCategoryRepository;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionItemRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@link QualityInspectionCategoryServiceImpl} 单元测试。 */
@ExtendWith(MockitoExtension.class)
class QualityInspectionCategoryServiceImplTest {

    private static final Long CATEGORY_ID = 100L;

    @Mock
    private QualityInspectionCategoryRepository categoryRepository;

    @Mock
    private QualityInspectionItemRepository itemRepository;

    @Mock
    private QualityCache qualityCache;

    private QualityInspectionCategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new QualityInspectionCategoryServiceImpl(
                categoryRepository,
                itemRepository,
                qualityCache);
    }

    @Test
    @DisplayName("更新检验分类：清除分类及所属项目详情缓存")
    void updateCategoryEvictsCategoryAndItemDetailCaches() {
        QualityInspectionCategoryEntity category = buildCategory();
        QualityInspectionCategorySaveReqVO request = buildUpdateRequest();
        List<Long> itemIds = List.of(201L, 202L);
        when(categoryRepository.findByIdAndDeletedFalseForUpdate(CATEGORY_ID))
                .thenReturn(Optional.of(category));
        when(itemRepository.findIdsByCategoryIdAndDeletedFalse(CATEGORY_ID))
                .thenReturn(itemIds);

        categoryService.updateCategory(CATEGORY_ID, request);

        verify(categoryRepository).saveAndFlush(category);
        verify(qualityCache).evictDetailAfterCommit(
                QualityRedisKeyConstants.INSPECTION_CATEGORY_RESOURCE,
                CATEGORY_ID);
        verify(qualityCache).evictDetailsAfterCommit(
                QualityRedisKeyConstants.INSPECTION_ITEM_RESOURCE,
                itemIds);
    }

    private QualityInspectionCategoryEntity buildCategory() {
        QualityInspectionCategoryEntity category = new QualityInspectionCategoryEntity();
        category.setId(CATEGORY_ID);
        category.setCategoryCode("APPEARANCE");
        category.setCategoryName("外观检验");
        category.setEnabledStatus(1);
        category.setDeleted(false);
        return category;
    }

    private QualityInspectionCategorySaveReqVO buildUpdateRequest() {
        QualityInspectionCategorySaveReqVO request = new QualityInspectionCategorySaveReqVO();
        request.setCategoryCode("APPEARANCE");
        request.setCategoryName("外观质量检验");
        request.setEnabledStatus(1);
        return request;
    }
}
