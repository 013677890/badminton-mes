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
 * <p>异常池是可人工处理的待办事实。处理动作必须先锁定未删除记录，再由 Service 判断处理状态，
 * 防止忽略和修正重试并发操作同一条异常。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Repository("integrationDeviceCountExceptionRepository")
public interface IntegrationDeviceCountExceptionRepository
        extends JpaRepository<IntegrationDeviceCountExceptionEntity, Long>,
        JpaSpecificationExecutor<IntegrationDeviceCountExceptionEntity> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT exception FROM IntegrationDeviceCountExceptionEntity exception
            WHERE exception.id = :id AND exception.deleted = false
            """)
    /**
     * 锁定未逻辑删除的异常池记录。
     *
     * <p>查询不在 SQL 中限定待处理状态，由业务层在锁内判断状态并返回稳定业务错误。
     */
    java.util.Optional<IntegrationDeviceCountExceptionEntity> findByIdForUpdate(@Param("id") Long id);
}
