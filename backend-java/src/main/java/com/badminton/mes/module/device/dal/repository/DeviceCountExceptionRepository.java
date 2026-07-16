package com.badminton.mes.module.device.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.device.dal.entity.DeviceCountExceptionEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 设备计数异常 Repository。
 *
 * <p>详情展示使用普通无锁查询；异常处置使用悲观写锁读取，确保状态校验与处理结果写入在同一事务内串行完成。
 */
public interface DeviceCountExceptionRepository extends JpaRepository<DeviceCountExceptionEntity, Long>,
        JpaSpecificationExecutor<DeviceCountExceptionEntity> {

    /** 按主键无锁查询异常详情，不附加处理状态过滤，已处理记录仍可用于审计回看。 */
    Optional<DeviceCountExceptionEntity> findById(Long id);

    /**
     * 按主键查询异常并获取悲观写锁，不过滤处理状态。
     *
     * <p>用于异常处置事务中重新判断是否仍为待处理，再写入处理人、时间和结论，
     * 防止多人并发操作覆盖首次处置结果。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select exception from DeviceCountExceptionEntity exception where exception.id = :id")
    Optional<DeviceCountExceptionEntity> findByIdForUpdate(@Param("id") Long id);
}
