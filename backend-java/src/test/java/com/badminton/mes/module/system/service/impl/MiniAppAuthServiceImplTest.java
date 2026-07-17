package com.badminton.mes.module.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.system.constants.SystemErrorCodeConstants;
import com.badminton.mes.module.system.constants.WechatBindingStatusConstants;
import com.badminton.mes.module.system.controller.vo.AuthLoginRespVO;
import com.badminton.mes.module.system.controller.vo.MiniAppBindByCodeReqVO;
import com.badminton.mes.module.system.controller.vo.MiniAppLoginReqVO;
import com.badminton.mes.module.system.dal.entity.UserEntity;
import com.badminton.mes.module.system.dal.entity.WechatUserBindingEntity;
import com.badminton.mes.module.system.dal.redis.MiniAppBindTicketRedisDAO;
import com.badminton.mes.module.system.service.AuthenticationSupport;
import com.badminton.mes.module.system.service.WechatCodeSessionClient;
import com.badminton.mes.module.system.service.WechatMiniAppCodeClient;
import com.badminton.mes.module.system.service.WechatUserBindingService;
import com.badminton.mes.module.system.service.dto.WechatBindingResult;
import com.badminton.mes.module.system.service.dto.WechatBindingTicket;
import com.badminton.mes.module.system.service.dto.WechatCodeSession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** 微信小程序二维码绑定流程单元测试。 */
class MiniAppAuthServiceImplTest {

    private static final Long USER_ID = 8L;

    private static final String TICKET = "0123456789abcdef0123456789abcdef";

    private WechatCodeSessionClient codeSessionClient;

    private WechatMiniAppCodeClient miniAppCodeClient;

    private WechatUserBindingService bindingService;

    private MiniAppBindTicketRedisDAO bindTicketRedisDAO;

    private AuthenticationSupport authenticationSupport;

