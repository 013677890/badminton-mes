package com.badminton.mes.module.device.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.device.dal.entity.DeviceAccessConfigEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/** 设备接入配置 Repository。 */
public interface DeviceAccessConfigRepository extends JpaRepository<DeviceAccessConfigEntity, Long>,
        JpaSpecificationExecutor<DeviceAccessConfigEntity> {

    Optional<DeviceAccessConfigEntity> findByIdAndDeletedFalse(Long id);

    Optional<DeviceAccessConfigEntity> findByConfigCodeAndDeletedFalse(String configCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select config from DeviceAccessConfigEntity config "
            + "where config.id = :id and config.deleted = false")
    Optional<DeviceAccessConfigEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select config from DeviceAccessConfigEntity config "
            + "where config.configCode = :configCode and config.deleted = false")
    Optional<DeviceAccessConfigEntity> findByConfigCodeAndDeletedFalseForUpdate(
            @Param("configCode") String configCode);

    boolean existsByConfigCodeAndDeletedFalse(String configCode);

    boolean existsByConfigCodeAndIdNotAndDeletedFalse(String configCode, Long id);

    boolean existsByConfigCode(String configCode);

    boolean existsByEquipmentIdAndCollectionPointCodeAndDeletedFalse(Long equipmentId, String collectionPointCode);

    boolean existsByEquipmentIdAndCollectionPointCodeAndIdNotAndDeletedFalse(
            Long equipmentId, String collectionPointCode, Long id);
}
