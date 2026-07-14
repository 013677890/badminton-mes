package com.badminton.mes.module.equipment.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.equipment.dal.entity.EquipmentManufacturerEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 设备制造商 JPA Repository。
 *
 * @author 角色C
 * @date 2026/07/09
 */
public interface EquipmentManufacturerRepository extends JpaRepository<EquipmentManufacturerEntity, Long>,
        JpaSpecificationExecutor<EquipmentManufacturerEntity> {

    /**
     * 按主键查询未删除的设备制造商。
     *
     * @param id 制造商主键
     * @return 制造商实体
     */
    Optional<EquipmentManufacturerEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 判断未删除制造商中是否已存在指定制造商编码。
     *
     * @param manufacturerCode 制造商编码
     * @return true 存在，false 不存在
     */
    boolean existsByManufacturerCodeAndDeletedFalse(String manufacturerCode);

    /**
     * 判断未删除制造商中是否已存在指定制造商编码（排除指定 id）。
     *
     * @param manufacturerCode 制造商编码
     * @param id               排除的制造商 id
     * @return true 存在，false 不存在
     */
    boolean existsByManufacturerCodeAndIdNotAndDeletedFalse(String manufacturerCode, Long id);
}