    private MiniAppAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        codeSessionClient = mock(WechatCodeSessionClient.class);
        miniAppCodeClient = mock(WechatMiniAppCodeClient.class);
        bindingService = mock(WechatUserBindingService.class);
        bindTicketRedisDAO = mock(MiniAppBindTicketRedisDAO.class);
        authenticationSupport = mock(AuthenticationSupport.class);
        service = new MiniAppAuthServiceImpl(codeSessionClient, miniAppCodeClient,
                bindingService, bindTicketRedisDAO, authenticationSupport);
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(USER_ID);
        loginUser.setUserNo("U008");
        SecurityContextHolder.set("test-token", loginUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    @DisplayName("微信未绑定时只提示先走账号登录，不再下发旧密码绑定票据")
    void shouldRequireAccountLoginWhenWechatIdentityIsUnbound() {
        when(codeSessionClient.exchange("code")).thenReturn(new WechatCodeSession("openid"));
        when(bindingService.findActiveByOpenId("openid")).thenReturn(Optional.empty());

        MiniAppLoginReqVO request = new MiniAppLoginReqVO();
        request.setCode("code");
        var response = service.login(request);

        assertThat(response.isBindingRequired()).isTrue();
        assertThat(response.getBindTicket()).isNull();
        assertThat(response.isWechatBound()).isFalse();
        verify(bindTicketRedisDAO, never()).save(any(), any());
    }

    @Test
    @DisplayName("已绑定微信登录时更新最近登录时间并创建会话")
    void shouldCreateSessionWhenWechatIdentityIsBound() {
        when(codeSessionClient.exchange("code")).thenReturn(new WechatCodeSession("openid"));
        WechatUserBindingEntity binding = new WechatUserBindingEntity();
        binding.setId(3L);
        binding.setUserId(USER_ID);
        binding.setCreateTime(LocalDateTime.now().minusDays(1));
        when(bindingService.findActiveByOpenId("openid")).thenReturn(Optional.of(binding));
        when(bindingService.updateLastLogin(any(), any())).thenAnswer(invocation -> {
            binding.setLastLoginTime(invocation.getArgument(1));
            return binding;
        });
        UserEntity user = user();
        when(authenticationSupport.loadEnabledUser(USER_ID)).thenReturn(user);
        when(authenticationSupport.createSession(user)).thenReturn(loginResponse(user));

        MiniAppLoginReqVO request = new MiniAppLoginReqVO();
        request.setCode("code");
        var response = service.login(request);

        assertThat(response.isBindingRequired()).isFalse();
        assertThat(response.isWechatBound()).isTrue();
        assertThat(response.getToken()).isEqualTo("token");
        verify(bindingService).updateLastLogin(any(), any());
    }

    @Test
    @DisplayName("账号生成 32 位 scene 并保存短期 Redis 票据")
    void shouldCreateBindingCodeForCurrentAccount() {
        UserEntity user = user();
        when(authenticationSupport.loadEnabledUser(USER_ID)).thenReturn(user);
        when(bindingService.findActiveByUserId(USER_ID)).thenReturn(Optional.empty());
        when(bindTicketRedisDAO.getTicketTtl()).thenReturn(Duration.ofMinutes(5));
        when(miniAppCodeClient.generateBindingCode(any())).thenReturn(new byte[] {(byte) 0x89, 0x50});

        var response = service.createBindingCode();

        assertThat(response.getTicket()).hasSize(32).matches("[0-9a-f]{32}");
        assertThat(response.getCodeImageBase64()).isNotBlank();
        assertThat(response.getStatus()).isEqualTo(WechatBindingStatusConstants.PENDING);
        verify(miniAppCodeClient).generateBindingCode(response.getTicket());
        ArgumentCaptor<WechatBindingTicket> captor = ArgumentCaptor.forClass(WechatBindingTicket.class);
        verify(bindTicketRedisDAO).save(eq(response.getTicket()), captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("扫码确认原子消费票据并通过 wx.login OpenID 建立绑定")
    void shouldConsumeTicketAndBindWechat() {
        when(codeSessionClient.exchange("scan-code")).thenReturn(new WechatCodeSession("openid"));
        WechatBindingTicket ticket = new WechatBindingTicket(
                USER_ID, "U008", "测试用户", LocalDateTime.now().plusMinutes(5));
        when(bindTicketRedisDAO.consume(TICKET)).thenReturn(Optional.of(ticket));
        when(authenticationSupport.loadEnabledUser(USER_ID)).thenReturn(user());
        when(bindingService.bind(USER_ID, "openid")).thenReturn(new WechatUserBindingEntity());

        MiniAppBindByCodeReqVO request = new MiniAppBindByCodeReqVO();
        request.setTicket(TICKET);
        request.setCode("scan-code");
        var response = service.bindByCode(request);

        assertThat(response.getStatus()).isEqualTo(WechatBindingStatusConstants.BOUND);
        verify(bindTicketRedisDAO).consume(TICKET);
        verify(bindingService).bind(USER_ID, "openid");
        ArgumentCaptor<WechatBindingResult> captor = ArgumentCaptor.forClass(WechatBindingResult.class);
        verify(bindTicketRedisDAO).saveResult(eq(TICKET), captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(WechatBindingStatusConstants.BOUND);
    }

    @Test
    @DisplayName("已过期票据即使被消费也不能建立绑定")
    void shouldRejectExpiredConsumedTicket() {
        when(codeSessionClient.exchange("scan-code")).thenReturn(new WechatCodeSession("openid"));
        when(bindTicketRedisDAO.consume(TICKET)).thenReturn(Optional.of(new WechatBindingTicket(
                USER_ID, "U008", "测试用户", LocalDateTime.now().minusSeconds(1))));
        MiniAppBindByCodeReqVO request = new MiniAppBindByCodeReqVO();
        request.setTicket(TICKET);
        request.setCode("scan-code");

        assertThatThrownBy(() -> service.bindByCode(request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(SystemErrorCodeConstants.WECHAT_BIND_TICKET_INVALID));
        verify(bindingService, never()).bind(any(), any());
    }

    private UserEntity user() {
        UserEntity user = new UserEntity();
        user.setId(USER_ID);
        user.setUserNo("U008");
        user.setUserName("测试用户");
        user.setWorkshopId(1L);
        user.setLineId(2L);
        return user;
    }

    private AuthLoginRespVO loginResponse(UserEntity user) {
        AuthLoginRespVO response = new AuthLoginRespVO();
        response.setToken("token");
        response.setUserId(user.getId());
        response.setUserNo(user.getUserNo());
        response.setUserName(user.getUserName());
        response.setRoleCodes(List.of("WORKSHOP_MANAGER"));
        return response;
    }
}
