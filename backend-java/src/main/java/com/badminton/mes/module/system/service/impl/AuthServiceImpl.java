package com.badminton.mes.module.system.service.impl;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.common.util.DesensitizeUtils;
import com.badminton.mes.module.system.constants.SystemErrorCodeConstants;
import com.badminton.mes.module.system.controller.vo.AuthLoginReqVO;
import com.badminton.mes.module.system.controller.vo.AuthLoginRespVO;
import com.badminton.mes.module.system.controller.vo.AuthPasswordReqVO;
import com.badminton.mes.module.system.controller.vo.AuthProfileRespVO;
import com.badminton.mes.module.system.controller.vo.AuthRegisterReqVO;
import com.badminton.mes.module.system.dal.entity.RoleEntity;
import com.badminton.mes.module.system.dal.entity.UserEntity;
import com.badminton.mes.module.system.dal.entity.UserRoleEntity;
import com.badminton.mes.module.system.dal.entity.WechatUserBindingEntity;
import com.badminton.mes.module.system.dal.redis.LoginSessionRedisDAO;
import com.badminton.mes.module.system.dal.redis.SystemRedisKeyConstants;
import com.badminton.mes.module.system.dal.repository.RoleRepository;
import com.badminton.mes.module.system.dal.repository.UserRepository;
import com.badminton.mes.module.system.dal.repository.UserRoleRepository;
import com.badminton.mes.module.system.service.AuthService;
import com.badminton.mes.module.system.service.AuthenticationSupport;
import com.badminton.mes.module.system.service.RoleService;
import com.badminton.mes.module.system.service.WechatUserBindingService;

