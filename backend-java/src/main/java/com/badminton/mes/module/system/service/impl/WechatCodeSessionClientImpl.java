package com.badminton.mes.module.system.service.impl;

import java.time.Duration;
import java.util.Map;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.system.constants.SystemErrorCodeConstants;
import com.badminton.mes.module.system.service.WechatCodeSessionClient;
import com.badminton.mes.module.system.service.dto.WechatCodeSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 微信小程序 code2Session HTTP 客户端。
 *
 * @author Codex
 * @date 2026/07/15
 */
@Component
public class WechatCodeSessionClientImpl implements WechatCodeSessionClient {

    private final RestClient restClient;

    private final String appId;

    private final String appSecret;

    /**
     * 构造微信客户端。
     *
     * @param codeSessionUrl 微信接口地址
     * @param appId 小程序 AppID
     * @param appSecret 小程序密钥
     * @param connectTimeout 连接超时
     * @param readTimeout 读取超时
     */
    public WechatCodeSessionClientImpl(
            @Value("${mes.wechat.mini-app.code-session-url:https://api.weixin.qq.com/sns/jscode2session}")
            String codeSessionUrl,
            @Value("${mes.wechat.mini-app.app-id:}") String appId,
            @Value("${mes.wechat.mini-app.app-secret:}") String appSecret,
            @Value("${mes.wechat.mini-app.connect-timeout:3s}") Duration connectTimeout,
            @Value("${mes.wechat.mini-app.read-timeout:5s}") Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = RestClient.builder().baseUrl(codeSessionUrl).requestFactory(requestFactory).build();
        this.appId = appId;
        this.appSecret = appSecret;
    }

    @Override
    public WechatCodeSession exchange(String code) {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)) {
            throw new ServiceException(SystemErrorCodeConstants.WECHAT_CONFIG_MISSING);
        }
        try {
            Map<?, ?> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("appid", appId)
                            .queryParam("secret", appSecret)
                            .queryParam("js_code", code)
                            .queryParam("grant_type", "authorization_code")
                            .build())
                    .retrieve()
                    .body(Map.class);
            if (response == null || response.containsKey("errcode")) {
                throw new ServiceException(SystemErrorCodeConstants.WECHAT_CODE_INVALID);
            }
            Object openId = response.get("openid");
            if (!(openId instanceof String value) || !StringUtils.hasText(value)) {
                throw new ServiceException(SystemErrorCodeConstants.WECHAT_CODE_INVALID);
            }
            return new WechatCodeSession(value);
        } catch (ServiceException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ServiceException(SystemErrorCodeConstants.WECHAT_SERVICE_UNAVAILABLE);
        }
    }
}
