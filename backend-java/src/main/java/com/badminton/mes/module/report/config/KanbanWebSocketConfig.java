package com.badminton.mes.module.report.config;

import com.badminton.mes.common.security.LoginSessionReader;
import com.badminton.mes.common.security.LoginUser;
import java.security.Principal;
import java.util.Map;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * 看板 STOMP WebSocket 配置及订阅鉴权。
 *
 * @author 刘涵
 */
@Configuration
@EnableWebSocketMessageBroker
public class KanbanWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String LOGIN_USER_ATTRIBUTE = "kanbanLoginUser";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String KANBAN_TOPIC_PREFIX = "/topic/report/kanban";
    private static final String MINI_APP_TOPIC_PREFIX = "/topic/report/mini_app";
    private static final String MINI_APP_REALTIME_TOPIC_PREFIX = MINI_APP_TOPIC_PREFIX + "/realtime/";

    private final LoginSessionReader sessionReader;

    public KanbanWebSocketConfig(LoginSessionReader sessionReader) {
        this.sessionReader = sessionReader;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/report/kanban").setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws/report/mini_app").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker(KANBAN_TOPIC_PREFIX, MINI_APP_TOPIC_PREFIX);
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                        message, StompHeaderAccessor.class);
                if (accessor == null) {
                    return message;
                }
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    authenticate(accessor);
                }
                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    authorizeSubscription(accessor);
                }
                return message;
            }
        });
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new MessagingException("WebSocket 未认证");
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        LoginUser user = sessionReader.resolve(token)
                .orElseThrow(() -> new MessagingException("WebSocket 会话已失效"));
        Map<String, Object> attributes = accessor.getSessionAttributes();
        if (attributes != null) {
            attributes.put(LOGIN_USER_ATTRIBUTE, user);
        }
        Principal principal = user::getUserNo;
        accessor.setUser(principal);
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        Map<String, Object> attributes = accessor.getSessionAttributes();
        LoginUser user = attributes == null ? null : (LoginUser) attributes.get(LOGIN_USER_ATTRIBUTE);
        if (user == null) {
            throw new MessagingException("WebSocket 未认证");
        }

        String destination = accessor.getDestination();
        if (destination == null) {
            throw new MessagingException("非法订阅主题");
        }

        boolean kanbanTopic = destination.startsWith(KANBAN_TOPIC_PREFIX + "/");
        boolean miniAppTopic = destination.startsWith(MINI_APP_REALTIME_TOPIC_PREFIX);
        if (!kanbanTopic && !miniAppTopic) {
            throw new MessagingException("非法订阅主题");
        }

        String[] parts = destination.split("/");
        int scopeTypeIndex = miniAppTopic ? 5 : 4;
        int scopeIdIndex = miniAppTopic ? 6 : 5;
        if (parts.length <= scopeIdIndex) {
            throw new MessagingException("非法订阅主题");
        }

        String scopeType = parts[scopeTypeIndex];
        if (!"workshop".equals(scopeType) && !"line".equals(scopeType)) {
            throw new MessagingException("非法订阅范围");
        }
        try {
            Long.valueOf(parts[scopeIdIndex]);
        } catch (NumberFormatException exception) {
            throw new MessagingException("非法订阅范围", exception);
        }
    }
}
