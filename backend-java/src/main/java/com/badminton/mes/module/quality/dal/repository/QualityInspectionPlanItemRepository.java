package com.badminton.mes.module.quality.dal.repository;

import java.util.List;

import com.badminton.mes.module.quality.dal.entity.QualityInspectionPlanItemEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 检验方案明细及方案级规则快照持久化仓库。
 *
 * <p>明细没有独立逻辑删除状态，生命周期从属于具体方案版本；项目引用查询用于删除保护和项目变更后
 * 对相关方案详情缓存进行级联失效。
 */
public interface QualityInspectionPlanItemRepository
        extends JpaRepository<QualityInspectionPlanItemEntity, Long> {

    /** 判断检验项目是否已被任一方案版本引用，用于阻止删除仍有历史方案关系的项目。 */
    boolean existsByInspectionItemId(Long inspectionItemId);

    /** 查询引用指定项目的不同方案版本主键，供项目变更后批量失效方案详情缓存。 */
    @Query("select distinct planItem.planId from QualityInspectionPlanItemEntity planItem "
            + "where planItem.inspectionItemId = :inspectionItemId")
    List<Long> findDistinctPlanIdsByInspectionItemId(@Param("inspectionItemId") Long inspectionItemId);

    /**
     * 按业务排序号、主键稳定读取方案版本的全部规则快照。
     *
     * <p>主键作为同排序号时的次级顺序，保证详情展示、版本复制和结果快照生成次序确定。
     */
    List<QualityInspectionPlanItemEntity> findByPlanIdOrderBySortOrderAscIdAsc(Long planId);

    /** 统计方案明细数量，用于审核前确认草稿方案至少包含一个检验项目。 */
    long countByPlanId(Long planId);

    /** 按方案版本批量删除明细，仅用于草稿方案整体删除或明细重建。 */
    void deleteByPlanId(Long planId);
}
