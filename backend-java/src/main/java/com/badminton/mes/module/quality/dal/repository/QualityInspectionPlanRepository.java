package com.badminton.mes.module.quality.dal.repository;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.quality.dal.entity.QualityInspectionPlanEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/** 检验标准方案 Repository。 */
public interface QualityInspectionPlanRepository extends JpaRepository<QualityInspectionPlanEntity, Long>,
        JpaSpecificationExecutor<QualityInspectionPlanEntity> {

    Optional<QualityInspectionPlanEntity> findByIdAndDeletedFalse(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select plan from QualityInspectionPlanEntity plan "
            + "where plan.id = :id and plan.deleted = false")
    Optional<QualityInspectionPlanEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    boolean existsByPlanCodeAndVersionNoAndDeletedFalse(String planCode, Integer versionNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select plan from QualityInspectionPlanEntity plan "
            + "where plan.planCode = :planCode and plan.deleted = false order by plan.versionNo")
    List<QualityInspectionPlanEntity> lockPlansByPlanCode(@Param("planCode") String planCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select plan from QualityInspectionPlanEntity plan "
            + "where plan.inspectionType = :inspectionType and plan.deleted = false order by plan.id")
    List<QualityInspectionPlanEntity> lockPlansByInspectionType(
            @Param("inspectionType") String inspectionType);

    @Query("select count(plan) > 0 from QualityInspectionPlanEntity plan "
            + "where plan.deleted = false and plan.planStatus = 'EFFECTIVE' and plan.defaultFlag = true "
            + "and ((:productId is null and plan.productId is null) or plan.productId = :productId) "
            + "and ((:customerId is null and plan.customerId is null) or plan.customerId = :customerId) "
            + "and plan.inspectionType = :inspectionType and plan.id <> :excludedId")
    boolean existsEffectiveDefaultForScope(@Param("productId") Long productId,
                                           @Param("customerId") Long customerId,
                                           @Param("inspectionType") String inspectionType,
                                           @Param("excludedId") Long excludedId);
}
