package com.badminton.mes.module.report.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.badminton.mes.common.security.LoginUser;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

class KanbanWebSocketConfigTest {

    private static final String TOKEN = "test-token";

    private LoginUser loginUser;
    private ChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        loginUser = new LoginUser();
        loginUser.setUserNo("U1001");
        loginUser.setWorkshopId(10L);
        loginUser.setLineId(20L);
        loginUser.setRoleCodes(List.of("OPERATOR"));

        KanbanWebSocketConfig config = new KanbanWebSocketConfig(
                token -> TOKEN.equals(token) ? Optional.of(loginUser) : Optional.empty());
        TestChannelRegistration registration = new TestChannelRegistration();
        config.configureClientInboundChannel(registration);
        interceptor = registration.getConfiguredInterceptors().getFirst();
    }

    @Test
    @DisplayName("simple broker 同时分发看板与小程序主题")
    void shouldEnableKanbanAndMiniAppBrokerPrefixes() {
        MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);
        KanbanWebSocketConfig config = new KanbanWebSocketConfig(token -> Optional.empty());

        config.configureMessageBroker(registry);

        verify(registry).enableSimpleBroker("/topic/report/kanban", "/topic/report/mini_app");
        verify(registry).setApplicationDestinationPrefixes("/app");
    }

    @Test
    @DisplayName("CONNECT 使用 Bearer 会话建立用户身份")
    void shouldAuthenticateConnectFrame() {
        Map<String, Object> sessionAttributes = new HashMap<>();
        Message<?> message = createMessage(StompCommand.CONNECT, sessionAttributes, null, "Bearer " + TOKEN);

        interceptor.preSend(message, mock(org.springframework.messaging.MessageChannel.class));

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        assertEquals("U1001", accessor.getUser().getName());
    }

    @Test
    @DisplayName("允许用户订阅自身产线的小程序实时主题")
    void shouldAllowOwnMiniAppLineSubscription() {
        Map<String, Object> sessionAttributes = authenticatedSession();
        Message<?> message = createMessage(
                StompCommand.SUBSCRIBE,
                sessionAttributes,
                "/topic/report/mini_app/realtime/line/20",
                null);

        assertDoesNotThrow(() -> interceptor.preSend(
                message, mock(org.springframework.messaging.MessageChannel.class)));
    }

    @Test
    @DisplayName("允许用户订阅自身产线的中控看板主题")
    void shouldAllowOwnKanbanLineSubscription() {
        Map<String, Object> sessionAttributes = authenticatedSession();
        Message<?> message = createMessage(
                StompCommand.SUBSCRIBE,
                sessionAttributes,
                "/topic/report/kanban/line/20",
                null);

        assertDoesNotThrow(() -> interceptor.preSend(
                message, mock(org.springframework.messaging.MessageChannel.class)));
    }

    @Test
    @DisplayName("拒绝订阅其他产线的小程序实时主题")
    void shouldRejectOtherMiniAppLineSubscription() {
        Map<String, Object> sessionAttributes = authenticatedSession();
        Message<?> message = createMessage(
                StompCommand.SUBSCRIBE,
                sessionAttributes,
                "/topic/report/mini_app/realtime/line/21",
                null);

        assertThrows(MessagingException.class, () -> interceptor.preSend(
                message, mock(org.springframework.messaging.MessageChannel.class)));
    }

    private Map<String, Object> authenticatedSession() {
        Map<String, Object> sessionAttributes = new HashMap<>();
        Message<?> connectMessage = createMessage(
                StompCommand.CONNECT, sessionAttributes, null, "Bearer " + TOKEN);
        interceptor.preSend(connectMessage, mock(org.springframework.messaging.MessageChannel.class));
        return sessionAttributes;
    }

    private Message<?> createMessage(
            StompCommand command,
            Map<String, Object> sessionAttributes,
            String destination,
            String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionAttributes(sessionAttributes);
        accessor.setDestination(destination);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static final class TestChannelRegistration extends ChannelRegistration {

        private List<ChannelInterceptor> getConfiguredInterceptors() {
            return super.getInterceptors();
        }
    }
}
