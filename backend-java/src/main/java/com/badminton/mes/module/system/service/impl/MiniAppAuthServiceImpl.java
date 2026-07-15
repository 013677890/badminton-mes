package com.badminton.mes.module.system.service.impl;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.system.constants.SystemErrorCodeConstants;
import com.badminton.mes.module.system.controller.vo.AuthLoginRespVO;
import com.badminton.mes.module.system.controller.vo.MiniAppBindReqVO;
import com.badminton.mes.module.system.controller.vo.MiniAppLoginReqVO;
import com.badminton.mes.module.system.controller.vo.MiniAppLoginRespVO;
import com.badminton.mes.module.system.dal.entity.UserEntity;
import com.badminton.mes.module.system.dal.entity.WechatUserBindingEntity;
import com.badminton.mes.module.system.dal.redis.LoginSessionRedisDAO;
import com.badminton.mes.module.system.dal.redis.MiniAppBindTicketRedisDAO;
import com.badminton.mes.module.system.dal.repository.WechatUserBindingRepository;
import com.badminton.mes.module.system.service.AuthenticationSupport;
import com.badminton.mes.module.system.service.MiniAppAuthService;
import com.badminton.mes.module.system.service.WechatCodeSessionClient;
import com.badminton.mes.module.system.service.dto.WechatCodeSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 微信小程序认证服务实现。
 *
 * @author Codex
 * @date 2026/07/15
 */
@Service
public class MiniAppAuthServiceImpl implements MiniAppAuthService {

    private static final int TICKET_BYTES = 16;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final WechatCodeSessionClient codeSessionClient;

    private final WechatUserBindingRepository bindingRepository;

    private final MiniAppBindTicketRedisDAO bindTicketRedisDAO;

    private final AuthenticationSupport authenticationSupport;

    private final LoginSessionRedisDAO loginSessionRedisDAO;

    private final String appId;

    /**
     * 构造小程序认证服务。
     *
     * @param codeSessionClient 微信 code2Session 客户端
     * @param bindingRepository 绑定关系 Repository
     * @param bindTicketRedisDAO 绑定票据 DAO
     * @param authenticationSupport 认证公共能力
     * @param loginSessionRedisDAO 登录会话 DAO
     * @param appId 小程序 AppID
     */
    public MiniAppAuthServiceImpl(WechatCodeSessionClient codeSessionClient,
                                  WechatUserBindingRepository bindingRepository,
                                  MiniAppBindTicketRedisDAO bindTicketRedisDAO,
                                  AuthenticationSupport authenticationSupport,
                                  LoginSessionRedisDAO loginSessionRedisDAO,
                                  @Value("${mes.wechat.mini-app.app-id:}") String appId) {
        this.codeSessionClient = codeSessionClient;
        this.bindingRepository = bindingRepository;
        this.bindTicketRedisDAO = bindTicketRedisDAO;
        this.authenticationSupport = authenticationSupport;
        this.loginSessionRedisDAO = loginSessionRedisDAO;
        this.appId = appId;
    }

    @Override
    public MiniAppLoginRespVO login(MiniAppLoginReqVO request) {
        WechatCodeSession codeSession = codeSessionClient.exchange(request.getCode());
        WechatUserBindingEntity binding = bindingRepository
                .findByAppIdAndOpenIdAndStatusAndDeletedFalse(
                        appId, codeSession.openId(), CommonStatusEnum.ENABLED.getStatus())
                .orElse(null);
        if (binding == null) {
            String ticket = generateTicket();
            bindTicketRedisDAO.save(ticket, codeSession.openId());
            MiniAppLoginRespVO response = new MiniAppLoginRespVO();
            response.setBindingRequired(true);
            response.setBindTicket(ticket);
            return response;
        }
        UserEntity user = authenticationSupport.loadEnabledUser(binding.getUserId());
        binding.setLastLoginTime(LocalDateTime.now());
        bindingRepository.save(binding);
        return toMiniAppResponse(authenticationSupport.createSession(user), user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MiniAppLoginRespVO bind(MiniAppBindReqVO request) {
        String openId = bindTicketRedisDAO.find(request.getBindTicket())
                .orElseThrow(() -> new ServiceException(SystemErrorCodeConstants.WECHAT_BIND_TICKET_INVALID));
        UserEntity user = authenticationSupport.authenticate(request.getUserNo(), request.getPassword());
        ensureBindingAvailable(openId, user.getId());

        WechatUserBindingEntity binding = new WechatUserBindingEntity();
        binding.setUserId(user.getId());
        binding.setAppId(appId);
        binding.setOpenId(openId);
        binding.setStatus(CommonStatusEnum.ENABLED.getStatus());
        binding.setLastLoginTime(LocalDateTime.now());
        binding.setDeleted(false);
        try {
            bindingRepository.saveAndFlush(binding);
        } catch (DataIntegrityViolationException exception) {
            throw new ServiceException(SystemErrorCodeConstants.WECHAT_BINDING_CONFLICT);
        }
        bindTicketRedisDAO.remove(request.getBindTicket());
        return toMiniAppResponse(authenticationSupport.createSession(user), user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbind() {
        Long userId = SecurityContextHolder.getRequiredLoginUserId();
        bindingRepository.findByAppIdAndUserIdAndStatusAndDeletedFalse(
                appId, userId, CommonStatusEnum.ENABLED.getStatus()).ifPresent(binding -> {
                    binding.setStatus(CommonStatusEnum.DISABLED.getStatus());
                    binding.setDeleted(true);
                    bindingRepository.save(binding);
                });
        loginSessionRedisDAO.removeSessionByUserId(userId);
    }

    private void ensureBindingAvailable(String openId, Long userId) {
        if (bindingRepository.findByAppIdAndOpenIdAndStatusAndDeletedFalse(
                appId, openId, CommonStatusEnum.ENABLED.getStatus()).isPresent()) {
            throw new ServiceException(SystemErrorCodeConstants.WECHAT_BINDING_CONFLICT);
        }
        if (bindingRepository.findByAppIdAndUserIdAndStatusAndDeletedFalse(
                appId, userId, CommonStatusEnum.ENABLED.getStatus()).isPresent()) {
            throw new ServiceException(SystemErrorCodeConstants.WECHAT_USER_ALREADY_BOUND);
        }
    }

    private MiniAppLoginRespVO toMiniAppResponse(AuthLoginRespVO login, UserEntity user) {
        MiniAppLoginRespVO response = new MiniAppLoginRespVO();
        response.setBindingRequired(false);
        response.setToken(login.getToken());
        response.setUserId(login.getUserId());
        response.setUserNo(login.getUserNo());
        response.setUserName(login.getUserName());
        response.setRoleCodes(login.getRoleCodes());
        response.setWorkshopId(user.getWorkshopId());
        response.setLineId(user.getLineId());
        return response;
    }

    private String generateTicket() {
        byte[] bytes = new byte[TICKET_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
