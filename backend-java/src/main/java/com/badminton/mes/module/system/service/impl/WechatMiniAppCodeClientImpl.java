package com.badminton.mes.module.system.service.impl;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;
import java.util.Map;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.system.constants.SystemErrorCodeConstants;
import com.badminton.mes.module.system.dal.redis.WechatAccessTokenRedisDAO;
import com.badminton.mes.module.system.service.WechatMiniAppCodeClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import tools.jackson.databind.ObjectMapper;

/**
 * 微信 access_token 与 getwxacodeunlimit 客户端。
 *
 * @author Codex
 * @date 2026/07/16
 */
@Component
public class WechatMiniAppCodeClientImpl implements WechatMiniAppCodeClient {

    private static final Logger logger = LoggerFactory.getLogger(WechatMiniAppCodeClientImpl.class);

    private static final int ACCESS_TOKEN_SAFETY_SECONDS = 300;

    private static final int MIN_ACCESS_TOKEN_TTL_SECONDS = 60;

    private static final int CODE_WIDTH = 430;

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private static final byte[] JPEG_SIGNATURE = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
    };

    private final RestClient accessTokenClient;

    private final RestClient codeClient;

    private final WechatAccessTokenRedisDAO accessTokenRedisDAO;

    private final ObjectMapper objectMapper;

    private final String appId;

    private final String appSecret;

    private final String bindPage;

    private final String codeEnvVersion;

    private final Object accessTokenMonitor = new Object();

    public WechatMiniAppCodeClientImpl(
            WechatAccessTokenRedisDAO accessTokenRedisDAO,
            ObjectMapper objectMapper,
            @Value("${mes.wechat.mini-app.access-token-url:https://api.weixin.qq.com/cgi-bin/token}")
            String accessTokenUrl,
            @Value("${mes.wechat.mini-app.unlimited-code-url:https://api.weixin.qq.com/wxa/getwxacodeunlimit}")
            String unlimitedCodeUrl,
            @Value("${mes.wechat.mini-app.app-id:}") String appId,
            @Value("${mes.wechat.mini-app.app-secret:}") String appSecret,
            @Value("${mes.wechat.mini-app.bind-page:pages/wechat-bind-confirm/wechat-bind-confirm}")
            String bindPage,
            @Value("${mes.wechat.mini-app.code-env-version:release}") String codeEnvVersion,
            @Value("${mes.wechat.mini-app.proxy-host:}") String proxyHost,
            @Value("${mes.wechat.mini-app.proxy-port:0}") int proxyPort,
            @Value("${mes.wechat.mini-app.connect-timeout:3s}") Duration connectTimeout,
            @Value("${mes.wechat.mini-app.read-timeout:5s}") Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        configureProxy(requestFactory, proxyHost, proxyPort);
        this.accessTokenClient = RestClient.builder()
                .baseUrl(accessTokenUrl)
                .requestFactory(requestFactory)
                .build();
        this.codeClient = RestClient.builder()
                .baseUrl(unlimitedCodeUrl)
                .requestFactory(requestFactory)
                .build();
        this.accessTokenRedisDAO = accessTokenRedisDAO;
        this.objectMapper = objectMapper;
        this.appId = appId;
        this.appSecret = appSecret;
        this.bindPage = bindPage;
        this.codeEnvVersion = codeEnvVersion;
    }

    @Override
    public byte[] generateBindingCode(String ticket) {
        validateConfiguration();
        String accessToken = getAccessToken();
        byte[] response = requestCode(ticket, accessToken);
        if (isImage(response)) {
            return response;
        }
        logWechatError("getwxacodeunlimit", response);
        if (isAccessTokenInvalid(response)) {
            accessTokenRedisDAO.remove(appId);
            response = requestCode(ticket, getAccessToken());
            if (isImage(response)) {
                return response;
            }
            logWechatError("getwxacodeunlimit-retry", response);
        }
        throw new ServiceException(SystemErrorCodeConstants.WECHAT_SERVICE_UNAVAILABLE);
    }

    private String getAccessToken() {
        return accessTokenRedisDAO.find(appId).orElseGet(() -> {
            synchronized (accessTokenMonitor) {
                return accessTokenRedisDAO.find(appId).orElseGet(this::requestAccessToken);
            }
        });
    }

    private String requestAccessToken() {
        try {
            Map<?, ?> response = accessTokenClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("grant_type", "client_credential")
                            .queryParam("appid", appId)
                            .queryParam("secret", appSecret)
                            .build())
                    .retrieve()
                    .body(Map.class);
            if (response == null) {
                logger.warn("[微信 access_token 获取失败] 微信接口返回空响应");
                throw new ServiceException(SystemErrorCodeConstants.WECHAT_SERVICE_UNAVAILABLE);
            }
            if (response.containsKey("errcode")) {
                logWechatError("access_token", response);
                throw new ServiceException(SystemErrorCodeConstants.WECHAT_SERVICE_UNAVAILABLE);
            }
            Object tokenValue = response.get("access_token");
            Object expiresValue = response.get("expires_in");
            if (!(tokenValue instanceof String token) || !StringUtils.hasText(token)
                    || !(expiresValue instanceof Number expiresNumber)) {
                throw new ServiceException(SystemErrorCodeConstants.WECHAT_SERVICE_UNAVAILABLE);
            }
            long ttlSeconds = Math.max(MIN_ACCESS_TOKEN_TTL_SECONDS,
                    expiresNumber.longValue() - ACCESS_TOKEN_SAFETY_SECONDS);
            accessTokenRedisDAO.save(appId, token, Duration.ofSeconds(ttlSeconds));
            return token;
        } catch (ServiceException exception) {
            throw exception;
        } catch (RestClientException exception) {
            logger.warn("[微信 access_token 请求异常] exceptionType: {}",
                    exception.getClass().getSimpleName());
            throw new ServiceException(SystemErrorCodeConstants.WECHAT_SERVICE_UNAVAILABLE);
        }
    }

    private byte[] requestCode(String ticket, String accessToken) {
        byte[] requestBody;
        try {
            requestBody = objectMapper.writeValueAsBytes(Map.of(
                    "scene", ticket,
                    "page", bindPage,
                    "check_path", false,
                    "env_version", codeEnvVersion,
                    "width", CODE_WIDTH));
        } catch (RuntimeException exception) {
            logger.warn("[微信小程序码请求体序列化失败] exceptionType: {}",
                    exception.getClass().getSimpleName());
            throw new ServiceException(SystemErrorCodeConstants.WECHAT_SERVICE_UNAVAILABLE);
        }

        try {
            byte[] response = codeClient.post()
                    .uri(uriBuilder -> uriBuilder.queryParam("access_token", accessToken).build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.IMAGE_PNG, MediaType.APPLICATION_JSON)
                    .contentLength(requestBody.length)
                    .body(requestBody)
                    .retrieve()
                    .body(byte[].class);
            return response == null ? new byte[0] : response;
        } catch (RestClientResponseException exception) {
            byte[] responseBody = exception.getResponseBodyAsByteArray();
            if (responseBody.length > 0) {
                return responseBody;
            }
            logger.warn("[微信小程序码请求失败且响应体为空] httpStatus: {}",
                    exception.getStatusCode().value());
            throw new ServiceException(SystemErrorCodeConstants.WECHAT_SERVICE_UNAVAILABLE);
        } catch (RestClientException exception) {
            logger.warn("[微信小程序码请求异常] exceptionType: {}",
                    exception.getClass().getSimpleName());
            throw new ServiceException(SystemErrorCodeConstants.WECHAT_SERVICE_UNAVAILABLE);
        }
    }

    private void configureProxy(
            SimpleClientHttpRequestFactory requestFactory, String proxyHost, int proxyPort) {
        if (!StringUtils.hasText(proxyHost)) {
            return;
        }
        if (proxyPort <= 0 || proxyPort > 65535) {
            throw new IllegalArgumentException("微信 HTTP 代理端口必须在 1 到 65535 之间");
        }
        Proxy proxy = new Proxy(Proxy.Type.HTTP,
                InetSocketAddress.createUnresolved(proxyHost, proxyPort));
        requestFactory.setProxy(proxy);
        logger.info("[微信 HTTP 代理已启用] proxyHost: {}, proxyPort: {}", proxyHost, proxyPort);
    }

    private boolean isImage(byte[] response) {
        return hasSignature(response, PNG_SIGNATURE) || hasSignature(response, JPEG_SIGNATURE);
    }

    private boolean hasSignature(byte[] response, byte[] signature) {
        if (response.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (response[index] != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean isAccessTokenInvalid(byte[] response) {
        try {
            Map<?, ?> error = objectMapper.readValue(response, Map.class);
            Object errorCode = error.get("errcode");
            if (!(errorCode instanceof Number number)) {
                return false;
            }
            return number.intValue() == 40001
                    || number.intValue() == 40014
                    || number.intValue() == 42001;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private void logWechatError(String operation, byte[] response) {
        try {
            Map<?, ?> error = objectMapper.readValue(response, Map.class);
            logWechatError(operation, error);
        } catch (RuntimeException exception) {
            logger.warn("[微信接口返回非图片且无法解析] operation: {}, responseBytes: {}",
                    operation, response.length);
        }
    }

    private void logWechatError(String operation, Map<?, ?> error) {
        logger.warn("[微信接口调用失败] operation: {}, errcode: {}, errmsg: {}",
                operation, error.get("errcode"), error.get("errmsg"));
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)
                || !StringUtils.hasText(bindPage) || !StringUtils.hasText(codeEnvVersion)) {
            throw new ServiceException(SystemErrorCodeConstants.WECHAT_CONFIG_MISSING);
        }
    }
}
