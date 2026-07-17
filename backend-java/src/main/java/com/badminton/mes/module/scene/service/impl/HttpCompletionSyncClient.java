package com.badminton.mes.module.scene.service.impl;

import com.badminton.mes.module.scene.dal.entity.SceneCompletionOrderEntity;
import com.badminton.mes.module.scene.service.CompletionSyncClient;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP JSON 完工同步客户端。
 *
 * <p>{@link ConditionalOnProperty} 表示只有配置了同步 URL 才创建该 Bean；构造器中的
 * {@code connectTimeout}/{@code readTimeout} 防止外部 ERP 不可用时长期占用业务线程，
 * {@code Idempotency-Key} 请求头与完工单同步记录共同保证重试安全。
 *
 * @author 刘涵
 * @date 2026/07/16
 */
@Component
@ConditionalOnProperty(name = "mes.scene.completion-sync.url")
public class HttpCompletionSyncClient implements CompletionSyncClient {
    private final RestClient restClient;
    private final String url;

    public HttpCompletionSyncClient(
            @Value("${mes.scene.completion-sync.url}") String url,
            @Value("${mes.scene.completion-sync.connect-timeout:15s}") Duration connectTimeout,
            @Value("${mes.scene.completion-sync.read-timeout:30s}") Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
        this.url = url;
    }

    /** 将完工单序列化为 JSON POST 请求发送到配置的同步地址。 */
    @Override
    public void sync(SceneCompletionOrderEntity order, String targetSystem, String idempotencyKey) {
        restClient.post().uri(url)
                .header("Idempotency-Key", idempotencyKey)
                .body(Map.of(
                        "finishNo", order.getFinishNo(),
                        "taskId", order.getTaskId(),
                        "workOrderId", order.getWorkOrderId(),
                        "productId", order.getProductId(),
                        "batchNo", order.getBatchNo(),
                        "finishQuantity", order.getFinishQuantity(),
                        "targetSystem", targetSystem))
                .retrieve().toBodilessEntity();
    }
}
