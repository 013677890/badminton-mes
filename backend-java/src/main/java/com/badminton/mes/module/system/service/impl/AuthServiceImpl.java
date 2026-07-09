package com.badminton.mes.module.system.service.impl;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;

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
import com.badminton.mes.module.system.dal.entity.RoleEntity;
import com.badminton.mes.module.system.dal.entity.UserEntity;
import com.badminton.mes.module.system.dal.entity.UserRoleEntity;
import com.badminton.mes.module.system.dal.redis.LoginSessionRedisDAO;
import com.badminton.mes.module.system.dal.redis.SystemRedisKeyConstants;
import com.badminton.mes.module.system.dal.repository.RoleRepository;
import com.badminton.mes.module.system.dal.repository.UserRepository;
import com.badminton.mes.module.system.dal.repository.UserRoleRepository;
import com.badminton.mes.module.system.service.AuthService;

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

    /** token 随机字节数：128 位随机量，hex 编码后 32 字符 */
    private static final int TOKEN_BYTES = 16;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;

    private final UserRoleRepository userRoleRepository;

    private final RoleRepository roleRepository;

    private final LoginSessionRedisDAO loginSessionRedisDAO;

    private final PasswordEncoder passwordEncoder;

    /**
     * 构造器注入，依赖不可变。
     *
     * @param userRepository       用户 Repository
     * @param userRoleRepository   用户角色关系 Repository
     * @param roleRepository       角色 Repository
     * @param loginSessionRedisDAO 登录会话 DAO
     * @param passwordEncoder      密码编码器
     */
    public AuthServiceImpl(UserRepository userRepository, UserRoleRepository userRoleRepository,
                           RoleRepository roleRepository, LoginSessionRedisDAO loginSessionRedisDAO,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.loginSessionRedisDAO = loginSessionRedisDAO;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public AuthLoginRespVO login(AuthLoginReqVO reqVO) {
        // 锁定判断先于密码校验，锁定期内正确密码同样被拒，阻断继续爆破
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
            logger.warn("[登录失败] userNo: {}, 原因: 账户已停用", reqVO.getUserNo());
            throw new ServiceException(SystemErrorCodeConstants.LOGIN_USER_DISABLED);
        }
        loginSessionRedisDAO.clearLoginFail(reqVO.getUserNo());

        List<RoleEntity> roles = loadEnabledRoles(user.getId());
        LoginUser loginUser = buildLoginUser(user, roles);
        String token = generateToken();
        loginSessionRedisDAO.createSession(token, loginUser);
        logger.info("[登录成功] userId: {}, userNo: {}", user.getId(), user.getUserNo());

        AuthLoginRespVO respVO = new AuthLoginRespVO();
        respVO.setToken(token);
        respVO.setUserId(user.getId());
        respVO.setUserNo(user.getUserNo());
        respVO.setUserName(user.getUserName());
        respVO.setRoleCodes(loginUser.getRoleCodes());
        return respVO;
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
        return respVO;
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

    /**
     * 组装会话载荷。
     *
     * @param user  用户实体
     * @param roles 启用角色列表
     * @return 会话载荷
     */
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

    /**
     * 生成登录令牌：SecureRandom 128 位随机量的 hex 串，不含任何业务含义。
     *
     * @return 登录令牌
     */
    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /**
     * 记录登录失败并返回对应异常：达到锁定阈值返回 A0211，否则返回 A0200。
     *
     * <p>账户不存在与密码错误共用此方法，对外提示一致以防撞库；
     * 失败次数刚好达到阈值时直接告知已锁定，避免用户不知道被锁而继续尝试。
     *
     * @param userNo 工号
     * @param reason 服务端日志区分用失败原因
     * @return 待抛出的业务异常
     */
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