import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/**
 * 认证 Service 实现。
 *
 * <p>登录失败原因(账户不存在/密码错误)只体现在服务端 warn 日志，
 * 响应统一为 A0200"工号或密码错误"；日志不记录任何密码内容。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final int TOKEN_BYTES = 16;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;

    private final UserRoleRepository userRoleRepository;

    private final RoleRepository roleRepository;

    private final LoginSessionRedisDAO loginSessionRedisDAO;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationSupport authenticationSupport;

    private final RoleService roleService;

    private final WechatUserBindingService wechatUserBindingService;

    /**
     * 构造器注入，依赖不可变。
     *
     * @param userRepository       用户 Repository
     * @param userRoleRepository   用户角色关系 Repository
     * @param roleRepository       角色 Repository
     * @param loginSessionRedisDAO 登录会话 DAO
     * @param passwordEncoder      密码编码器
     * @param authenticationSupport 认证公共能力
     */
    public AuthServiceImpl(UserRepository userRepository, UserRoleRepository userRoleRepository,
                           RoleRepository roleRepository, LoginSessionRedisDAO loginSessionRedisDAO,
                           PasswordEncoder passwordEncoder, AuthenticationSupport authenticationSupport,
                           RoleService roleService, WechatUserBindingService wechatUserBindingService) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.loginSessionRedisDAO = loginSessionRedisDAO;
        this.passwordEncoder = passwordEncoder;
        this.authenticationSupport = authenticationSupport;
        this.roleService = roleService;
        this.wechatUserBindingService = wechatUserBindingService;
    }

    @Override
    public AuthLoginRespVO login(AuthLoginReqVO reqVO) {
        AuthLoginRespVO response;
        if (authenticationSupport == null) {
            response = loginWithLocalDependencies(reqVO);
        } else {
            UserEntity user = authenticationSupport.authenticate(reqVO.getUserNo(), reqVO.getPassword());
            response = authenticationSupport.createSession(user);
            logger.info("[登录成功] userId: {}, userNo: {}", user.getId(), user.getUserNo());
        }
        applyWechatBinding(response, response.getUserId());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(AuthRegisterReqVO reqVO) {
        if (userRepository.existsByUserNoAndDeletedFalse(reqVO.getUserNo())) {
            throw new ServiceException(SystemErrorCodeConstants.USER_NO_DUPLICATE);
        }
        if (!roleService.isRegistrationRole(reqVO.getRoleId())) {
            throw new ServiceException(SystemErrorCodeConstants.USER_ROLE_INVALID);
        }

        UserEntity user = new UserEntity();
        user.setUserNo(reqVO.getUserNo());
        user.setUserName(reqVO.getUserName());
        user.setPassword(passwordEncoder.encode(reqVO.getPassword()));
        user.setStatus(CommonStatusEnum.ENABLED.getStatus());
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new ServiceException(SystemErrorCodeConstants.USER_NO_DUPLICATE);
        }

        UserRoleEntity relation = new UserRoleEntity();
        relation.setUserId(user.getId());
        relation.setRoleId(reqVO.getRoleId());
        userRoleRepository.save(relation);
        logger.info("[小程序注册成功] userId: {}, userNo: {}, roleId: {}",
                user.getId(), user.getUserNo(), reqVO.getRoleId());
        return user.getId();
    }

    private AuthLoginRespVO loginWithLocalDependencies(AuthLoginReqVO reqVO) {
        if (loginSessionRedisDAO.isLoginLocked(reqVO.getUserNo())) {
            throw new ServiceException(SystemErrorCodeConstants.LOGIN_LOCKED);
        }
        UserEntity user = userRepository.findByUserNoAndDeletedFalse(reqVO.getUserNo()).orElse(null);
        if (user == null) {
            throw loginFail(reqVO.getUserNo(), "账户不存在");
        }
        if (!passwordEncoder.matches(reqVO.getPassword(), user.getPassword())) {
            throw loginFail(reqVO.getUserNo(), "密码错误");
        }
        if (!CommonStatusEnum.ENABLED.getStatus().equals(user.getStatus())) {
            throw new ServiceException(SystemErrorCodeConstants.LOGIN_USER_DISABLED);
        }
        loginSessionRedisDAO.clearLoginFail(reqVO.getUserNo());

        List<RoleEntity> roles = loadEnabledRoles(user.getId());
        LoginUser loginUser = buildLoginUser(user, roles);
        String token = generateToken();
        loginSessionRedisDAO.createSession(token, loginUser);

        AuthLoginRespVO response = new AuthLoginRespVO();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUserNo(user.getUserNo());
        response.setUserName(user.getUserName());
        response.setRoleCodes(loginUser.getRoleCodes());
        return response;
    }

    @Override
    public void logout() {
        String token = SecurityContextHolder.getToken();
        if (StringUtils.hasText(token)) {
            loginSessionRedisDAO.removeSession(token);
        }
    }

    @Override
    public AuthProfileRespVO getProfile() {
        Long userId = SecurityContextHolder.getRequiredLoginUserId();
        UserEntity user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ServiceException(SystemErrorCodeConstants.USER_NOT_EXISTS));
        List<RoleEntity> roles = loadEnabledRoles(userId);

        AuthProfileRespVO respVO = new AuthProfileRespVO();
        respVO.setUserId(user.getId());
        respVO.setUserNo(user.getUserNo());
        respVO.setUserName(user.getUserName());
        respVO.setMobile(DesensitizeUtils.maskMobile(user.getMobile()));
        respVO.setWorkshopId(user.getWorkshopId());
        respVO.setLineId(user.getLineId());
        respVO.setRoleCodes(roles.stream().map(RoleEntity::getRoleCode).toList());
        respVO.setRoleNames(roles.stream().map(RoleEntity::getRoleName).toList());
        Optional<WechatUserBindingEntity> binding = findWechatBinding(userId);
        respVO.setWechatBound(binding.isPresent());
        binding.ifPresent(value -> {
            respVO.setWechatBindingTime(value.getCreateTime());
            respVO.setWechatLastLoginTime(value.getLastLoginTime());
        });
        return respVO;
    }

    private void applyWechatBinding(AuthLoginRespVO response, Long userId) {
        Optional<WechatUserBindingEntity> binding = findWechatBinding(userId);
        response.setWechatBound(binding.isPresent());
        binding.ifPresent(value -> {
            response.setWechatBindingTime(value.getCreateTime());
            response.setWechatLastLoginTime(value.getLastLoginTime());
        });
    }

    private Optional<WechatUserBindingEntity> findWechatBinding(Long userId) {
        if (wechatUserBindingService == null) {
            return Optional.empty();
        }
        return wechatUserBindingService.findActiveByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(AuthPasswordReqVO reqVO) {
        Long userId = SecurityContextHolder.getRequiredLoginUserId();
        UserEntity user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ServiceException(SystemErrorCodeConstants.USER_NOT_EXISTS));
        if (!passwordEncoder.matches(reqVO.getOldPassword(), user.getPassword())) {
            throw new ServiceException(SystemErrorCodeConstants.USER_OLD_PASSWORD_MISMATCH);
        }
        user.setPassword(passwordEncoder.encode(reqVO.getNewPassword()));
        userRepository.save(user);
        // 改密提交后再下线：避免事务回滚后密码未变却误踢用户
        evictSessionAfterCommit(userId);
        logger.info("[修改密码] userId: {}", userId);
    }

    /**
     * 查询用户当前有效且启用的角色，已停用角色不参与鉴权。
     *
     * @param userId 用户主键
     * @return 启用角色列表，按 id 升序
     */
    private List<RoleEntity> loadEnabledRoles(Long userId) {
        List<Long> roleIds = userRoleRepository.findByUserIdAndDeletedFalse(userId).stream()
                .map(UserRoleEntity::getRoleId)
                .toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleRepository.findByIdInAndDeletedFalseOrderByIdAsc(roleIds).stream()
                .filter(role -> CommonStatusEnum.ENABLED.getStatus().equals(role.getStatus()))
                .toList();
    }

    private LoginUser buildLoginUser(UserEntity user, List<RoleEntity> roles) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUserNo(user.getUserNo());
        loginUser.setUserName(user.getUserName());
        loginUser.setWorkshopId(user.getWorkshopId());
        loginUser.setLineId(user.getLineId());
        loginUser.setRoleCodes(roles.stream().map(RoleEntity::getRoleCode).toList());
        return loginUser;
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private ServiceException loginFail(String userNo, String reason) {
        long failCount = loginSessionRedisDAO.recordLoginFail(userNo);
        logger.warn("[登录失败] userNo: {}, 原因: {}", userNo, reason);
        if (failCount >= SystemRedisKeyConstants.LOGIN_FAIL_MAX) {
            return new ServiceException(SystemErrorCodeConstants.LOGIN_LOCKED);
        }
        return new ServiceException(SystemErrorCodeConstants.LOGIN_CREDENTIALS_INVALID);
    }

    /**
     * 事务提交后再删除会话：避免事务回滚时数据未变却误踢用户。
     * 无事务上下文时(理论上不会发生在 @Transactional 方法中)直接删除。
     *
     * @param userId 待下线用户主键
     */
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
