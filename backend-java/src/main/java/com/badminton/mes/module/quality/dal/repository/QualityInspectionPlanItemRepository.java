package com.badminton.mes.module.quality.dal.repository;

import java.util.List;

import com.badminton.mes.module.quality.dal.entity.QualityInspectionPlanItemEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/** 检验方案明细 Repository。 */
public interface QualityInspectionPlanItemRepository
        extends JpaRepository<QualityInspectionPlanItemEntity, Long> {

    boolean existsByInspectionItemId(Long inspectionItemId);

    List<QualityInspectionPlanItemEntity> findByPlanIdOrderBySortOrderAscIdAsc(Long planId);

    long countByPlanId(Long planId);

    void deleteByPlanId(Long planId);
}
