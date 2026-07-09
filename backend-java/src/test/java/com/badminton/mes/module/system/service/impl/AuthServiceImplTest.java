package com.badminton.mes.module.system.service.impl;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.system.constants.SystemErrorCodeConstants;
import com.badminton.mes.module.system.controller.vo.AuthLoginReqVO;
import com.badminton.mes.module.system.controller.vo.AuthLoginRespVO;
import com.badminton.mes.module.system.controller.vo.AuthPasswordReqVO;
import com.badminton.mes.module.system.dal.entity.RoleEntity;
import com.badminton.mes.module.system.dal.entity.UserEntity;
import com.badminton.mes.module.system.dal.entity.UserRoleEntity;
import com.badminton.mes.module.system.dal.redis.LoginSessionRedisDAO;
import com.badminton.mes.module.system.dal.redis.SystemRedisKeyConstants;
import com.badminton.mes.module.system.dal.repository.RoleRepository;
import com.badminton.mes.module.system.dal.repository.UserRepository;
import com.badminton.mes.module.system.dal.repository.UserRoleRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AuthServiceImpl} 单元测试。
 *
 * <p>覆盖登录成功、锁定拦截、失败计数、停用拦截与改密强制下线等核心分支，
 * 依赖全部 Mock，不连数据库与 Redis。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final Long USER_ID = 9L;

    private static final String USER_NO = "EMP001";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private LoginSessionRedisDAO loginSessionRedisDAO;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUpLoginContext() {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(USER_ID);
        loginUser.setUserNo(USER_NO);
        SecurityContextHolder.set("unit-test-token", loginUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    private UserEntity buildUser() {
        UserEntity user = new UserEntity();
        user.setId(USER_ID);
        user.setUserNo(USER_NO);
        user.setUserName("张三");
        user.setPassword("$bcrypt-hash$");
        user.setStatus(1);
        return user;
    }

    private AuthLoginReqVO buildLoginReq() {
        AuthLoginReqVO reqVO = new AuthLoginReqVO();
        reqVO.setUserNo(USER_NO);
        reqVO.setPassword("admin123");
        return reqVO;
    }

    @Test
    @DisplayName("登录成功：清失败计数、创建会话并返回 token 与角色")
    void loginSuccessCreatesSession() {
        UserEntity user = buildUser();
        when(loginSessionRedisDAO.isLoginLocked(USER_NO)).thenReturn(false);
        when(userRepository.findByUserNoAndDeletedFalse(USER_NO)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("admin123", user.getPassword())).thenReturn(true);
        UserRoleEntity relation = new UserRoleEntity();
        relation.setUserId(USER_ID);
        relation.setRoleId(2L);
        when(userRoleRepository.findByUserIdAndDeletedFalse(USER_ID)).thenReturn(List.of(relation));
        RoleEntity role = new RoleEntity();
        role.setId(2L);
        role.setRoleCode("PMC");
        role.setRoleName("PMC计划员");
        role.setStatus(1);
        when(roleRepository.findByIdInAndDeletedFalseOrderByIdAsc(List.of(2L))).thenReturn(List.of(role));

        AuthLoginRespVO respVO = authService.login(buildLoginReq());

        assertThat(respVO.getToken()).hasSize(32).matches("[0-9a-f]+");
        assertThat(respVO.getUserId()).isEqualTo(USER_ID);
        assertThat(respVO.getRoleCodes()).containsExactly("PMC");
        verify(loginSessionRedisDAO).clearLoginFail(USER_NO);
        ArgumentCaptor<LoginUser> captor = ArgumentCaptor.forClass(LoginUser.class);
        verify(loginSessionRedisDAO).createSession(eq(respVO.getToken()), captor.capture());
        assertThat(captor.getValue().getRoleCodes()).containsExactly("PMC");
    }

    @Test
    @DisplayName("登录锁定期内：直接拒绝 A0211，不做密码校验")
    void loginRejectedWhenLocked() {
        when(loginSessionRedisDAO.isLoginLocked(USER_NO)).thenReturn(true);

        assertThatThrownBy(() -> authService.login(buildLoginReq()))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(SystemErrorCodeConstants.LOGIN_LOCKED));
        verify(userRepository, never()).findByUserNoAndDeletedFalse(anyString());
    }

    @Test
    @DisplayName("密码错误：记失败计数并统一提示 A0200")
    void loginWrongPasswordRecordsFail() {
        UserEntity user = buildUser();
        when(loginSessionRedisDAO.isLoginLocked(USER_NO)).thenReturn(false);
        when(userRepository.findByUserNoAndDeletedFalse(USER_NO)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("admin123", user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(buildLoginReq()))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(SystemErrorCodeConstants.LOGIN_CREDENTIALS_INVALID));
        verify(loginSessionRedisDAO).recordLoginFail(USER_NO);
        verify(loginSessionRedisDAO, never()).createSession(anyString(), any());
    }

    @Test
    @DisplayName("账户不存在：与密码错误同码 A0200，防撞库枚举账号")
    void loginUnknownUserSameError() {
        when(loginSessionRedisDAO.isLoginLocked(USER_NO)).thenReturn(false);
        when(userRepository.findByUserNoAndDeletedFalse(USER_NO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(buildLoginReq()))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(SystemErrorCodeConstants.LOGIN_CREDENTIALS_INVALID));
        verify(loginSessionRedisDAO).recordLoginFail(USER_NO);
    }

    @Test
    @DisplayName("失败次数达到阈值：第 5 次失败直接返回 A0211 锁定，而非 A0200")
    void loginReturnsLockedWhenFailCountReachesThreshold() {
        UserEntity user = buildUser();
        when(loginSessionRedisDAO.isLoginLocked(USER_NO)).thenReturn(false);
        when(userRepository.findByUserNoAndDeletedFalse(USER_NO)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("admin123", user.getPassword())).thenReturn(false);
        // 第 5 次失败，达到锁定阈值
        when(loginSessionRedisDAO.recordLoginFail(USER_NO))
                .thenReturn((long) SystemRedisKeyConstants.LOGIN_FAIL_MAX);

        assertThatThrownBy(() -> authService.login(buildLoginReq()))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(SystemErrorCodeConstants.LOGIN_LOCKED));
    }

    @Test
    @DisplayName("账户已停用：密码正确仍拒绝 A0202，且不记失败计数")
    void loginDisabledUserRejected() {
        UserEntity user = buildUser();
        user.setStatus(0);
        when(loginSessionRedisDAO.isLoginLocked(USER_NO)).thenReturn(false);
        when(userRepository.findByUserNoAndDeletedFalse(USER_NO)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("admin123", user.getPassword())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(buildLoginReq()))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(SystemErrorCodeConstants.LOGIN_USER_DISABLED));
        verify(loginSessionRedisDAO, never()).recordLoginFail(anyString());
    }

    @Test
    @DisplayName("修改密码：旧密码正确则落库新哈希并强制下线")
    void changePasswordForcesLogout() {
        UserEntity user = buildUser();
        when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newPass123")).thenReturn("$new-hash$");
        AuthPasswordReqVO reqVO = new AuthPasswordReqVO();
        reqVO.setOldPassword("old");
        reqVO.setNewPassword("newPass123");

        authService.changePassword(reqVO);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("$new-hash$");
        verify(loginSessionRedisDAO).removeSessionByUserId(USER_ID);
    }

    @Test
    @DisplayName("修改密码：旧密码错误拒绝 A0210，不落库不下线")
    void changePasswordRejectsWrongOldPassword() {
        UserEntity user = buildUser();
        when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", user.getPassword())).thenReturn(false);
        AuthPasswordReqVO reqVO = new AuthPasswordReqVO();
        reqVO.setOldPassword("bad");
        reqVO.setNewPassword("newPass123");

        assertThatThrownBy(() -> authService.changePassword(reqVO))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(SystemErrorCodeConstants.USER_OLD_PASSWORD_MISMATCH));
        verify(userRepository, never()).save(any());
        verify(loginSessionRedisDAO, never()).removeSessionByUserId(any());
    }
}
