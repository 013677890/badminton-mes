package com.badminton.mes.module.equipment.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.equipment.dal.entity.EquipmentMaintenancePlanEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 设备保养计划 JPA Repository。
 *
 * @author 角色C
 * @date 2026/07/11
 */
public interface EquipmentMaintenancePlanRepository extends JpaRepository<EquipmentMaintenancePlanEntity, Long>,
        JpaSpecificationExecutor<EquipmentMaintenancePlanEntity> {

    Optional<EquipmentMaintenancePlanEntity> findByIdAndDeletedFalse(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select plan from EquipmentMaintenancePlanEntity plan where plan.id = :id and plan.deleted = false")
    Optional<EquipmentMaintenancePlanEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    boolean existsByPlanCodeAndDeletedFalse(String planCode);

    boolean existsByPlanCodeAndIdNotAndDeletedFalse(String planCode, Long id);

    boolean existsByPlanCode(String planCode);

    long countByEquipmentIdAndStatusAndDeletedFalse(Long equipmentId, Integer status);

    long countByEquipmentIdAndDeletedFalse(Long equipmentId);
}
