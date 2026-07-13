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
    Optional<ErpCraftPendingEntity> findByIdForUpdate(@Param("id") Long id);
}
