package com.badminton.mes.module.scene.service;

import com.badminton.mes.module.scene.dal.entity.SceneCompletionOrderEntity;
import com.badminton.mes.module.scene.dal.entity.SceneCompletionSyncRecordEntity;

/**
 * 完工外部同步结果的原子持久化服务。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
public interface CompletionSyncResultService {

    /**
     * 原子保存同步记录和完工单同步状态。
     *
     * @param order 完工单
     * @param record 同步记录
     * @param status 同步状态
     * @param errorSummary 错误摘要，成功时为空
     */
    void saveResult(SceneCompletionOrderEntity order, SceneCompletionSyncRecordEntity record,
                    int status, String errorSummary);
}
