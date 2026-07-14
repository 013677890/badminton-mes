package com.badminton.mes.module.quality.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.quality.dal.entity.QualityInspectionRecordEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/** 质量检验单 Repository。 */
public interface QualityInspectionRecordRepository extends JpaRepository<QualityInspectionRecordEntity, Long>,
        JpaSpecificationExecutor<QualityInspectionRecordEntity> {

    Optional<QualityInspectionRecordEntity> findByIdAndDeletedFalse(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select record from QualityInspectionRecordEntity record "
            + "where record.id = :id and record.deleted = false")
    Optional<QualityInspectionRecordEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);
}
