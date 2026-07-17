package com.badminton.mes.module.system.service.impl;

import java.time.Duration;
import java.util.Map;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.system.constants.SystemErrorCodeConstants;
import com.badminton.mes.module.system.dal.redis.WechatAccessTokenRedisDAO;
import com.badminton.mes.module.system.service.WechatMiniAppCodeClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import tools.jackson.databind.ObjectMapper;

/**
 * 微信 access_token 与 getwxacodeunlimit 客户端。
 *
 * @author Codex
 * @date 2026/07/16
 */
@Component
public class WechatMiniAppCodeClientImpl implements WechatMiniAppCodeClient {

    private static final int ACCESS_TOKEN_SAFETY_SECONDS = 300;

    private static final int MIN_ACCESS_TOKEN_TTL_SECONDS = 60;

    private static final int CODE_WIDTH = 430;

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
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
            @Value("${mes.wechat.mini-app.connect-timeout:3s}") Duration connectTimeout,
            @Value("${mes.wechat.mini-app.read-timeout:5s}") Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
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
        if (isPng(response)) {
            return response;
        }
        if (isAccessTokenInvalid(response)) {
            accessTokenRedisDAO.remove(appId);
            response = requestCode(ticket, getAccessToken());
            if (isPng(response)) {
                return response;
            }
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
            if (response == null || response.containsKey("errcode")) {
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
            throw new ServiceException(SystemErrorCodeConstants.WECHAT_SERVICE_UNAVAILABLE);
        }
    }

    private byte[] requestCode(String ticket, String accessToken) {
        try {
            byte[] response = codeClient.post()
                    .uri(uriBuilder -> uriBuilder.queryParam("access_token", accessToken).build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.IMAGE_PNG, MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "scene", ticket,
                            "page", bindPage,
                            "check_path", false,
                            "env_version", codeEnvVersion,
                            "width", CODE_WIDTH))
                    .retrieve()
                    .body(byte[].class);
            return response == null ? new byte[0] : response;
        } catch (RestClientException exception) {
            throw new ServiceException(SystemErrorCodeConstants.WECHAT_SERVICE_UNAVAILABLE);
        }
    }

    private boolean isPng(byte[] response) {
        if (response.length < PNG_SIGNATURE.length) {
            return false;
        }
        for (int index = 0; index < PNG_SIGNATURE.length; index++) {
            if (response[index] != PNG_SIGNATURE[index]) {
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

    private void validateConfiguration() {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)
                || !StringUtils.hasText(bindPage) || !StringUtils.hasText(codeEnvVersion)) {
            throw new ServiceException(SystemErrorCodeConstants.WECHAT_CONFIG_MISSING);
        }
    }
}
