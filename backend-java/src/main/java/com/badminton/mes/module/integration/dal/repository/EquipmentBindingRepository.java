package com.badminton.mes.module.integration.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.integration.dal.entity.EquipmentBindingEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 设备报工绑定配置 Repository。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
public interface EquipmentBindingRepository extends JpaRepository<EquipmentBindingEntity, Long> {

    Optional<EquipmentBindingEntity> findByEquipmentCodeAndDeletedFalse(String equipmentCode);
}
