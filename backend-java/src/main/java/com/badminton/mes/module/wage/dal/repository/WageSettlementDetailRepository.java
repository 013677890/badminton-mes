package com.badminton.mes.module.wage.dal.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.wage.dal.entity.WageSettlementDetailEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 计件工资结算明细 Repository。 */
public interface WageSettlementDetailRepository extends JpaRepository<WageSettlementDetailEntity, Long> {

    /** 查询结算中的有效明细。 */
    List<WageSettlementDetailEntity> findBySettlementIdAndActiveTrueAndDeletedFalseOrderByIdAsc(
            Long settlementId);

    /** 统计结算有效明细。 */
    long countBySettlementIdAndActiveTrueAndDeletedFalse(Long settlementId);

    /** 分页查询结算有效明细。 */
    Page<WageSettlementDetailEntity> findBySettlementIdAndActiveTrueAndDeletedFalse(
            Long settlementId, Pageable pageable);

    /** 查询结算中的指定有效明细。 */
    Optional<WageSettlementDetailEntity> findByIdAndSettlementIdAndActiveTrueAndDeletedFalse(
            Long id, Long settlementId);

    /** 查询结算历史关联的全部来源报工主键。 */
    @Query("SELECT DISTINCT detail.workRecordId FROM WageSettlementDetailEntity detail "
            + "WHERE detail.settlementId = :settlementId AND detail.deleted = false ORDER BY detail.workRecordId")
    List<Long> findWorkRecordIdsBySettlementId(@Param("settlementId") Long settlementId);

    /** 将旧明细释放为非活动状态，保留历史快照。 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE WageSettlementDetailEntity detail SET detail.active = false "
            + "WHERE detail.settlementId = :settlementId AND detail.active = true AND detail.deleted = false")
    int deactivateBySettlementId(@Param("settlementId") Long settlementId);

    /** 按员工汇总已审核结算。 */
    @Query("""
            SELECT detail.employeeId AS employeeId,
                   SUM(detail.qualifiedQuantity) AS qualifiedQuantity,
                   SUM(detail.defectQuantity) AS defectQuantity,
                   SUM(detail.finalAmountBasis) AS amountBasis
            FROM WageSettlementDetailEntity detail, WageSettlementEntity settlement
            WHERE settlement.id = detail.settlementId
              AND settlement.settlementStatus = :approvedStatus
              AND settlement.deleted = false
              AND detail.active = true
              AND detail.deleted = false
              AND detail.workDate BETWEEN :periodStart AND :periodEnd
              AND (:allIds = true OR detail.employeeId IN :ids)
            GROUP BY detail.employeeId
            ORDER BY detail.employeeId ASC
            """)
    List<EmployeeWageSummaryProjection> summarizeEmployees(
            @Param("approvedStatus") Integer approvedStatus,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd,
            @Param("allIds") boolean allIds,
            @Param("ids") Collection<Long> ids,
            Pageable pageable);

    /** 按工序汇总已审核结算。 */
    @Query("""
            SELECT detail.processId AS processId,
                   SUM(detail.qualifiedQuantity) AS qualifiedQuantity,
                   SUM(detail.defectQuantity) AS defectQuantity,
                   SUM(detail.finalAmountBasis) AS amountBasis
            FROM WageSettlementDetailEntity detail, WageSettlementEntity settlement
            WHERE settlement.id = detail.settlementId
              AND settlement.settlementStatus = :approvedStatus
              AND settlement.deleted = false
              AND detail.active = true
              AND detail.deleted = false
              AND detail.workDate BETWEEN :periodStart AND :periodEnd
              AND (:allIds = true OR detail.processId IN :ids)
            GROUP BY detail.processId
            ORDER BY detail.processId ASC
            """)
    List<ProcessWageSummaryProjection> summarizeProcesses(
            @Param("approvedStatus") Integer approvedStatus,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd,
            @Param("allIds") boolean allIds,
            @Param("ids") Collection<Long> ids,
            Pageable pageable);
}
