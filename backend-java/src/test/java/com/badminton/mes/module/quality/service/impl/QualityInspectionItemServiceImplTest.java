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

/**
 * {@link QualityInspectionItemServiceImpl} 的单元测试。
 *
 * <p>通过 Mock 项目、分类和方案关系仓储以及质量缓存隔离数据库与 Redis，重点验证项目更新时的
 * 分类有效性协作、实体持久化，以及服务是否请求缓存组件安排项目详情和全部引用方案详情的级联失效。</p>
 */
@ExtendWith(MockitoExtension.class)
class QualityInspectionItemServiceImplTest {

    /** 被更新检验项目的固定主键，同时用于项目缓存键断言。 */
    private static final Long ITEM_ID = 200L;

    /** 项目所属启用分类的固定主键，用于模拟分类有效性校验。 */
    private static final Long CATEGORY_ID = 100L;

    /** 隔离检验项目的锁定读取与更新持久化。 */
    @Mock
    private QualityInspectionItemRepository itemRepository;

    /** 隔离项目更新前对所属分类存在性和启用状态的查询。 */
    @Mock
    private QualityInspectionCategoryRepository categoryRepository;

    /** 隔离引用当前项目的方案主键集合查询。 */
    @Mock
    private QualityInspectionPlanItemRepository planItemRepository;

    /** 隔离 Redis，并验证项目及关联方案的提交后缓存失效。 */
    @Mock
    private QualityCache qualityCache;

    /** 使用全部 Mock 协作者构建的被测项目服务。 */
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
        // 先核对项目自身缓存键，再核对所有引用方案，覆盖更新的级联缓存副作用边界。
        verify(qualityCache).evictDetailAfterCommit(
                QualityRedisKeyConstants.INSPECTION_ITEM_RESOURCE,
                ITEM_ID);
        verify(qualityCache).evictDetailsAfterCommit(
                QualityRedisKeyConstants.INSPECTION_PLAN_RESOURCE,
                planIds);
    }

    /**
     * 构造数据库中的既有项目快照，为更新流程提供完整且可编辑的初始状态。
     */
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

    /**
     * 构造启用且未删除的分类夹具，使测试聚焦项目更新与缓存传播而非异常分支。
     */
    private QualityInspectionCategoryEntity buildEnabledCategory() {
        QualityInspectionCategoryEntity category = new QualityInspectionCategoryEntity();
        category.setId(CATEGORY_ID);
        category.setEnabledStatus(1);
        category.setDeleted(false);
        return category;
    }

    /**
     * 构造合法更新请求，集中表达本次更新后的项目字段快照。
     */
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
