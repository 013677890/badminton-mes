package com.badminton.mes.module.report.config;

import com.badminton.mes.common.security.*;
import java.security.Principal;
import java.util.Map;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.*;

/** 看板 STOMP WebSocket 配置及订阅鉴权。 @author 刘涵 */
@Configuration
@EnableWebSocketMessageBroker
public class KanbanWebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private static final String LOGIN_USER_ATTRIBUTE = "kanbanLoginUser";
    private final LoginSessionReader sessionReader;

    public KanbanWebSocketConfig(LoginSessionReader sessionReader) { this.sessionReader = sessionReader; }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/report/kanban").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic/report/kanban");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) return message;
                if (StompCommand.CONNECT.equals(accessor.getCommand())) authenticate(accessor);
                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) authorizeSubscription(accessor);
                return message;
            }
        });
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) throw new MessagingException("WebSocket 未认证");
        String token = authorization.substring(7).trim();
        LoginUser user = sessionReader.resolve(token).orElseThrow(() -> new MessagingException("WebSocket 会话已失效"));
        Map<String, Object> attributes = accessor.getSessionAttributes();
        if (attributes != null) attributes.put(LOGIN_USER_ATTRIBUTE, user);
        Principal principal = () -> user.getUserNo(); accessor.setUser(principal);
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        Map<String, Object> attributes = accessor.getSessionAttributes();
        LoginUser user = attributes == null ? null : (LoginUser) attributes.get(LOGIN_USER_ATTRIBUTE);
        if (user == null) throw new MessagingException("WebSocket 未认证");
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith("/topic/report/kanban/")) throw new MessagingException("非法订阅主题");
        if (user.getRoleCodes().contains(RoleCodeConstants.ADMIN) || user.getRoleCodes().contains(RoleCodeConstants.PMC)) return;
        String[] parts = destination.split("/");
        if (parts.length < 7) throw new MessagingException("非法订阅主题");
        String scopeType = parts[5]; Long scopeId;
        try { scopeId = Long.valueOf(parts[6]); } catch (NumberFormatException exception) { throw new MessagingException("非法订阅范围", exception); }
        boolean allowed = "workshop".equals(scopeType) ? scopeId.equals(user.getWorkshopId())
                : "line".equals(scopeType) && scopeId.equals(user.getLineId());
        if (!allowed) throw new MessagingException("无权订阅该看板范围");
    }
}
