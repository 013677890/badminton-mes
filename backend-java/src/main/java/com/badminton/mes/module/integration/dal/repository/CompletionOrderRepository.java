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
 * <p>提供完工发布幂等查询、审核处理写锁及按生产任务聚合已审核数量。所有显式业务查询均排除
 * 逻辑删除数据；聚合直接在数据库完成，避免为计算已完工总量加载全部明细实体。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
public interface CompletionOrderRepository extends JpaRepository<CompletionOrderEntity, Long>,
        JpaSpecificationExecutor<CompletionOrderEntity> {

    /** 按完工单号查询未删除的已发布完工单，供发布入口前置幂等检查和并发冲突回查。 */
    java.util.Optional<CompletionOrderEntity> findByCompletionNoAndDeletedFalse(String completionNo);

    /**
     * 按主键悲观锁定未删除完工单，供审核状态迁移串行处理。
     *
     * <p>命中行锁保持到当前事务结束，防止两个审核请求同时基于旧状态更新同一完工单。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    /**
     * 汇总指定生产任务在目标审核状态下的有效完工数量。
     *
     * <p>{@code COALESCE} 使无匹配记录时返回 0 而不是 null，便于调用方直接参与数量运算。
     */
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
