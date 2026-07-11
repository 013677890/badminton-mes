package com.badminton.mes.module.wage.dal.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.wage.dal.entity.PieceRateRuleEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/** 计件规则 Repository。 */
public interface PieceRateRuleRepository extends JpaRepository<PieceRateRuleEntity, Long>,
        JpaSpecificationExecutor<PieceRateRuleEntity> {

    /** 按主键查询未删除规则。 */
    Optional<PieceRateRuleEntity> findByIdAndDeletedFalse(Long id);

    /** 判断工序是否仍被指定状态的未删除计件规则引用。 */
    boolean existsByProcessIdAndStatusAndDeletedFalse(Long processId, Integer status);

    /** 按主键写锁查询未删除规则。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rule FROM PieceRateRuleEntity rule WHERE rule.id = :id AND rule.deleted = false")
    Optional<PieceRateRuleEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    /**
     * 判断同一工序、同一产品维度是否存在重叠生效期间。
     */
    @Query("""
            SELECT CASE WHEN COUNT(rule) > 0 THEN true ELSE false END
            FROM PieceRateRuleEntity rule
            WHERE rule.processId = :processId
              AND ((:productId IS NULL AND rule.productId IS NULL) OR rule.productId = :productId)
              AND rule.deleted = false
              AND (:excludeId IS NULL OR rule.id <> :excludeId)
              AND (:effectiveEnd IS NULL OR rule.effectiveStart <= :effectiveEnd)
              AND (rule.effectiveEnd IS NULL OR rule.effectiveEnd >= :effectiveStart)
            """)
    boolean existsOverlapping(@Param("processId") Long processId,
                              @Param("productId") Long productId,
                              @Param("effectiveStart") LocalDate effectiveStart,
                              @Param("effectiveEnd") LocalDate effectiveEnd,
                              @Param("excludeId") Long excludeId);

    /**
     * 批量查询结算周期内可能命中的启用规则。
     */
    @Query("""
            SELECT rule FROM PieceRateRuleEntity rule
            WHERE rule.processId IN :processIds
              AND rule.status = :status
              AND rule.deleted = false
              AND rule.effectiveStart <= :periodEnd
              AND (rule.effectiveEnd IS NULL OR rule.effectiveEnd >= :periodStart)
            ORDER BY rule.processId ASC, rule.productId DESC, rule.effectiveStart DESC, rule.id DESC
            """)
    List<PieceRateRuleEntity> findCandidates(
            @Param("processIds") Collection<Long> processIds,
            @Param("status") Integer status,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd);
}
