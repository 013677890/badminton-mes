package com.badminton.mes.module.wage.dal.repository;

import com.badminton.mes.module.wage.dal.entity.WageRuleChangeLogEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** 计件规则变更日志 Repository。 */
public interface WageRuleChangeLogRepository extends JpaRepository<WageRuleChangeLogEntity, Long> {
    /** 统计规则日志。 */
    long countByRuleIdAndDeletedFalse(Long ruleId);

    /** 分页查询规则日志。 */
    Page<WageRuleChangeLogEntity> findByRuleIdAndDeletedFalse(Long ruleId, Pageable pageable);
}
