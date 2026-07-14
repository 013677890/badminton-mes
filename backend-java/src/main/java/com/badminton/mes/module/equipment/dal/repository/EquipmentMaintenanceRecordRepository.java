package com.badminton.mes.module.equipment.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.equipment.dal.entity.EquipmentMaintenanceRecordEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 设备保养记录 JPA Repository。
 *
 * @author 角色C
 * @date 2026/07/11
 */
public interface EquipmentMaintenanceRecordRepository extends JpaRepository<EquipmentMaintenanceRecordEntity, Long>,
        JpaSpecificationExecutor<EquipmentMaintenanceRecordEntity> {

    Optional<EquipmentMaintenanceRecordEntity> findByIdAndDeletedFalse(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select record from EquipmentMaintenanceRecordEntity record where record.id = :id and record.deleted = false")
    Optional<EquipmentMaintenanceRecordEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    boolean existsByRecordNoAndDeletedFalse(String recordNo);

    boolean existsByRecordNoAndIdNotAndDeletedFalse(String recordNo, Long id);

    boolean existsByRecordNo(String recordNo);

    long countByPlanIdAndDeletedFalse(Long planId);

    long countByPlanIdAndRecordStatusInAndDeletedFalse(Long planId, Iterable<String> recordStatuses);

    long countByEquipmentIdAndRecordStatusInAndDeletedFalse(Long equipmentId, Iterable<String> recordStatuses);

    long countByEquipmentIdAndDeletedFalse(Long equipmentId);

    long countByEquipmentIdAndRecordStatusAndIdNotAndDeletedFalse(
            Long equipmentId, String recordStatus, Long id);

    @Query("select max(record.finishTime) from EquipmentMaintenanceRecordEntity record "
            + "where record.planId = :planId and record.recordStatus = 'COMPLETED' and record.deleted = false")
    Optional<java.time.LocalDateTime> findLatestCompletedTimeByPlanId(@Param("planId") Long planId);
}
