package com.badminton.mes.module.system.service.impl;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.system.constants.SystemErrorCodeConstants;
import com.badminton.mes.module.system.dal.entity.WechatUserBindingEntity;
import com.badminton.mes.module.system.dal.redis.LoginSessionRedisDAO;
import com.badminton.mes.module.system.dal.repository.WechatUserBindingRepository;
import com.badminton.mes.module.system.service.WechatUserBindingService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 微信用户绑定关系服务实现。
 *
 * @author Codex
 * @date 2026/07/16
 */
@Service
public class WechatUserBindingServiceImpl implements WechatUserBindingService {

    private final WechatUserBindingRepository bindingRepository;

    private final LoginSessionRedisDAO loginSessionRedisDAO;

    private final String appId;

    public WechatUserBindingServiceImpl(
            WechatUserBindingRepository bindingRepository,
            LoginSessionRedisDAO loginSessionRedisDAO,
            @Value("${mes.wechat.mini-app.app-id:}") String appId) {
        this.bindingRepository = bindingRepository;
        this.loginSessionRedisDAO = loginSessionRedisDAO;
        this.appId = appId;
    }

    @Override
    public Optional<WechatUserBindingEntity> findActiveByOpenId(String openId) {
        return bindingRepository.findByAppIdAndOpenIdAndStatusAndDeletedFalse(
                appId, openId, CommonStatusEnum.ENABLED.getStatus());
    }

    @Override
    public Optional<WechatUserBindingEntity> findActiveByUserId(Long userId) {
        return bindingRepository.findByAppIdAndUserIdAndStatusAndDeletedFalse(
                appId, userId, CommonStatusEnum.ENABLED.getStatus());
    }

    @Override
    public Map<Long, WechatUserBindingEntity> findActiveByUserIds(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return bindingRepository.findByAppIdAndUserIdInAndStatusAndDeletedFalse(
                        appId, userIds, CommonStatusEnum.ENABLED.getStatus())
                .stream()
                .collect(Collectors.toMap(WechatUserBindingEntity::getUserId, Function.identity()));
    }

    @Override
    public Set<Long> findActiveUserIds() {
        return bindingRepository.findByAppIdAndStatusAndDeletedFalse(
                        appId, CommonStatusEnum.ENABLED.getStatus())
                .stream()
                .map(WechatUserBindingEntity::getUserId)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WechatUserBindingEntity bind(Long userId, String openId) {
        if (findActiveByOpenId(openId).isPresent()) {
            throw new ServiceException(SystemErrorCodeConstants.WECHAT_BINDING_CONFLICT);
        }
        if (findActiveByUserId(userId).isPresent()) {
            throw new ServiceException(SystemErrorCodeConstants.WECHAT_USER_ALREADY_BOUND);
        }

        WechatUserBindingEntity binding = new WechatUserBindingEntity();
        binding.setUserId(userId);
        binding.setAppId(appId);
        binding.setOpenId(openId);
        binding.setStatus(CommonStatusEnum.ENABLED.getStatus());
        binding.setDeleted(false);
        try {
            return bindingRepository.saveAndFlush(binding);
        } catch (DataIntegrityViolationException exception) {
            throw new ServiceException(SystemErrorCodeConstants.WECHAT_BINDING_CONFLICT);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WechatUserBindingEntity updateLastLogin(WechatUserBindingEntity binding, LocalDateTime loginTime) {
        binding.setLastLoginTime(loginTime);
        return bindingRepository.save(binding);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbind(Long userId) {
        findActiveByUserId(userId).ifPresent(binding -> {
            binding.setStatus(CommonStatusEnum.DISABLED.getStatus());
            binding.setDeleted(true);
            bindingRepository.saveAndFlush(binding);
        });
        evictSessionAfterCommit(userId);
    }

    private void evictSessionAfterCommit(Long userId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            loginSessionRedisDAO.removeSessionByUserId(userId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                loginSessionRedisDAO.removeSessionByUserId(userId);
            }
        });
    }
}
