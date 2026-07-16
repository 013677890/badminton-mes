package com.badminton.mes.module.integration.service;

import java.time.Duration;
import java.util.List;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.integration.constants.IntegrationErrorCodeConstants;
import com.badminton.mes.module.integration.service.dto.ErpCraftDTO;
import com.badminton.mes.module.integration.service.dto.ErpTaskDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * ERP HTTP 客户端，协议约定为 JSON 数组读取接口。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Component
@ConditionalOnProperty(name = "mes.erp.mode", havingValue = "remote")
public class ErpRemoteClient implements ErpDataSource {

    private static final Logger logger = LoggerFactory.getLogger(ErpRemoteClient.class);

    private final RestClient restClient;
    private final String taskPath;
    private final String craftPath;

    public ErpRemoteClient(
            @Value("${mes.erp.base-url}") String baseUrl,
            @Value("${mes.erp.task-path:/api/mes/tasks}") String taskPath,
            @Value("${mes.erp.craft-path:/api/mes/crafts}") String craftPath,
            @Value("${mes.erp.connect-timeout:3s}") Duration connectTimeout,
            @Value("${mes.erp.read-timeout:10s}") Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.taskPath = taskPath;
        this.craftPath = craftPath;
    }

    /** 读取 ERP 生产任务。 */
    @Override
    public List<ErpTaskDTO> fetchTasks() {
        return getList(taskPath, new ParameterizedTypeReference<>() {
        });
    }

    /** 读取 ERP 工艺路线。 */
    @Override
    public List<ErpCraftDTO> fetchCrafts() {
        return getList(craftPath, new ParameterizedTypeReference<>() {
        });
    }

    private <T> List<T> getList(String path, ParameterizedTypeReference<List<T>> type) {
        try {
            List<T> result = restClient.get().uri(path).retrieve().body(type);
            return result == null ? List.of() : result;
        } catch (RestClientException exception) {
            logger.error("[ERP remote call failed] path: {}, message: {}",
                    path, exception.getMessage(), exception);
            throw new ServiceException(IntegrationErrorCodeConstants.ERP_REMOTE_CALL_FAILED);
        }
    }
}
