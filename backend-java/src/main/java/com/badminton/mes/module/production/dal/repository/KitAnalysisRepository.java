package com.badminton.mes.module.production.dal.repository;

import java.math.BigDecimal;
import java.util.List;

import com.badminton.mes.module.production.dal.entity.KitAnalysisEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 齐套分析结果 JPA Repository。
 *
 * <p>每次分析"软删旧结果 + 插入新结果"在同一事务内完成，
 * 未删除行即最新分析快照；欠料看板走 idx_material_id 聚合。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public interface KitAnalysisRepository extends JpaRepository<KitAnalysisEntity, Long> {

    /**
     * 查询指定工单最新一次齐套分析结果。
     *
     * @param workOrderId 工单主键
     * @return 逐物料分析结果，未分析时为空集合
     */
    List<KitAnalysisEntity> findByWorkOrderIdAndDeletedFalse(Long workOrderId);

    /**
     * 软删指定工单旧分析结果，重新分析前调用(同一事务)。
     *
     * @param workOrderId 工单主键
     * @return 影响行数
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE KitAnalysisEntity analysis
            SET analysis.deleted = true,
                analysis.updateTime = CURRENT_TIMESTAMP
            WHERE analysis.workOrderId = :workOrderId
              AND analysis.deleted = false
            """)
    int softDeleteByWorkOrderId(@Param("workOrderId") Long workOrderId);

    /**
     * 欠料看板聚合：按物料汇总欠料总量、影响工单数、在途数量，欠料量降序。
     *
     * <p>在途数量取 MAX：各工单行来自不同时刻的分析快照，取最大值仅作看板展示参考。
     *
     * @return 聚合投影列表，无欠料时为空集合
     */
    @Query("""
            SELECT analysis.materialId AS materialId,
                   SUM(analysis.shortageQuantity) AS totalShortage,
                   COUNT(DISTINCT analysis.workOrderId) AS affectedOrderCount,
                   MAX(analysis.transitQuantity) AS transitQuantity
            FROM KitAnalysisEntity analysis
            WHERE analysis.deleted = false
              AND analysis.shortageQuantity > 0
            GROUP BY analysis.materialId
            ORDER BY SUM(analysis.shortageQuantity) DESC
            """)
    List<ShortageBoardProjection> aggregateShortageBoard();

    /**
     * 欠料看板下钻：查询某物料欠料的全部工单行。
     *
     * @param materialId 物料主键
     * @param zero       欠料下限(传 BigDecimal.ZERO)
     * @return 欠料行列表，无数据时为空集合
     */
    List<KitAnalysisEntity> findByMaterialIdAndShortageQuantityGreaterThanAndDeletedFalse(
            Long materialId, BigDecimal zero);
}
