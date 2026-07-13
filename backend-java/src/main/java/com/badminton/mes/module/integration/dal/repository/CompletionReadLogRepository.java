package com.badminton.mes.module.integration.dal.repository;

import com.badminton.mes.module.integration.dal.entity.CompletionReadLogEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 生产完工单读取日志 Repository。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
public interface CompletionReadLogRepository extends JpaRepository<CompletionReadLogEntity, Long>,
        JpaSpecificationExecutor<CompletionReadLogEntity> {
}
