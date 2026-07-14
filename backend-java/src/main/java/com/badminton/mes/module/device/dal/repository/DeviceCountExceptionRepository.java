package com.badminton.mes.module.device.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.device.dal.entity.DeviceCountExceptionEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/** 设备计数异常 Repository。 */
public interface DeviceCountExceptionRepository extends JpaRepository<DeviceCountExceptionEntity, Long>,
        JpaSpecificationExecutor<DeviceCountExceptionEntity> {

    Optional<DeviceCountExceptionEntity> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select exception from DeviceCountExceptionEntity exception where exception.id = :id")
    Optional<DeviceCountExceptionEntity> findByIdForUpdate(@Param("id") Long id);
}
