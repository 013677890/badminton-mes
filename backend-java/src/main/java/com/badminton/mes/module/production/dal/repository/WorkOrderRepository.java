package com.badminton.mes.module.production.dal.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
