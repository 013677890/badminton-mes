package com.badminton.mes.module.equipment.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.equipment.dal.entity.EquipmentLedgerEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 设备台账 JPA Repository。
 *
 * @author 角色C
 * @date 2026/07/09
 */
public interface EquipmentLedgerRepository extends JpaRepository<EquipmentLedgerEntity, Long>,
        JpaSpecificationExecutor<EquipmentLedgerEntity> {

    /**
     * 按主键查询未删除的设备台账。
     *
     * @param id 设备主键
     * @return 设备台账实体
     */
    Optional<EquipmentLedgerEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 按主键查询未删除的设备台账，并对记录加写锁。
     *
     * @param id 设备主键
     * @return 设备台账实体
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ledger from EquipmentLedgerEntity ledger where ledger.id = :id and ledger.deleted = false")
    Optional<EquipmentLedgerEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    /**
     * 判断未删除设备中是否已存在指定设备编码。
     *
     * @param equipmentCode 设备编码
     * @return true 存在，false 不存在
     */
    boolean existsByEquipmentCodeAndDeletedFalse(String equipmentCode);

    /**
     * 判断未删除设备中是否已存在指定设备编码（排除指定 id）。
     *
     * @param equipmentCode 设备编码
     * @param id            排除的设备 id
     * @return true 存在，false 不存在
     */
    boolean existsByEquipmentCodeAndIdNotAndDeletedFalse(String equipmentCode, Long id);

    /**
     * 统计指定设备类别下的未删除设备数量。
     *
     * @param categoryId 设备类别 id
     * @return 设备数量
     */
    long countByCategoryIdAndDeletedFalse(Long categoryId);

    /**
     * 统计指定设备制造商下的未删除设备数量。
     *
     * @param manufacturerId 设备制造商 id
     * @return 设备数量
     */
    long countByManufacturerIdAndDeletedFalse(Long manufacturerId);
}
