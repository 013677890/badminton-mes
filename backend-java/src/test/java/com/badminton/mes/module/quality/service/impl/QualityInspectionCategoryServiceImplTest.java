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

/**
 * {@link QualityInspectionCategoryServiceImpl} 的单元测试。
 *
 * <p>使用 Mock 分类仓储、项目仓储和质量缓存隔离数据库及 Redis，重点验证分类更新持久化，
 * 以及服务是否请求缓存组件安排分类详情和全部所属项目详情的事务后级联失效。</p>
 */
@ExtendWith(MockitoExtension.class)
class QualityInspectionCategoryServiceImplTest {

    /** 被更新分类的固定主键，同时用于分类缓存键断言。 */
    private static final Long CATEGORY_ID = 100L;

    /** 隔离分类的锁定读取和更新持久化。 */
    @Mock
    private QualityInspectionCategoryRepository categoryRepository;

    /** 隔离分类下未删除项目主键集合的查询。 */
    @Mock
    private QualityInspectionItemRepository itemRepository;

    /** 隔离 Redis，并记录分类与所属项目的提交后缓存失效请求。 */
    @Mock
    private QualityCache qualityCache;

    /** 使用全部 Mock 协作者构建的被测分类服务。 */
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
        // 分类更新会影响自身和所属项目详情，分别验证资源类型与受影响主键集合。
        verify(qualityCache).evictDetailAfterCommit(
                QualityRedisKeyConstants.INSPECTION_CATEGORY_RESOURCE,
                CATEGORY_ID);
        verify(qualityCache).evictDetailsAfterCommit(
                QualityRedisKeyConstants.INSPECTION_ITEM_RESOURCE,
                itemIds);
    }

    /**
     * 构造数据库中的既有分类快照，为更新流程提供启用且未删除的初始实体。
     */
    private QualityInspectionCategoryEntity buildCategory() {
        QualityInspectionCategoryEntity category = new QualityInspectionCategoryEntity();
        category.setId(CATEGORY_ID);
        category.setCategoryCode("APPEARANCE");
        category.setCategoryName("外观检验");
        category.setEnabledStatus(1);
        category.setDeleted(false);
        return category;
    }

    /**
     * 构造合法分类更新请求，集中表达更新后的名称及启用状态。
     */
    private QualityInspectionCategorySaveReqVO buildUpdateRequest() {
        QualityInspectionCategorySaveReqVO request = new QualityInspectionCategorySaveReqVO();
        request.setCategoryCode("APPEARANCE");
        request.setCategoryName("外观质量检验");
        request.setEnabledStatus(1);
        return request;
    }
}
