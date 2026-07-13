package com.badminton.mes.module.integration.dal.repository;

import com.badminton.mes.module.integration.dal.entity.DeviceCountExceptionEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 设备计数异常池 Repository。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
public interface DeviceCountExceptionRepository
        extends JpaRepository<DeviceCountExceptionEntity, Long>,
        JpaSpecificationExecutor<DeviceCountExceptionEntity> {
}
