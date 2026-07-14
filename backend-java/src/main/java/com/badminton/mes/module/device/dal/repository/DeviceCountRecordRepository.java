package com.badminton.mes.module.device.dal.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import com.badminton.mes.module.device.dal.entity.DeviceCountRecordEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** 设备计数记录 Repository。 */
public interface DeviceCountRecordRepository extends JpaRepository<DeviceCountRecordEntity, Long>,
        JpaSpecificationExecutor<DeviceCountRecordEntity> {

    Optional<DeviceCountRecordEntity> findById(Long id);

    boolean existsByDeduplicationKey(String deduplicationKey);

    long countByAccessConfigId(Long accessConfigId);

    Optional<DeviceCountRecordEntity>
            findTopByAccessConfigIdAndCollectedAtLessThanOrderByCollectedAtDescIdDesc(
                    Long accessConfigId, LocalDateTime collectedAt);
}
