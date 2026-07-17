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
 * <p>仅在 {@code mes.erp.mode=remote} 时装配。连接与读取超时由配置注入，任务和工艺分别使用
 * 独立路径但共享基础地址及 HTTP 客户端；空响应统一转换为空集合，网络和协议异常统一翻译为
 * ERP 远程调用业务错误，避免上层批处理依赖具体 HTTP 客户端异常类型。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Component
@ConditionalOnProperty(name = "mes.erp.mode", havingValue = "remote")
public class ErpRemoteClient implements ErpDataSource {

    /** 记录远程路径和完整异常堆栈，供接口故障排查。 */
    private static final Logger logger = LoggerFactory.getLogger(ErpRemoteClient.class);

    /** 已配置基础地址和超时策略的同步 HTTP 客户端。 */
    private final RestClient restClient;

    /** ERP 生产任务 JSON 数组读取路径。 */
    private final String taskPath;

    /** ERP 工艺路线 JSON 数组读取路径。 */
    private final String craftPath;

    /**
     * 根据配置构造 ERP HTTP 客户端。
     *
     * @param baseUrl ERP 服务基础地址
     * @param taskPath 任务读取路径
     * @param craftPath 工艺读取路径
     * @param connectTimeout TCP 连接超时
     * @param readTimeout 响应读取超时
     */
    public ErpRemoteClient(
            @Value("${mes.erp.base-url}") String baseUrl,
            @Value("${mes.erp.task-path:/api/mes/tasks}") String taskPath,
            @Value("${mes.erp.craft-path:/api/mes/crafts}") String craftPath,
            @Value("${mes.erp.connect-timeout:3s}") Duration connectTimeout,
            @Value("${mes.erp.read-timeout:10s}") Duration readTimeout) {
        // 将配置化超时写入请求工厂，避免 ERP 不可用时同步线程无限阻塞。
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

    /** 执行 GET 并按带泛型信息的 JSON 数组类型反序列化响应。 */
    private <T> List<T> getList(String path, ParameterizedTypeReference<List<T>> type) {
        try {
            List<T> result = restClient.get().uri(path).retrieve().body(type);
            // 204 或空响应体按没有待同步数据处理，避免向上层返回 null。
            return result == null ? List.of() : result;
        } catch (RestClientException exception) {
            // 日志保留底层网络细节，对外只暴露稳定的集成模块错误码。
            logger.error("[ERP remote call failed] path: {}, message: {}",
                    path, exception.getMessage(), exception);
            throw new ServiceException(IntegrationErrorCodeConstants.ERP_REMOTE_CALL_FAILED);
        }
    }
}
