package com.badminton.mes.module.integration.dal.repository;

import com.badminton.mes.module.integration.dal.entity.CompletionOrderEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 生产完工单读取 Repository。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
public interface CompletionOrderRepository extends JpaRepository<CompletionOrderEntity, Long>,
        JpaSpecificationExecutor<CompletionOrderEntity> {

    /** 按完工单号查询未删除的已发布完工单。 */
    java.util.Optional<CompletionOrderEntity> findByCompletionNoAndDeletedFalse(String completionNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT completion FROM CompletionOrderEntity completion
            WHERE completion.id = :id AND completion.deleted = false
            """)
    java.util.Optional<CompletionOrderEntity> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT COALESCE(SUM(completion.completionQuantity), 0)
            FROM CompletionOrderEntity completion
            WHERE completion.productionTaskId = :productionTaskId
              AND completion.auditStatus = :auditStatus
              AND completion.deleted = false
            """)
    Long sumCompletionQuantityByTaskAndStatus(
            @Param("productionTaskId") Long productionTaskId,
            @Param("auditStatus") Integer auditStatus);
}
