package com.badminton.mes.module.integration.dal.repository;

import com.badminton.mes.module.craft.dal.repository.CraftProcessRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 设备计数并发校验所需 Repository 锁契约测试。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
class DeviceCountRepositoryLockContractTest {

    @Test
    @DisplayName("设备计数：最近累计值查询使用悲观写锁获取当前已提交值")
    void recentCountQueryUsesPessimisticWriteLock() throws NoSuchMethodException {
        Lock lock = DeviceCountRecordRepository.class.getMethod(
                "findFirstBySourceSystemAndEquipmentCodeAndDispatchOrderIdAndProcessId"
                        + "AndDeletedFalseOrderByIdDesc",
                String.class, String.class, Long.class, Long.class)
                .getAnnotation(Lock.class);

        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    @DisplayName("设备计数：工序编码查询使用悲观写锁防止校验后并发失效")
    void processQueryUsesPessimisticWriteLock() throws NoSuchMethodException {
        Lock lock = CraftProcessRepository.class.getMethod(
                "findByProcessCodeAndDeletedFalseForUpdate", String.class)
                .getAnnotation(Lock.class);

        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }
}
