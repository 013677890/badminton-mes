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
    Optional<DeviceCountRecordEntity>
            findFirstBySourceSystemAndEquipmentCodeAndDispatchOrderIdAndProcessIdAndDeletedFalseOrderByIdDesc(
                    String sourceSystem, String equipmentCode, Long dispatchOrderId, Long processId);
}
