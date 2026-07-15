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

/** HTTP JSON 完工同步客户端，使用稳定幂等请求头。 @author 刘涵 */
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
