package com.badminton.mes.module.integration.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.integration.dal.entity.ErpCraftPendingEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * ERP 工艺待确认数据 Repository。
 *
 * <p>来源系统、ERP 路线编码和版本组成同步幂等键；查询保留各种处理状态，便于失败/驳回数据
 * 修正重试。确认和驳回使用悲观写锁，使同一待确认记录只能被一个事务推进状态。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
public interface ErpCraftPendingRepository extends JpaRepository<ErpCraftPendingEntity, Long>,
        JpaSpecificationExecutor<ErpCraftPendingEntity> {

    /**
     * 按来源系统、路线编码和版本查询待确认数据，用于幂等判断。
     *
     * @param sourceSystem     来源系统
     * @param erpRoutingCode   ERP 工艺路线编码
     * @param erpRoutingVersion ERP 工艺路线版本
     * @return 已存在的待确认数据
     */
    Optional<ErpCraftPendingEntity> findBySourceSystemAndErpRoutingCodeAndErpRoutingVersion(
            String sourceSystem, String erpRoutingCode, String erpRoutingVersion);

    /**
     * 以悲观写锁查询待确认数据，串行化同一记录的确认操作。
     *
     * @param id 待确认数据主键
     * @return 已锁定的待确认数据
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pending FROM ErpCraftPendingEntity pending WHERE pending.id = :id")
    /**
     * 按主键锁定待确认记录，锁保持至当前确认或驳回事务结束。
     *
     * <p>此查询不在 JPQL 中过滤状态，由 Service 在锁内判断是否仍为 PENDING，避免状态检查与
     * 加锁之间出现竞争窗口。
     */
    Optional<ErpCraftPendingEntity> findByIdForUpdate(@Param("id") Long id);
}
