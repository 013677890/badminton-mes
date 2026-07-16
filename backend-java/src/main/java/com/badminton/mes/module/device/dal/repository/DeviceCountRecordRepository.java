package com.badminton.mes.module.device.dal.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import com.badminton.mes.module.device.dal.entity.DeviceCountRecordEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 设备计数记录 Repository。
 *
 * <p>计数事实查询均不加锁，写入幂等最终由数据库唯一约束兜底；前序累计记录查询只选择采集时间严格早于
 * 当前报文的数据，并按采集时间、主键倒序确定唯一最近基线。
 */
public interface DeviceCountRecordRepository extends JpaRepository<DeviceCountRecordEntity, Long>,
        JpaSpecificationExecutor<DeviceCountRecordEntity> {

    /** 按主键无锁查询计数原始记录，不附加业务状态过滤，用于详情和审计追溯。 */
    Optional<DeviceCountRecordEntity> findById(Long id);

    /** 按幂等键无锁判断报文是否已落库，用于写入前快速识别重复上报。 */
    boolean existsByDeduplicationKey(String deduplicationKey);

    /** 按接入配置统计全部计数历史，不加锁，用于禁止删除已有采集事实的配置。 */
    long countByAccessConfigId(Long accessConfigId);

    /**
     * 查询同一接入配置在指定采集时间之前最近的一条计数记录，不加锁。
     *
     * <p>{@code collectedAt < 当前时间} 排除当前及未来、同时间报文；先按采集时间倒序、再按主键倒序
     * 解决时间相同记录的确定性选择，用作累计计数模式计算本次差值的前序基线。
     */
    Optional<DeviceCountRecordEntity>
            findTopByAccessConfigIdAndCollectedAtLessThanOrderByCollectedAtDescIdDesc(
                    Long accessConfigId, LocalDateTime collectedAt);
}
