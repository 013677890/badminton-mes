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

/**
 * {@link QualityCache} 的单元测试。
 *
 * <p>使用 Mock Redis 模板和值操作隔离真实 Redis，保留事务同步管理器以观察提交回调，重点覆盖
 * 带版本详情缓存的未命中回填、Lua 原子写入参数，以及批量去重后延迟失效的副作用。</p>
 */
@ExtendWith(MockitoExtension.class)
class QualityCacheTest {

    /** 固定分类主键，用于生成详情键与版本键并核对脚本参数。 */
    private static final Long CATEGORY_ID = 100L;

    /** 隔离 Redis 脚本执行和 ValueOperations 获取。 */
    @Mock
    private StringRedisTemplate stringRedisTemplate;

    /** 隔离详情值与版本值读取，精确塑造缓存命中状态。 */
    @Mock
    private ValueOperations<String, String> valueOperations;

    /** 使用真实 JSON 映射器保留缓存序列化行为，不依赖 Spring 容器。 */
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    /** 注入 Mock Redis 模板后的被测缓存组件。 */
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
        // 先观测版本再模拟详情未命中，构造加载期间需要版本防护的缓存回填窗口。
        when(valueOperations.get(versionKey)).thenReturn("7");
        when(valueOperations.get(detailKey)).thenReturn(null);

        QualityInspectionCategoryRespVO result = qualityCache.getOrLoadDetail(
                QualityRedisKeyConstants.INSPECTION_CATEGORY_RESOURCE,
                CATEGORY_ID,
                QualityInspectionCategoryRespVO.class,
                () -> loadedCategory);

        assertThat(result).isSameAs(loadedCategory);
        // 脚本参数同时锁定键顺序、观测版本和 TTL，证明回填采用原子版本校验而非普通写入。
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

        // 显式建立事务同步上下文，以便区分方法调用阶段与提交回调阶段的 Redis 副作用。
        TransactionSynchronizationManager.initSynchronization();
        try {
            qualityCache.evictDetailsAfterCommit(
                    QualityRedisKeyConstants.INSPECTION_CATEGORY_RESOURCE,
                    List.of(CATEGORY_ID, CATEGORY_ID, secondCategoryId));

            // 提交前禁止执行脚本；手动触发回调后，每个去重主键应各执行一次版本递增与详情删除。
            verify(stringRedisTemplate, never()).execute(any(RedisScript.class), anyList());
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            verify(stringRedisTemplate).execute(any(RedisScript.class), eq(firstCacheKeys));
            verify(stringRedisTemplate).execute(any(RedisScript.class), eq(secondCacheKeys));
        } finally {
            // 清理线程绑定的事务同步状态，防止本测试的回调污染同线程后续用例。
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
