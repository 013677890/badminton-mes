package com.badminton.mes.module.quality.dal.repository;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.quality.dal.entity.QualityInspectionResultEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/** 质量检验项目结果 Repository。 */
public interface QualityInspectionResultRepository extends JpaRepository<QualityInspectionResultEntity, Long> {

    List<QualityInspectionResultEntity> findByInspectionRecordIdOrderBySortOrderAscIdAsc(Long inspectionRecordId);

    Optional<QualityInspectionResultEntity> findByIdAndInspectionRecordId(Long id, Long inspectionRecordId);
}
