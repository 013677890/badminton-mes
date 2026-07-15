package com.badminton.mes.module.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.system.controller.vo.AuthLoginRespVO;
import com.badminton.mes.module.system.controller.vo.MiniAppBindReqVO;
import com.badminton.mes.module.system.controller.vo.MiniAppLoginReqVO;
import com.badminton.mes.module.system.dal.entity.UserEntity;
import com.badminton.mes.module.system.dal.entity.WechatUserBindingEntity;
import com.badminton.mes.module.system.dal.redis.LoginSessionRedisDAO;
import com.badminton.mes.module.system.dal.redis.MiniAppBindTicketRedisDAO;
import com.badminton.mes.module.system.dal.repository.WechatUserBindingRepository;
import com.badminton.mes.module.system.service.AuthenticationSupport;
import com.badminton.mes.module.system.service.WechatCodeSessionClient;
import com.badminton.mes.module.system.service.dto.WechatCodeSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MiniAppAuthServiceImplTest {

    private WechatCodeSessionClient codeSessionClient;

    private WechatUserBindingRepository bindingRepository;

    private MiniAppBindTicketRedisDAO bindTicketRedisDAO;

    private AuthenticationSupport authenticationSupport;

    private MiniAppAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        codeSessionClient = mock(WechatCodeSessionClient.class);
        bindingRepository = mock(WechatUserBindingRepository.class);
        bindTicketRedisDAO = mock(MiniAppBindTicketRedisDAO.class);
        authenticationSupport = mock(AuthenticationSupport.class);
        service = new MiniAppAuthServiceImpl(codeSessionClient, bindingRepository, bindTicketRedisDAO,
                authenticationSupport, mock(LoginSessionRedisDAO.class), "test-app");
    }

    @Test
    void shouldReturnBindTicketWhenWechatIdentityIsUnbound() {
        when(codeSessionClient.exchange("code")).thenReturn(new WechatCodeSession("openid"));
        when(bindingRepository.findByAppIdAndOpenIdAndStatusAndDeletedFalse("test-app", "openid", 1))
                .thenReturn(Optional.empty());

        MiniAppLoginReqVO request = new MiniAppLoginReqVO();
        request.setCode("code");
        var response = service.login(request);

        assertTrue(response.isBindingRequired());
        assertNotNull(response.getBindTicket());
        verify(bindTicketRedisDAO).save(response.getBindTicket(), "openid");
    }

    @Test
    void shouldCreateSessionWhenWechatIdentityIsBound() {
        when(codeSessionClient.exchange("code")).thenReturn(new WechatCodeSession("openid"));
        WechatUserBindingEntity binding = new WechatUserBindingEntity();
        binding.setUserId(7L);
        when(bindingRepository.findByAppIdAndOpenIdAndStatusAndDeletedFalse("test-app", "openid", 1))
                .thenReturn(Optional.of(binding));
        UserEntity user = user(7L, "U007");
        when(authenticationSupport.loadEnabledUser(7L)).thenReturn(user);
        when(authenticationSupport.createSession(user)).thenReturn(loginResponse(user));

        MiniAppLoginReqVO request = new MiniAppLoginReqVO();
        request.setCode("code");
        var response = service.login(request);

        assertFalse(response.isBindingRequired());
        assertNotNull(response.getToken());
        verify(bindingRepository).save(binding);
    }

    @Test
    void shouldBindMesUserAndConsumeTicket() {
        when(bindTicketRedisDAO.find("ticket")).thenReturn(Optional.of("openid"));
        UserEntity user = user(8L, "U008");
        when(authenticationSupport.authenticate("U008", "password")).thenReturn(user);
        when(bindingRepository.findByAppIdAndOpenIdAndStatusAndDeletedFalse("test-app", "openid", 1))
                .thenReturn(Optional.empty());
        when(bindingRepository.findByAppIdAndUserIdAndStatusAndDeletedFalse("test-app", 8L, 1))
                .thenReturn(Optional.empty());
        when(bindingRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(authenticationSupport.createSession(user)).thenReturn(loginResponse(user));

        MiniAppBindReqVO request = new MiniAppBindReqVO();
        request.setBindTicket("ticket");
        request.setUserNo("U008");
        request.setPassword("password");
        var response = service.bind(request);

        assertFalse(response.isBindingRequired());
        assertNotNull(response.getToken());
        verify(bindTicketRedisDAO).remove("ticket");
    }

    private UserEntity user(Long id, String userNo) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUserNo(userNo);
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
