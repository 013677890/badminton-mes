package com.badminton.mes.module.quality.service.impl;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.quality.controller.vo.QualityInspectionItemSaveReqVO;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionCategoryEntity;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionItemEntity;
import com.badminton.mes.module.quality.dal.redis.QualityCache;
import com.badminton.mes.module.quality.dal.redis.QualityRedisKeyConstants;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionCategoryRepository;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionItemRepository;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionPlanItemRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@link QualityInspectionItemServiceImpl} 单元测试。 */
@ExtendWith(MockitoExtension.class)
class QualityInspectionItemServiceImplTest {

    private static final Long ITEM_ID = 200L;
    private static final Long CATEGORY_ID = 100L;

    @Mock
    private QualityInspectionItemRepository itemRepository;

    @Mock
    private QualityInspectionCategoryRepository categoryRepository;

    @Mock
    private QualityInspectionPlanItemRepository planItemRepository;

    @Mock
    private QualityCache qualityCache;

    private QualityInspectionItemServiceImpl itemService;

    @BeforeEach
    void setUp() {
        itemService = new QualityInspectionItemServiceImpl(
                itemRepository,
                categoryRepository,
                planItemRepository,
                qualityCache);
    }

    @Test
    @DisplayName("更新检验项目：清除项目及引用方案详情缓存")
    void updateItemEvictsItemAndPlanDetailCaches() {
        QualityInspectionItemEntity item = buildItem();
        QualityInspectionItemSaveReqVO request = buildUpdateRequest();
        QualityInspectionCategoryEntity category = buildEnabledCategory();
        List<Long> planIds = List.of(301L, 302L);
        when(itemRepository.findByIdAndDeletedFalseForUpdate(ITEM_ID))
                .thenReturn(Optional.of(item));
        when(categoryRepository.findByIdAndDeletedFalse(CATEGORY_ID))
                .thenReturn(Optional.of(category));
        when(planItemRepository.findDistinctPlanIdsByInspectionItemId(ITEM_ID))
                .thenReturn(planIds);

        itemService.updateItem(ITEM_ID, request);

        verify(itemRepository).saveAndFlush(item);
        verify(qualityCache).evictDetailAfterCommit(
                QualityRedisKeyConstants.INSPECTION_ITEM_RESOURCE,
                ITEM_ID);
        verify(qualityCache).evictDetailsAfterCommit(
                QualityRedisKeyConstants.INSPECTION_PLAN_RESOURCE,
                planIds);
    }

    private QualityInspectionItemEntity buildItem() {
        QualityInspectionItemEntity item = new QualityInspectionItemEntity();
        item.setId(ITEM_ID);
        item.setItemCode("COLOR");
        item.setItemName("颜色");
        item.setCategoryId(CATEGORY_ID);
        item.setValueType("TEXT");
        item.setJudgmentMethod("MANUAL");
        item.setRequiredFlag(false);
        item.setEnabledStatus(1);
        item.setDeleted(false);
        return item;
    }

    private QualityInspectionCategoryEntity buildEnabledCategory() {
        QualityInspectionCategoryEntity category = new QualityInspectionCategoryEntity();
        category.setId(CATEGORY_ID);
        category.setEnabledStatus(1);
        category.setDeleted(false);
        return category;
    }

    private QualityInspectionItemSaveReqVO buildUpdateRequest() {
        QualityInspectionItemSaveReqVO request = new QualityInspectionItemSaveReqVO();
        request.setItemCode("COLOR");
        request.setItemName("成品颜色");
        request.setCategoryId(CATEGORY_ID);
        request.setValueType("TEXT");
        request.setJudgmentMethod("MANUAL");
        request.setRequiredFlag(true);
        request.setEnabledStatus(1);
        return request;
    }
}
