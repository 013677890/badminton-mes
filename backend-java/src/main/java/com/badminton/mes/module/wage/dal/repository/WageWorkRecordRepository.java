package com.badminton.mes.module.wage.dal.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import com.badminton.mes.module.wage.dal.entity.WageWorkRecordEntity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/** 已审核报工快照 Repository。 */
public interface WageWorkRecordRepository extends JpaRepository<WageWorkRecordEntity, Long>,
        JpaSpecificationExecutor<WageWorkRecordEntity> {

    /** 批量查询已经导入的来源报工。 */
    List<WageWorkRecordEntity> findBySourceReportIdIn(Collection<Long> sourceReportIds);

    /**
     * 幂等插入报工快照。唯一键竞争时不改写既有快照并返回 0。
     *
     * <p>{@code INSERT IGNORE} 的插入和重复返回值固定为 1 和 0，不受 JDBC
     * {@code CLIENT_FOUND_ROWS} 配置影响，确保导入计数在各环境保持一致。</p>
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT IGNORE INTO wage_work_record
              (source_report_id, employee_id, work_date, work_order_id, process_id, product_id,
               qualified_quantity, defect_quantity, source_audit_time, create_by)
            VALUES
              (:sourceReportId, :employeeId, :workDate, :workOrderId, :processId, :productId,
               :qualifiedQuantity, :defectQuantity, :sourceAuditTime, :createBy)
            """, nativeQuery = true)
    int insertIdempotently(@Param("sourceReportId") Long sourceReportId,
                           @Param("employeeId") Long employeeId,
                           @Param("workDate") LocalDate workDate,
                           @Param("workOrderId") Long workOrderId,
                           @Param("processId") Long processId,
                           @Param("productId") Long productId,
                           @Param("qualifiedQuantity") java.math.BigDecimal qualifiedQuantity,
                           @Param("defectQuantity") java.math.BigDecimal defectQuantity,
                           @Param("sourceAuditTime") java.time.LocalDateTime sourceAuditTime,
                           @Param("createBy") Long createBy);

    /**
     * 按主键升序锁定报工快照，重新计算时与首次计算保持统一锁顺序。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT record FROM WageWorkRecordEntity record
            WHERE record.id IN :ids AND record.deleted = false
            ORDER BY record.id ASC
            """)
    List<WageWorkRecordEntity> findAllByIdInForUpdateOrderByIdAsc(@Param("ids") Collection<Long> ids);

    /**
     * 写锁查询尚未被有效结算明细占用的报工快照。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT record FROM WageWorkRecordEntity record
            WHERE record.workDate BETWEEN :periodStart AND :periodEnd
              AND (:allEmployees = true OR record.employeeId IN :employeeIds)
              AND record.deleted = false
              AND NOT EXISTS (
                    SELECT detail.id FROM WageSettlementDetailEntity detail
                    WHERE detail.workRecordId = record.id
                      AND detail.active = true
                      AND detail.deleted = false
              )
            ORDER BY record.id ASC
            """)
    List<WageWorkRecordEntity> findEligibleForUpdate(
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd,
            @Param("allEmployees") boolean allEmployees,
            @Param("employeeIds") Collection<Long> employeeIds,
            Pageable pageable);
}
