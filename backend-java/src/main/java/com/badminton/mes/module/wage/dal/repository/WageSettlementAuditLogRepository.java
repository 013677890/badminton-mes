package com.badminton.mes.module.wage.dal.repository;

import com.badminton.mes.module.wage.dal.entity.WageSettlementAuditLogEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** 结算审计日志 Repository。 */
public interface WageSettlementAuditLogRepository
        extends JpaRepository<WageSettlementAuditLogEntity, Long> {
    /** 统计结算审计日志。 */
    long countBySettlementIdAndDeletedFalse(Long settlementId);
    /** 分页查询结算审计日志。 */
    Page<WageSettlementAuditLogEntity> findBySettlementIdAndDeletedFalse(
            Long settlementId, Pageable pageable);
}
