package com.badminton.mes.module.scene.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.scene.dal.entity.SceneWorkReportEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 现场报工 Repository。
 *
 * @author Codex
 * @date 2026/07/13
 */
public interface SceneWorkReportRepository extends JpaRepository<SceneWorkReportEntity, Long> {

    Optional<SceneWorkReportEntity> findBySourceTypeAndSourceRecordIdAndDeletedFalse(
            Integer sourceType, Long sourceRecordId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT report FROM SceneWorkReportEntity report
            WHERE report.id = :id AND report.deleted = false
            """)
    Optional<SceneWorkReportEntity> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT COALESCE(SUM(report.qualifiedQuantity + report.defectQuantity), 0)
            FROM SceneWorkReportEntity report
            WHERE report.productionTaskId = :productionTaskId
              AND report.processId = :processId
              AND report.auditStatus = :auditStatus
              AND report.deleted = false
            """)
    java.math.BigDecimal sumApprovedQuantity(
            @Param("productionTaskId") Long productionTaskId,
            @Param("processId") Long processId,
            @Param("auditStatus") Integer auditStatus);
}
