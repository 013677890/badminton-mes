package com.badminton.mes.module.integration.dal.repository;

import com.badminton.mes.module.integration.dal.entity.IntegrationWriteLogEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.Optional;

/**
 * 外部接口写入日志 Repository。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
public interface IntegrationWriteLogRepository extends JpaRepository<IntegrationWriteLogEntity, Long>,
        JpaSpecificationExecutor<IntegrationWriteLogEntity> {

    /**
     * 按接口类型、来源系统和业务键查询最近一条成功或重复日志，
     * 用于派工单写入幂等判断（派工单表无来源跟踪列，以日志为准）。
     *
     * @param interfaceType 接口类型
     * @param sourceSystem  来源系统
     * @param businessKey   来源侧业务键
     * @param writeStatuses 写入状态集合（成功+重复）
     * @return 最近一条匹配日志，无匹配时为空
     */
    Optional<IntegrationWriteLogEntity> findFirstByInterfaceTypeAndSourceSystemAndBusinessKeyAndWriteStatusInOrderByIdDesc(
            String interfaceType, String sourceSystem, String businessKey,
            Collection<Integer> writeStatuses);
}
