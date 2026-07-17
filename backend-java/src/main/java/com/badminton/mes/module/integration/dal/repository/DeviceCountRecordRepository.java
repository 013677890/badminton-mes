package com.badminton.mes.module.integration.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.integration.dal.entity.DeviceCountRecordEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

/**
 * 设备计数成功记录 Repository。
 *
 * <p>成功记录保存外部累计值及本次增量，是后续计算增量和识别回退的事实基线。读取最近记录时
 * 申请悲观写锁，保证同一设备、派工单和工序维度不会有两个事务同时基于同一累计值计算。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Repository("integrationDeviceCountRecordRepository")
public interface DeviceCountRecordRepository extends JpaRepository<DeviceCountRecordEntity, Long> {

    /**
     * 查询相同来源、设备、派工单和工序维度的最近累计计数。
     *
     * @param sourceSystem   来源系统
     * @param equipmentCode 设备编码
     * @param dispatchOrderId 派工单主键
     * @param processId      工序主键
     * @return 最近一条未删除计数记录
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    /**
     * 查询同一来源、设备、派工单和工序维度的最近有效累计记录并加写锁。
     *
     * <p>按主键倒序取最新记录；逻辑删除历史不参与累计基线，锁保持到调用方事务结束。
     */
    Optional<DeviceCountRecordEntity>
            findFirstBySourceSystemAndEquipmentCodeAndDispatchOrderIdAndProcessIdAndDeletedFalseOrderByIdDesc(
                    String sourceSystem, String equipmentCode, Long dispatchOrderId, Long processId);
}
