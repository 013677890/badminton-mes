package com.badminton.mes.module.scene.service.impl;

import java.time.LocalDateTime;

import com.badminton.mes.module.scene.dal.entity.SceneCompletionOrderEntity;
import com.badminton.mes.module.scene.dal.entity.SceneCompletionSyncRecordEntity;
import com.badminton.mes.module.scene.dal.repository.SceneCompletionOrderRepository;
import com.badminton.mes.module.scene.dal.repository.SceneCompletionSyncRecordRepository;
import com.badminton.mes.module.scene.service.CompletionSyncResultService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 完工同步结果事务实现。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
@Service
public class CompletionSyncResultServiceImpl implements CompletionSyncResultService {

    private final SceneCompletionOrderRepository orderRepository;
    private final SceneCompletionSyncRecordRepository syncRecordRepository;

    public CompletionSyncResultServiceImpl(SceneCompletionOrderRepository orderRepository,
                                           SceneCompletionSyncRecordRepository syncRecordRepository) {
        this.orderRepository = orderRepository;
        this.syncRecordRepository = syncRecordRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveResult(SceneCompletionOrderEntity order, SceneCompletionSyncRecordEntity record,
                           int status, String errorSummary) {
        record.setRetryCount(record.getRetryCount() + 1);
        record.setSyncStatus(status);
        record.setErrorSummary(errorSummary);
        record.setLastSyncTime(LocalDateTime.now());
        syncRecordRepository.save(record);
        order.setSyncStatus(status);
        orderRepository.save(order);
    }
}
