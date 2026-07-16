package com.badminton.mes.module.system.service.impl;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Pattern;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.system.constants.SystemErrorCodeConstants;
import com.badminton.mes.module.system.constants.WechatBindingStatusConstants;
import com.badminton.mes.module.system.controller.vo.AuthLoginRespVO;
import com.badminton.mes.module.system.controller.vo.MiniAppBindByCodeReqVO;
import com.badminton.mes.module.system.controller.vo.MiniAppLoginReqVO;
import com.badminton.mes.module.system.controller.vo.MiniAppLoginRespVO;
import com.badminton.mes.module.system.controller.vo.WechatBindingCodePreviewRespVO;
import com.badminton.mes.module.system.controller.vo.WechatBindingCodeRespVO;
import com.badminton.mes.module.system.controller.vo.WechatBindingStatusRespVO;
import com.badminton.mes.module.system.dal.entity.UserEntity;
import com.badminton.mes.module.system.dal.entity.WechatUserBindingEntity;
import com.badminton.mes.module.system.dal.redis.MiniAppBindTicketRedisDAO;
import com.badminton.mes.module.system.service.AuthenticationSupport;
import com.badminton.mes.module.system.service.MiniAppAuthService;
import com.badminton.mes.module.system.service.WechatCodeSessionClient;
import com.badminton.mes.module.system.service.WechatMiniAppCodeClient;
import com.badminton.mes.module.system.service.WechatUserBindingService;
import com.badminton.mes.module.system.service.dto.WechatBindingResult;
import com.badminton.mes.module.system.service.dto.WechatBindingTicket;
import com.badminton.mes.module.system.service.dto.WechatCodeSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 微信小程序认证和二维码绑定流程实现。
 *
 * <p>绑定关系以 MySQL 为事实源；Redis 仅保存短期、一次性消费的随机票据
 * 及轮询状态。扫码微信只提交 wx.login 临时 code，不接触 MES 密码。
 *
 * @author Codex
 * @date 2026/07/16
 */
@Service
public class MiniAppAuthServiceImpl implements MiniAppAuthService {

    private static final Logger logger = LoggerFactory.getLogger(MiniAppAuthServiceImpl.class);

    private static final int TICKET_BYTES = 16;

    private static final int USER_NO_KEEP_LENGTH = 2;

    private static final Pattern TICKET_PATTERN = Pattern.compile("^[0-9a-f]{32}$");

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final WechatCodeSessionClient codeSessionClient;

    private final WechatMiniAppCodeClient miniAppCodeClient;

    private final WechatUserBindingService bindingService;

    private final MiniAppBindTicketRedisDAO bindTicketRedisDAO;

    private final AuthenticationSupport authenticationSupport;

    public MiniAppAuthServiceImpl(
            WechatCodeSessionClient codeSessionClient,
            WechatMiniAppCodeClient miniAppCodeClient,
            WechatUserBindingService bindingService,
            MiniAppBindTicketRedisDAO bindTicketRedisDAO,
            AuthenticationSupport authenticationSupport) {
        this.codeSessionClient = codeSessionClient;
        this.miniAppCodeClient = miniAppCodeClient;
        this.bindingService = bindingService;
        this.bindTicketRedisDAO = bindTicketRedisDAO;
        this.authenticationSupport = authenticationSupport;
    }

    @Override
    public MiniAppLoginRespVO login(MiniAppLoginReqVO request) {
        WechatCodeSession codeSession = codeSessionClient.exchange(request.getCode());
        Optional<WechatUserBindingEntity> bindingOptional = bindingService.findActiveByOpenId(codeSession.openId());
        if (bindingOptional.isEmpty()) {
            MiniAppLoginRespVO response = new MiniAppLoginRespVO();
            response.setBindingRequired(true);
            response.setWechatBound(false);
            return response;
        }

        LocalDateTime loginTime = LocalDateTime.now();
        WechatUserBindingEntity binding = bindingService.updateLastLogin(bindingOptional.get(), loginTime);
        UserEntity user = authenticationSupport.loadEnabledUser(binding.getUserId());
        return toMiniAppResponse(authenticationSupport.createSession(user), user, binding);
    }

    @Override
    public WechatBindingCodeRespVO createBindingCode() {
        Long userId = SecurityContextHolder.getRequiredLoginUserId();
        UserEntity user = authenticationSupport.loadEnabledUser(userId);
        if (bindingService.findActiveByUserId(userId).isPresent()) {
            throw new ServiceException(SystemErrorCodeConstants.WECHAT_USER_ALREADY_BOUND);
        }

        String ticket = generateTicket();
        LocalDateTime expiresAt = LocalDateTime.now().plus(bindTicketRedisDAO.getTicketTtl());
        byte[] codeImage = miniAppCodeClient.generateBindingCode(ticket);
        bindTicketRedisDAO.save(ticket, new WechatBindingTicket(
                user.getId(), user.getUserNo(), user.getUserName(), expiresAt));

        WechatBindingCodeRespVO response = new WechatBindingCodeRespVO();
        response.setTicket(ticket);
        response.setExpiresAt(expiresAt);
        response.setCodeImageBase64(Base64.getEncoder().encodeToString(codeImage));
        response.setStatus(WechatBindingStatusConstants.PENDING);
        return response;
    }

    @Override
    public WechatBindingCodePreviewRespVO previewBindingCode(String ticket) {
        validateTicketFormat(ticket);
        WechatBindingTicket payload = bindTicketRedisDAO.find(ticket)
                .orElseThrow(() -> new ServiceException(SystemErrorCodeConstants.WECHAT_BIND_TICKET_INVALID));
        if (isExpired(payload.expiresAt())) {
            throw new ServiceException(SystemErrorCodeConstants.WECHAT_BIND_TICKET_INVALID);
        }
        authenticationSupport.loadEnabledUser(payload.userId());

        WechatBindingCodePreviewRespVO response = new WechatBindingCodePreviewRespVO();
        response.setUserName(payload.userName());
        response.setMaskedUserNo(maskUserNo(payload.userNo()));
        response.setExpiresAt(payload.expiresAt());
        response.setStatus(WechatBindingStatusConstants.PENDING);
        return response;
    }

