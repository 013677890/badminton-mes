package com.badminton.mes.module.scene.service;

import com.badminton.mes.module.scene.dal.entity.SceneCompletionOrderEntity;

/**
 * 可替换的完工外部同步端口。
 *
 * <p>{@link SceneCompletionOrderService} 只依赖此接口，因此本地联调可以注入模拟实现，
 * 生产环境再由 {@code HttpCompletionSyncClient} 通过 HTTP 调用 ERP；调用方提供幂等键，
 * 实现必须保证重复请求不会重复入账。
 *
 * @author 刘涵
 * @date 2026/07/16
 */
public interface CompletionSyncClient {

    /** 将审核通过的完工单发送到目标系统。 */
    void sync(SceneCompletionOrderEntity order, String targetSystem, String idempotencyKey);
}
