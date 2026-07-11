package com.badminton.mes.module.integration.dal.repository;

import com.badminton.mes.module.integration.dal.entity.IntegrationWriteLogEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 外部接口写入日志 Repository。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
public interface IntegrationWriteLogRepository extends JpaRepository<IntegrationWriteLogEntity, Long>,
        JpaSpecificationExecutor<IntegrationWriteLogEntity> {
}
