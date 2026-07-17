package com.badminton.mes.module.integration.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.integration.dal.entity.EquipmentBindingEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 设备报工绑定配置 Repository。
 *
 * <p>绑定以规范化设备编码为业务键，正常读取固定排除逻辑删除记录。保存和启停校验由 Service
 * 在事务内完成，本接口只提供按设备编码读取当前有效配置的持久化能力。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
public interface EquipmentBindingRepository extends JpaRepository<EquipmentBindingEntity, Long> {

    /** 按设备编码读取未逻辑删除的绑定配置，供设备计数校验和管理端 upsert 使用。 */
    Optional<EquipmentBindingEntity> findByEquipmentCodeAndDeletedFalse(String equipmentCode);
}
