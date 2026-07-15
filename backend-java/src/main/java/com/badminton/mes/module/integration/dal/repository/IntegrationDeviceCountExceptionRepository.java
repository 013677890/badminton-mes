package com.badminton.mes.module.integration.dal.repository;

import com.badminton.mes.module.integration.dal.entity.IntegrationDeviceCountExceptionEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

/**
 * 设备计数异常池 Repository。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Repository("integrationDeviceCountExceptionRepository")
public interface DeviceCountExceptionRepository
        extends JpaRepository<DeviceCountExceptionEntity, Long>,
        JpaSpecificationExecutor<DeviceCountExceptionEntity> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT exception FROM IntegrationDeviceCountExceptionEntity exception
            WHERE exception.id = :id AND exception.deleted = false
            """)
    java.util.Optional<IntegrationDeviceCountExceptionEntity> findByIdForUpdate(@Param("id") Long id);
}
