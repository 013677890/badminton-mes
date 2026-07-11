package com.badminton.mes.module.equipment.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.equipment.dal.entity.EquipmentRepairOrderEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 设备报修任务 JPA Repository。
 *
 * @author 角色C
 * @date 2026/07/10
 */
public interface EquipmentRepairOrderRepository extends JpaRepository<EquipmentRepairOrderEntity, Long>,
        JpaSpecificationExecutor<EquipmentRepairOrderEntity> {

    /**
     * 按主键查询未删除的报修任务。
     *
     * @param id 报修任务主键
     * @return 报修任务实体
     */
    Optional<EquipmentRepairOrderEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 判断未删除报修任务中是否已存在指定报修单号。
     *
     * @param repairNo 报修单号
     * @return true 存在，false 不存在
     */
    boolean existsByRepairNoAndDeletedFalse(String repairNo);

    /**
     * 判断未删除报修任务中是否已存在指定报修单号（排除指定 id）。
     *
     * @param repairNo 报修单号
     * @param id       排除的报修任务 id
     * @return true 存在，false 不存在
     */
    boolean existsByRepairNoAndIdNotAndDeletedFalse(String repairNo, Long id);

    /**
     * 统计指定设备下的未删除报修任务数量。
     *
     * @param equipmentId 设备台账 id
     * @return 报修任务数量
     */
    long countByEquipmentIdAndDeletedFalse(Long equipmentId);

    /**
     * 统计指定故障原理下的未删除报修任务数量。
     *
     * @param faultPrincipleId 故障原理 id
     * @return 报修任务数量
     */
    long countByFaultPrincipleIdAndDeletedFalse(Long faultPrincipleId);
}