    @Override
    public WechatBindingStatusRespVO getBindingCodeStatus(String ticket) {
        validateTicketFormat(ticket);
        WechatBindingResult result = bindTicketRedisDAO.findResult(ticket)
                .orElseThrow(() -> new ServiceException(SystemErrorCodeConstants.WECHAT_BIND_TICKET_INVALID));
        Long currentUserId = SecurityContextHolder.getRequiredLoginUserId();
        if (!currentUserId.equals(result.userId())) {
            throw new ServiceException(SystemErrorCodeConstants.WECHAT_BIND_TICKET_INVALID);
        }

        Optional<WechatUserBindingEntity> binding = bindingService.findActiveByUserId(result.userId());
        if (binding.isPresent()) {
            LocalDateTime bindingTime = binding.get().getCreateTime();
            if (bindingTime == null) {
                bindingTime = result.bindingTime();
            }
            WechatBindingResult bound = new WechatBindingResult(
                    result.userId(), WechatBindingStatusConstants.BOUND, result.expiresAt(), bindingTime);
            saveResultQuietly(ticket, bound);
            return toStatusResponse(bound);
        }
        if (isExpired(result.expiresAt())) {
            return toStatusResponse(new WechatBindingResult(
                    result.userId(), WechatBindingStatusConstants.EXPIRED, result.expiresAt(), null));
        }
        return toStatusResponse(result);
    }

    @Override
    public WechatBindingStatusRespVO bindByCode(MiniAppBindByCodeReqVO request) {
        WechatCodeSession codeSession = codeSessionClient.exchange(request.getCode());
        WechatBindingTicket ticket = bindTicketRedisDAO.consume(request.getTicket())
                .orElseThrow(() -> new ServiceException(SystemErrorCodeConstants.WECHAT_BIND_TICKET_INVALID));
        if (isExpired(ticket.expiresAt())) {
            saveResultQuietly(request.getTicket(), new WechatBindingResult(
                    ticket.userId(), WechatBindingStatusConstants.EXPIRED, ticket.expiresAt(), null));
            throw new ServiceException(SystemErrorCodeConstants.WECHAT_BIND_TICKET_INVALID);
        }

        UserEntity user = authenticationSupport.loadEnabledUser(ticket.userId());
        try {
            bindingService.bind(user.getId(), codeSession.openId());
        } catch (ServiceException exception) {
            saveResultQuietly(request.getTicket(), new WechatBindingResult(
                    ticket.userId(), WechatBindingStatusConstants.EXPIRED, ticket.expiresAt(), null));
            throw exception;
        }

        LocalDateTime bindingTime = LocalDateTime.now();
        WechatBindingResult result = new WechatBindingResult(
                ticket.userId(), WechatBindingStatusConstants.BOUND, ticket.expiresAt(), bindingTime);
        saveResultQuietly(request.getTicket(), result);
        return toStatusResponse(result);
    }

    @Override
    public void unbind() {
        bindingService.unbind(SecurityContextHolder.getRequiredLoginUserId());
    }

    private MiniAppLoginRespVO toMiniAppResponse(
            AuthLoginRespVO login, UserEntity user, WechatUserBindingEntity binding) {
        MiniAppLoginRespVO response = new MiniAppLoginRespVO();
        response.setBindingRequired(false);
        response.setToken(login.getToken());
        response.setUserId(login.getUserId());
        response.setUserNo(login.getUserNo());
        response.setUserName(login.getUserName());
        response.setRoleCodes(login.getRoleCodes());
        response.setWorkshopId(user.getWorkshopId());
        response.setLineId(user.getLineId());
        response.setWechatBound(true);
        response.setWechatBindingTime(binding.getCreateTime());
        response.setWechatLastLoginTime(binding.getLastLoginTime());
        return response;
    }

    private WechatBindingStatusRespVO toStatusResponse(WechatBindingResult result) {
        WechatBindingStatusRespVO response = new WechatBindingStatusRespVO();
        response.setStatus(result.status());
        response.setExpiresAt(result.expiresAt());
        response.setBindingTime(result.bindingTime());
        return response;
    }

    private void saveResultQuietly(String ticket, WechatBindingResult result) {
        try {
            bindTicketRedisDAO.saveResult(ticket, result);
        } catch (RuntimeException exception) {
            logger.warn("[微信绑定状态写入 Redis 失败] userId: {}, status: {}",
                    result.userId(), result.status(), exception);
        }
    }

    private void validateTicketFormat(String ticket) {
        if (ticket == null || !TICKET_PATTERN.matcher(ticket).matches()) {
            throw new ServiceException(SystemErrorCodeConstants.WECHAT_BIND_TICKET_INVALID);
        }
    }

    private boolean isExpired(LocalDateTime expiresAt) {
        return expiresAt == null || !expiresAt.isAfter(LocalDateTime.now());
    }

    private String maskUserNo(String userNo) {
        if (userNo == null || userNo.isBlank()) {
            return "****";
        }
        if (userNo.length() <= USER_NO_KEEP_LENGTH * 2) {
            return userNo.substring(0, 1) + "***";
        }
        return userNo.substring(0, USER_NO_KEEP_LENGTH) + "****"
                + userNo.substring(userNo.length() - USER_NO_KEEP_LENGTH);
    }

    private String generateTicket() {
        byte[] bytes = new byte[TICKET_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
