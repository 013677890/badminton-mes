package com.badminton.mes.module.production.dal.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.production.dal.entity.DispatchOrderEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 派工单 JPA Repository。
 *
 * <p>状态流转使用返回影响行数的 JPQL 更新保留 CAS 语义；
 * 产能校验聚合走 idx_line_date_shift 组合索引。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public interface DispatchOrderRepository extends JpaRepository<DispatchOrderEntity, Long>,
        JpaSpecificationExecutor<DispatchOrderEntity> {

    /**
     * 判断产线是否被任意未删除派工单引用。
     *
     * @param lineId 产线主键
     * @return true 表示存在派工引用
     */
    boolean existsByLineIdAndDeletedFalse(Long lineId);

    /**
     * 判断产线是否被指定状态的未删除派工单引用。
     *
     * @param lineId 产线主键
     * @param dispatchStatuses 派工状态集合
     * @return true 表示存在匹配派工
     */
    boolean existsByLineIdAndDispatchStatusInAndDeletedFalse(
            Long lineId, Collection<Integer> dispatchStatuses);

    /**
     * 按主键查询未删除的派工单。
     *
     * @param id 派工单主键
     * @return 派工单实体
     */
    Optional<DispatchOrderEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 仅取派工单所属工单 id(标量投影，不载入实体)。
     *
     * <p>写路径锁序为"先工单行后派工单行"，锁工单前需要 workOrderId 但
     * 不能提前把派工单载入一级缓存(锁后重读会命中缓存拿到过期快照)，
     * 故以标量查询获取。
     *
     * @param id 派工单主键
     * @return 所属工单 id
     */
    @Query("""
            SELECT dispatch.workOrderId FROM DispatchOrderEntity dispatch
            WHERE dispatch.id = :id AND dispatch.deleted = false
            """)
    Optional<Long> findWorkOrderIdById(@Param("id") Long id);

    /**
     * 按主键加悲观写锁查询未删除派工单，写路径在锁定工单行后调用，
     * 保证读到最新已提交状态且阻塞并发修改/取消。
     *
     * @param id 派工单主键
     * @return 派工单实体
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT dispatch FROM DispatchOrderEntity dispatch
            WHERE dispatch.id = :id AND dispatch.deleted = false
            """)
    Optional<DispatchOrderEntity> findByIdForUpdate(@Param("id") Long id);

    /**
     * 查询工单下的派工单，最新在前。
     *
     * @param workOrderId 工单主键
     * @return 派工单列表，无数据时为空集合
     */
    List<DispatchOrderEntity> findByWorkOrderIdAndDeletedFalseOrderByIdDesc(Long workOrderId);

    /**
     * 产线排程视图：查询产线在日期区间内的派工单(排除已取消)。
     *
     * @param lineId          产线主键
     * @param startDate       起始日期(含)
     * @param endDate         结束日期(含)
     * @param cancelledStatus 已取消状态值(排除)
     * @return 派工单列表，按日期/班次升序
     */
    @Query("""
            SELECT dispatch FROM DispatchOrderEntity dispatch
            WHERE dispatch.lineId = :lineId
              AND dispatch.planDate BETWEEN :startDate AND :endDate
              AND dispatch.dispatchStatus <> :cancelledStatus
              AND dispatch.deleted = false
            ORDER BY dispatch.planDate ASC, dispatch.shiftId ASC, dispatch.id ASC
            """)
    List<DispatchOrderEntity> findLineSchedule(@Param("lineId") Long lineId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate,
                                               @Param("cancelledStatus") Integer cancelledStatus);

    /**
     * 汇总产线某日某班次的已排产数量(排除已取消)，防超产能校验。
     *
     * @param lineId          产线主键
     * @param planDate        排产日期
     * @param shiftId         班次主键
     * @param cancelledStatus 已取消状态值(排除)
     * @param excludeId       调整场景排除自身，可空(新建传 null)
     * @return 已排产数量合计，无数据时为 0
     */
    @Query("""
            SELECT COALESCE(SUM(dispatch.planQuantity), 0) FROM DispatchOrderEntity dispatch
            WHERE dispatch.lineId = :lineId
              AND dispatch.planDate = :planDate
              AND dispatch.shiftId = :shiftId
              AND dispatch.dispatchStatus <> :cancelledStatus
              AND dispatch.deleted = false
              AND (:excludeId IS NULL OR dispatch.id <> :excludeId)
            """)
    long sumPlannedQuantity(@Param("lineId") Long lineId,
                            @Param("planDate") LocalDate planDate,
                            @Param("shiftId") Long shiftId,
                            @Param("cancelledStatus") Integer cancelledStatus,
                            @Param("excludeId") Long excludeId);

    /**
     * 状态流转 CAS：仅当当前状态等于前置状态才更新。
     *
     * @param id         派工单主键
     * @param fromStatus 前置状态
     * @param toStatus   目标状态
     * @return 影响行数；1 成功，0 表示不存在、已删除或状态不满足
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE DispatchOrderEntity dispatch
            SET dispatch.dispatchStatus = :toStatus,
                dispatch.updateTime = CURRENT_TIMESTAMP
            WHERE dispatch.id = :id
              AND dispatch.dispatchStatus = :fromStatus
              AND dispatch.deleted = false
            """)
    int updateStatus(@Param("id") Long id,
                     @Param("fromStatus") Integer fromStatus,
                     @Param("toStatus") Integer toStatus);

    /**
     * 审核 CAS：待审核 → 已审核，同时落审核人与审核时间。
     *
     * @param id          派工单主键
     * @param fromStatus  前置状态(待审核)
     * @param toStatus    目标状态(已审核)
     * @param auditBy     审核人
     * @param auditTime   审核时间
     * @return 影响行数；1 成功，0 表示状态不满足
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE DispatchOrderEntity dispatch
            SET dispatch.dispatchStatus = :toStatus,
                dispatch.auditBy = :auditBy,
                dispatch.auditTime = :auditTime,
                dispatch.updateTime = CURRENT_TIMESTAMP
            WHERE dispatch.id = :id
              AND dispatch.dispatchStatus = :fromStatus
              AND dispatch.deleted = false
            """)
    int updateToAudited(@Param("id") Long id,
                        @Param("fromStatus") Integer fromStatus,
                        @Param("toStatus") Integer toStatus,
                        @Param("auditBy") Long auditBy,
                        @Param("auditTime") LocalDateTime auditTime);
}
