package com.badminton.mes.module.equipment.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.equipment.dal.entity.EquipmentFaultPrincipleEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 设备故障原理 JPA Repository。
 *
 * @author 角色C
 * @date 2026/07/10
 */
public interface EquipmentFaultPrincipleRepository extends JpaRepository<EquipmentFaultPrincipleEntity, Long>,
        JpaSpecificationExecutor<EquipmentFaultPrincipleEntity> {

    /**
     * 按主键查询未删除的故障原理。
     *
     * @param id 故障原理主键
     * @return 故障原理实体
     */
    Optional<EquipmentFaultPrincipleEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 按主键查询未删除的故障原理，并对记录加写锁。
     *
     * @param id 故障原理主键
     * @return 故障原理实体
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select faultPrinciple from EquipmentFaultPrincipleEntity faultPrinciple where faultPrinciple.id = :id and faultPrinciple.deleted = false")
    Optional<EquipmentFaultPrincipleEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    /**
     * 判断未删除故障原理中是否已存在指定故障编码。
     *
     * @param faultCode 故障编码
     * @return true 存在，false 不存在
     */
    boolean existsByFaultCodeAndDeletedFalse(String faultCode);

    /**
     * 判断未删除故障原理中是否已存在指定故障编码（排除指定 id）。
     *
     * @param faultCode 故障编码
     * @param id        排除的故障原理 id
     * @return true 存在，false 不存在
     */
    boolean existsByFaultCodeAndIdNotAndDeletedFalse(String faultCode, Long id);

    /**
     * 统计指定设备类别下的未删除故障原理数量。
     *
     * @param categoryId 设备类别 id
     * @return 故障原理数量
     */
    long countByCategoryIdAndDeletedFalse(Long categoryId);
}
