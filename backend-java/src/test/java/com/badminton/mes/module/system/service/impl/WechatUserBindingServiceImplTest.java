package com.badminton.mes.module.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.system.constants.SystemErrorCodeConstants;
import com.badminton.mes.module.system.dal.entity.WechatUserBindingEntity;
import com.badminton.mes.module.system.dal.redis.LoginSessionRedisDAO;
import com.badminton.mes.module.system.dal.repository.WechatUserBindingRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 微信绑定 MySQL 事实源服务测试。 */
@ExtendWith(MockitoExtension.class)
class WechatUserBindingServiceImplTest {

    private static final Long USER_ID = 7L;

    @Mock
    private WechatUserBindingRepository bindingRepository;

    @Mock
    private LoginSessionRedisDAO loginSessionRedisDAO;

    private WechatUserBindingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new WechatUserBindingServiceImpl(bindingRepository, loginSessionRedisDAO, "test-app");
    }

    @Test
    @DisplayName("绑定：应用层校验后写入有效绑定，数据库唯一索引继续兜底")
    void bindCreatesActiveBinding() {
        when(bindingRepository.findByAppIdAndOpenIdAndStatusAndDeletedFalse("test-app", "openid", 1))
                .thenReturn(Optional.empty());
        when(bindingRepository.findByAppIdAndUserIdAndStatusAndDeletedFalse("test-app", USER_ID, 1))
                .thenReturn(Optional.empty());
        when(bindingRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WechatUserBindingEntity result = service.bind(USER_ID, "openid");

        assertThat(result.getAppId()).isEqualTo("test-app");
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getOpenId()).isEqualTo("openid");
        assertThat(result.getStatus()).isEqualTo(1);
        assertThat(result.getDeleted()).isFalse();
    }

    @Test
    @DisplayName("绑定：OpenID 已被使用时拒绝")
    void bindRejectsOpenIdConflict() {
        when(bindingRepository.findByAppIdAndOpenIdAndStatusAndDeletedFalse("test-app", "openid", 1))
                .thenReturn(Optional.of(new WechatUserBindingEntity()));

        assertThatThrownBy(() -> service.bind(USER_ID, "openid"))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(SystemErrorCodeConstants.WECHAT_BINDING_CONFLICT));
        verify(bindingRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("解绑：逻辑删除绑定并使目标用户全部会话失效")
    void unbindDisablesBindingAndEvictsSessions() {
        WechatUserBindingEntity binding = new WechatUserBindingEntity();
        binding.setUserId(USER_ID);
        binding.setStatus(1);
        binding.setDeleted(false);
        when(bindingRepository.findByAppIdAndUserIdAndStatusAndDeletedFalse("test-app", USER_ID, 1))
                .thenReturn(Optional.of(binding));

        service.unbind(USER_ID);

        ArgumentCaptor<WechatUserBindingEntity> captor = ArgumentCaptor.forClass(WechatUserBindingEntity.class);
        verify(bindingRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getStatus()).isZero();
        assertThat(captor.getValue().getDeleted()).isTrue();
        verify(loginSessionRedisDAO).removeSessionByUserId(USER_ID);
    }
}
