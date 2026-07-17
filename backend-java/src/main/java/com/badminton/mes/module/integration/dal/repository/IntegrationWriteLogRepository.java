package com.badminton.mes.module.integration.dal.repository;

import com.badminton.mes.module.integration.dal.entity.IntegrationWriteLogEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.Optional;

/**
 * 外部接口写入日志 Repository。
 *
 * <p>日志既是审计事实，也是部分接口的幂等结果索引。成功/重复查询只返回可作为既有结果的状态；
 * 设备计数查询故意包含 FAILED，以便修正重试能够原位替换失败日志并保持外部幂等键稳定。
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

    /**
     * 按接口、来源和业务键查询最近处理结果，不区分成功或失败状态。
     *
     * <p>设备计数以任何已落日志为幂等处理完成，重复请求直接返回原结果引用。
     *
     * @param interfaceType 接口类型
     * @param sourceSystem  来源系统
     * @param businessKey   来源侧业务键
     * @return 最近一条处理日志
     */
    Optional<IntegrationWriteLogEntity>
            findFirstByInterfaceTypeAndSourceSystemAndBusinessKeyOrderByIdDesc(
                    String interfaceType, String sourceSystem, String businessKey);
}
