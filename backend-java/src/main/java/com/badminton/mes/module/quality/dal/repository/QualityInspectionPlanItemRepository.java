package com.badminton.mes.module.quality.dal.repository;

import java.util.List;

import com.badminton.mes.module.quality.dal.entity.QualityInspectionPlanItemEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 检验方案明细 Repository。 */
public interface QualityInspectionPlanItemRepository
        extends JpaRepository<QualityInspectionPlanItemEntity, Long> {

    boolean existsByInspectionItemId(Long inspectionItemId);

    @Query("select distinct planItem.planId from QualityInspectionPlanItemEntity planItem "
            + "where planItem.inspectionItemId = :inspectionItemId")
    List<Long> findDistinctPlanIdsByInspectionItemId(@Param("inspectionItemId") Long inspectionItemId);

    List<QualityInspectionPlanItemEntity> findByPlanIdOrderBySortOrderAscIdAsc(Long planId);

    long countByPlanId(Long planId);

    void deleteByPlanId(Long planId);
}
