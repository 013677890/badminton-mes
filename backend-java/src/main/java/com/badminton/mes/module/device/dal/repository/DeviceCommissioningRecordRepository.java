package com.badminton.mes.module.device.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.device.dal.entity.DeviceCommissioningRecordEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** 设备联调记录 Repository。 */
public interface DeviceCommissioningRecordRepository
        extends JpaRepository<DeviceCommissioningRecordEntity, Long>,
                JpaSpecificationExecutor<DeviceCommissioningRecordEntity> {

    Optional<DeviceCommissioningRecordEntity> findById(Long id);

    long countByAccessConfigId(Long accessConfigId);
}
