package com.badminton.mes.module.scene.service.impl;

import com.badminton.mes.module.scene.dal.entity.SceneCompletionOrderEntity;
import com.badminton.mes.module.scene.dal.entity.SceneCompletionSyncRecordEntity;
import com.badminton.mes.module.scene.dal.repository.SceneCompletionOrderRepository;
import com.badminton.mes.module.scene.dal.repository.SceneCompletionSyncRecordRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 完工同步结果原子写入业务测试。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
class CompletionSyncResultServiceImplTest {

    @Test
    void savesRecordAndOrderWithSameStatus() {
        SceneCompletionOrderRepository orderRepository = mock(SceneCompletionOrderRepository.class);
        SceneCompletionSyncRecordRepository recordRepository =
                mock(SceneCompletionSyncRecordRepository.class);
        CompletionSyncResultServiceImpl service =
                new CompletionSyncResultServiceImpl(orderRepository, recordRepository);
        SceneCompletionOrderEntity order = new SceneCompletionOrderEntity();
        SceneCompletionSyncRecordEntity record = new SceneCompletionSyncRecordEntity();
        record.setRetryCount(1);

        service.saveResult(order, record, 2, "timeout");

        assertThat(order.getSyncStatus()).isEqualTo(2);
        assertThat(record.getSyncStatus()).isEqualTo(2);
        assertThat(record.getRetryCount()).isEqualTo(2);
        assertThat(record.getErrorSummary()).isEqualTo("timeout");
        verify(recordRepository).save(record);
        verify(orderRepository).save(order);
    }
}
