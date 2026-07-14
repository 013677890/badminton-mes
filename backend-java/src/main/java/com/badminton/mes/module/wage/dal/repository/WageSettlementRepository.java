package com.badminton.mes.module.wage.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.wage.dal.entity.WageSettlementEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/** 计件工资结算批次 Repository。 */
public interface WageSettlementRepository extends JpaRepository<WageSettlementEntity, Long>,
        JpaSpecificationExecutor<WageSettlementEntity> {

    /** 按主键查询未删除结算。 */
    Optional<WageSettlementEntity> findByIdAndDeletedFalse(Long id);

    /** 按主键写锁查询未删除结算。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT settlement FROM WageSettlementEntity settlement "
            + "WHERE settlement.id = :id AND settlement.deleted = false")
    Optional<WageSettlementEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);
}
