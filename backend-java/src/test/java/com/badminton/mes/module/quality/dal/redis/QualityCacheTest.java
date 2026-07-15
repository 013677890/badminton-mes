package com.badminton.mes.module.quality.dal.redis;

import java.util.List;

import com.badminton.mes.module.quality.controller.vo.QualityInspectionCategoryRespVO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@link QualityCache} 单元测试。 */
@ExtendWith(MockitoExtension.class)
class QualityCacheTest {

    private static final Long CATEGORY_ID = 100L;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private QualityCache qualityCache;

    @BeforeEach
    void setUp() {
        qualityCache = new QualityCache(stringRedisTemplate, jsonMapper);
    }

    @Test
    @DisplayName("缓存未命中：仅在详情版本未变化时执行原子回填")
    @SuppressWarnings("unchecked")
    void getOrLoadDetailWritesWithObservedVersion() {
        String detailKey = QualityRedisKeyConstants.detailKey(
                QualityRedisKeyConstants.INSPECTION_CATEGORY_RESOURCE,
                CATEGORY_ID);
        String versionKey = QualityRedisKeyConstants.detailVersionKey(
                QualityRedisKeyConstants.INSPECTION_CATEGORY_RESOURCE,
                CATEGORY_ID);
        QualityInspectionCategoryRespVO loadedCategory = new QualityInspectionCategoryRespVO();
        loadedCategory.setId(CATEGORY_ID);
        loadedCategory.setCategoryCode("APPEARANCE");
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(versionKey)).thenReturn("7");
        when(valueOperations.get(detailKey)).thenReturn(null);

        QualityInspectionCategoryRespVO result = qualityCache.getOrLoadDetail(
                QualityRedisKeyConstants.INSPECTION_CATEGORY_RESOURCE,
                CATEGORY_ID,
                QualityInspectionCategoryRespVO.class,
                () -> loadedCategory);

        assertThat(result).isSameAs(loadedCategory);
        verify(stringRedisTemplate).execute(
                any(RedisScript.class),
                eq(List.of(detailKey, versionKey)),
                eq("7"),
                eq(Long.toString(QualityRedisKeyConstants.QUALITY_DETAIL_TTL.toMillis())),
                anyString());
    }

    @Test
    @DisplayName("批量失效：事务提交后递增版本并删除去重后的详情缓存")
    @SuppressWarnings("unchecked")
    void evictDetailsAfterCommitDefersVersionedInvalidation() {
        Long secondCategoryId = 101L;
        List<String> firstCacheKeys = List.of(
                QualityRedisKeyConstants.detailKey(
                        QualityRedisKeyConstants.INSPECTION_CATEGORY_RESOURCE,
                        CATEGORY_ID),
                QualityRedisKeyConstants.detailVersionKey(
                        QualityRedisKeyConstants.INSPECTION_CATEGORY_RESOURCE,
                        CATEGORY_ID));
        List<String> secondCacheKeys = List.of(
                QualityRedisKeyConstants.detailKey(
                        QualityRedisKeyConstants.INSPECTION_CATEGORY_RESOURCE,
                        secondCategoryId),
                QualityRedisKeyConstants.detailVersionKey(
                        QualityRedisKeyConstants.INSPECTION_CATEGORY_RESOURCE,
                        secondCategoryId));

        TransactionSynchronizationManager.initSynchronization();
        try {
            qualityCache.evictDetailsAfterCommit(
                    QualityRedisKeyConstants.INSPECTION_CATEGORY_RESOURCE,
                    List.of(CATEGORY_ID, CATEGORY_ID, secondCategoryId));

            verify(stringRedisTemplate, never()).execute(any(RedisScript.class), anyList());
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            verify(stringRedisTemplate).execute(any(RedisScript.class), eq(firstCacheKeys));
            verify(stringRedisTemplate).execute(any(RedisScript.class), eq(secondCacheKeys));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
