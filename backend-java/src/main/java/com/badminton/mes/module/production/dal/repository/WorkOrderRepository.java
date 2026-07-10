package com.badminton.mes.module.production.dal.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 生产工单 JPA Repository。
 *
 * <p>状态流转和逻辑删除使用返回影响行数的 JPQL 更新，保留数据库层 CAS 语义。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
public interface WorkOrderRepository extends JpaRepository<WorkOrderEntity, Long>,
        JpaSpecificationExecutor<WorkOrderEntity> {

    /**
     * 按主键查询未删除的工单。
     *
     * @param id 工单主键
     * @return 工单实体
     */
    Optional<WorkOrderEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 按主键集合批量查询未删除工单，欠料看板下钻回填工单号/产品名。
     *
     * @param ids 工单主键集合，调用方保证非空且规模有限
     * @return 工单列表
     */
    List<WorkOrderEntity> findByIdInAndDeletedFalse(Collection<Long> ids);

    /**
     * 判断未删除工单中是否已存在指定工单号。
     *
     * @param workOrderNo 工单号
     * @return true 存在，false 不存在
     */
    boolean existsByWorkOrderNoAndDeletedFalse(String workOrderNo);

    /**
     * 更新工单计划信息，仅已创建状态允许修改。可空字段使用 COALESCE 保持旧值。
     *
     * @return 影响行数；0 表示工单不存在、已删除或状态已变化
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE WorkOrderEntity workOrder
            SET workOrder.productId = :productId,
                workOrder.productName = :productName,
                workOrder.spec = :spec,
                workOrder.unitId = :unitId,
                workOrder.batchNo = COALESCE(:batchNo, workOrder.batchNo),
                workOrder.bomId = COALESCE(:bomId, workOrder.bomId),
                workOrder.routingId = COALESCE(:routingId, workOrder.routingId),
                workOrder.customerId = COALESCE(:customerId, workOrder.customerId),
                workOrder.workshopId = :workshopId,
                workOrder.planQuantity = :planQuantity,
                workOrder.overRatio = COALESCE(:overRatio, workOrder.overRatio),
                workOrder.priority = COALESCE(:priority, workOrder.priority),
                workOrder.planStartTime = :planStartTime,
                workOrder.planEndTime = :planEndTime,
                workOrder.updateTime = CURRENT_TIMESTAMP
            WHERE workOrder.id = :id
              AND workOrder.orderStatus = :createdStatus
              AND workOrder.deleted = false
            """)
    int updatePlan(@Param("id") Long id,
                   @Param("productId") Long productId,
                   @Param("productName") String productName,
                   @Param("spec") String spec,
                   @Param("unitId") Long unitId,
                   @Param("batchNo") String batchNo,
                   @Param("bomId") Long bomId,
                   @Param("routingId") Long routingId,
                   @Param("customerId") Long customerId,
                   @Param("workshopId") Long workshopId,
                   @Param("planQuantity") Integer planQuantity,
                   @Param("overRatio") BigDecimal overRatio,
                   @Param("priority") Integer priority,
                   @Param("planStartTime") LocalDateTime planStartTime,
                   @Param("planEndTime") LocalDateTime planEndTime,
                   @Param("createdStatus") Integer createdStatus);

    /**
     * 下达工单：已创建 -> 已下达，且 BOM 与工艺路线必须已维护。
     *
     * @return 影响行数；1 成功，0 条件不满足
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE WorkOrderEntity workOrder
            SET workOrder.orderStatus = :releasedStatus,
                workOrder.updateTime = CURRENT_TIMESTAMP
            WHERE workOrder.id = :id
              AND workOrder.orderStatus = :createdStatus
              AND workOrder.deleted = false
              AND workOrder.bomId IS NOT NULL
              AND workOrder.routingId IS NOT NULL
            """)
    int updateToReleased(@Param("id") Long id,
                         @Param("createdStatus") Integer createdStatus,
                         @Param("releasedStatus") Integer releasedStatus);

    /**
     * 通用状态流转 CAS：仅当当前状态在允许的前置状态集合内才更新。
     * 用于暂停、恢复、完工、关闭、作废等流转。
     *
     * @param id           工单主键
     * @param fromStatuses 允许的前置状态集合
     * @param toStatus     目标状态
     * @return 影响行数；1 成功，0 表示工单不存在、已删除或状态不满足
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE WorkOrderEntity workOrder
            SET workOrder.orderStatus = :toStatus,
                workOrder.updateTime = CURRENT_TIMESTAMP
            WHERE workOrder.id = :id
              AND workOrder.orderStatus IN :fromStatuses
              AND workOrder.deleted = false
            """)
    int updateStatus(@Param("id") Long id,
                     @Param("fromStatuses") Collection<Integer> fromStatuses,
                     @Param("toStatus") Integer toStatus);

    /**
     * 完工流转 CAS：状态与"完工数量不超过 计划数量×(1+超产比例) 向下取整"在同一条
     * UPDATE 内原子校验，消除先读后写的检查-执行竞态(并发报工顶高完工数量的场景)。
     *
     * <p>单一前置状态入参：命中即可确定真实的变更前状态，供状态日志留痕。
     *
     * @param id             工单主键
     * @param fromStatus     前置状态(已下达或生产中，由调用方逐个尝试)
     * @param finishedStatus 已完工状态值
     * @return 影响行数；1 成功，0 表示工单不存在、状态不满足或完工数量超上限
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE WorkOrderEntity workOrder
            SET workOrder.orderStatus = :finishedStatus,
                workOrder.updateTime = CURRENT_TIMESTAMP
            WHERE workOrder.id = :id
              AND workOrder.orderStatus = :fromStatus
              AND workOrder.deleted = false
              AND (workOrder.finishQuantity IS NULL
                   OR workOrder.finishQuantity <= FLOOR(workOrder.planQuantity
                        * (1 + COALESCE(workOrder.overRatio, 0) / 100)))
            """)
    int updateToFinished(@Param("id") Long id,
                         @Param("fromStatus") Integer fromStatus,
                         @Param("finishedStatus") Integer finishedStatus);

    /**
     * 已下达后的计划变更：仅允许修改计划数量与计划时间，且计划数量不能低于已派工数量。
     * 变更原因由 Service 记入状态日志。
     *
     * @return 影响行数；0 表示工单不存在、状态不允许或计划数量低于已派工数量
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE WorkOrderEntity workOrder
            SET workOrder.planQuantity = :planQuantity,
                workOrder.planStartTime = :planStartTime,
                workOrder.planEndTime = :planEndTime,
                workOrder.updateTime = CURRENT_TIMESTAMP
            WHERE workOrder.id = :id
              AND workOrder.orderStatus = :releasedStatus
              AND workOrder.deleted = false
              AND workOrder.dispatchedQuantity <= :planQuantity
            """)
    int updateReleasedPlan(@Param("id") Long id,
                           @Param("planQuantity") Integer planQuantity,
                           @Param("planStartTime") LocalDateTime planStartTime,
                           @Param("planEndTime") LocalDateTime planEndTime,
                           @Param("releasedStatus") Integer releasedStatus);

    /**
     * 逻辑删除工单，仅已创建状态允许删除。
     *
     * @return 影响行数；0 表示工单不存在、已删除或状态已变化
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE WorkOrderEntity workOrder
            SET workOrder.deleted = true,
                workOrder.updateTime = CURRENT_TIMESTAMP
            WHERE workOrder.id = :id
              AND workOrder.orderStatus = :createdStatus
              AND workOrder.deleted = false
            """)
    int logicDeleteById(@Param("id") Long id, @Param("createdStatus") Integer createdStatus);

    /**
     * 按主键悲观锁查询工单行(SELECT ... FOR UPDATE)，须在事务内调用。
     *
     * <p>齐套重算与派工共用这把工单行锁：串行化"软删旧结果+插新结果"防止
     * 并发重算残留双份，也阻断"边派工边重算"的交叉写(业务 SQL 1.4)。
     *
     * @param id 工单主键
     * @return 工单实体
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT workOrder FROM WorkOrderEntity workOrder
            WHERE workOrder.id = :id
              AND workOrder.deleted = false
            """)
    Optional<WorkOrderEntity> findByIdForUpdate(@Param("id") Long id);

    /**
     * 冗余回写工单齐套状态(业务 SQL 1.3 的回写步骤)。
     *
     * @param id        工单主键
     * @param kitStatus 工单级齐套状态(各物料行 MAX)
     * @return 影响行数
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE WorkOrderEntity workOrder
            SET workOrder.kitStatus = :kitStatus,
                workOrder.updateTime = CURRENT_TIMESTAMP
            WHERE workOrder.id = :id
              AND workOrder.deleted = false
            """)
    int updateKitStatus(@Param("id") Long id, @Param("kitStatus") Integer kitStatus);

    /**
     * 累加已派工数量，WHERE 条件兜底超派(业务 SQL 1.4)：
     * 累加后不得超过 计划数量×(1+超产比例) 向下取整。
     *
     * @param id       工单主键
     * @param quantity 本次派工数量
     * @return 影响行数；0 表示会超派，调用方应抛错回滚
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE WorkOrderEntity workOrder
            SET workOrder.dispatchedQuantity = workOrder.dispatchedQuantity + :quantity,
                workOrder.updateTime = CURRENT_TIMESTAMP
            WHERE workOrder.id = :id
              AND workOrder.deleted = false
              AND workOrder.dispatchedQuantity + :quantity
                  <= FLOOR(workOrder.planQuantity * (1 + COALESCE(workOrder.overRatio, 0) / 100))
            """)
    int increaseDispatchedQuantity(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * 回退已派工数量(取消派工单)，WHERE 条件兜底防负数。
     *
     * @param id       工单主键
     * @param quantity 回退数量
     * @return 影响行数；0 表示会回退成负数，数据已不一致需人工介入
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE WorkOrderEntity workOrder
            SET workOrder.dispatchedQuantity = workOrder.dispatchedQuantity - :quantity,
                workOrder.updateTime = CURRENT_TIMESTAMP
            WHERE workOrder.id = :id
              AND workOrder.deleted = false
              AND workOrder.dispatchedQuantity >= :quantity
            """)
    int decreaseDispatchedQuantity(@Param("id") Long id, @Param("quantity") Integer quantity);
}
