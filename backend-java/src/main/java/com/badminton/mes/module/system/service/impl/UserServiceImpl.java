package com.badminton.mes.module.system.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.common.util.DesensitizeUtils;
import com.badminton.mes.module.system.constants.SystemErrorCodeConstants;
import com.badminton.mes.module.system.controller.vo.UserPageReqVO;
import com.badminton.mes.module.system.controller.vo.UserRespVO;
import com.badminton.mes.module.system.controller.vo.UserSaveReqVO;
import com.badminton.mes.module.system.dal.entity.RoleEntity;
import com.badminton.mes.module.system.dal.entity.UserEntity;
import com.badminton.mes.module.system.dal.entity.UserRoleEntity;
import com.badminton.mes.module.system.dal.redis.LoginSessionRedisDAO;
import com.badminton.mes.module.system.dal.repository.RoleRepository;
import com.badminton.mes.module.system.dal.repository.UserRepository;
import com.badminton.mes.module.system.dal.repository.UserRoleRepository;
import com.badminton.mes.module.system.dal.repository.UserSpecifications;
import com.badminton.mes.module.system.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/**
 * 系统用户 Service 实现。
 *
 * <p>停用/删除/重置密码/调整角色等会削弱既有会话权限的操作，
 * 一律按用户强制下线，避免旧会话继续持有原权限。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

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
    public UserServiceImpl(UserRepository userRepository, UserRoleRepository userRoleRepository,
                           RoleRepository roleRepository, LoginSessionRedisDAO loginSessionRedisDAO,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.loginSessionRedisDAO = loginSessionRedisDAO;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserSaveReqVO reqVO) {
        // 创建与修改共用 VO，初始密码仅创建时必填，由此在 Service 校验
        if (!StringUtils.hasText(reqVO.getPassword())) {
            throw new ServiceException(SystemErrorCodeConstants.USER_INIT_PASSWORD_REQUIRED);
        }
        if (userRepository.existsByUserNoAndDeletedFalse(reqVO.getUserNo())) {
            throw new ServiceException(SystemErrorCodeConstants.USER_NO_DUPLICATE);
        }
        validateRolesUsable(reqVO.getRoleIds());

        UserEntity user = new UserEntity();
        user.setUserNo(reqVO.getUserNo());
        user.setUserName(reqVO.getUserName());
        user.setPassword(passwordEncoder.encode(reqVO.getPassword()));
        user.setMobile(reqVO.getMobile());
        user.setWorkshopId(reqVO.getWorkshopId());
        user.setLineId(reqVO.getLineId());
        user.setStatus(CommonStatusEnum.ENABLED.getStatus());
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // 并发创建或工号被已删除用户占用时，由唯一索引 uk_user_no 兜底
            throw new ServiceException(SystemErrorCodeConstants.USER_NO_DUPLICATE);
        }
        replaceUserRoles(user.getId(), reqVO.getRoleIds());

        logger.info("[创建用户] id: {}, userNo: {}, operator: {}",
                user.getId(), user.getUserNo(), SecurityContextHolder.getRequiredLoginUserId());
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long id, UserSaveReqVO reqVO) {
        UserEntity user = validateUserExists(id);
        validateRolesUsable(reqVO.getRoleIds());

        // 工号不允许修改，密码走专用接口，两个字段均忽略
        user.setUserName(reqVO.getUserName());
        user.setMobile(reqVO.getMobile());
        user.setWorkshopId(reqVO.getWorkshopId());
        user.setLineId(reqVO.getLineId());
        userRepository.save(user);

        Set<Long> oldRoleIds = userRoleRepository.findByUserIdAndDeletedFalse(id).stream()
                .map(UserRoleEntity::getRoleId)
                .collect(Collectors.toSet());
        Set<Long> newRoleIds = new LinkedHashSet<>(reqVO.getRoleIds());
        if (!oldRoleIds.equals(newRoleIds)) {
            replaceUserRoles(id, newRoleIds);
            // 角色变化后旧会话仍持有原角色，提交后下线让新角色重新登录生效
            evictSessionAfterCommit(id);
        }
        logger.info("[修改用户] id: {}, operator: {}", id, SecurityContextHolder.getRequiredLoginUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        validateNotCurrentUser(id);
        UserEntity user = validateUserExists(id);
        user.setDeleted(true);
        userRepository.save(user);
        // 授权关系随用户失效
        userRoleRepository.logicDeleteByUserId(id);
        evictSessionAfterCommit(id);
        logger.info("[删除用户] id: {}, userNo: {}, operator: {}",
                id, user.getUserNo(), SecurityContextHolder.getRequiredLoginUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long id, Integer status) {
        if (CommonStatusEnum.DISABLED.getStatus().equals(status)) {
            validateNotCurrentUser(id);
        }
        UserEntity user = validateUserExists(id);
        user.setStatus(status);
        userRepository.save(user);
        if (CommonStatusEnum.DISABLED.getStatus().equals(status)) {
            // 停用即强制下线
            evictSessionAfterCommit(id);
        }
        logger.info("[变更用户状态] id: {}, status: {}, operator: {}",
                id, status, SecurityContextHolder.getRequiredLoginUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String newPassword) {
        UserEntity user = validateUserExists(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        // 重置后强制下线，持旧凭据的会话立即失效
        evictSessionAfterCommit(id);
        logger.info("[重置密码] id: {}, operator: {}", id, SecurityContextHolder.getRequiredLoginUserId());
    }

    @Override
    public UserRespVO getUser(Long id) {
        UserEntity user = validateUserExists(id);
        Map<Long, List<RoleEntity>> rolesByUser = loadRolesByUserIds(List.of(id));
        return toRespVO(user, rolesByUser.getOrDefault(id, List.of()));
    }

    @Override
    public PageResult<UserRespVO> getUserPage(UserPageReqVO reqVO) {
        Collection<Long> userIdsFilter = null;
        if (reqVO.getRoleId() != null) {
            userIdsFilter = userRoleRepository.findByRoleIdAndDeletedFalse(reqVO.getRoleId()).stream()
                    .map(UserRoleEntity::getUserId)
                    .collect(Collectors.toSet());
            if (userIdsFilter.isEmpty()) {
                return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
            }
        }
        Specification<UserEntity> specification = UserSpecifications.page(reqVO, userIdsFilter);
        // 先 count：总数为 0 直接返回空页，省一次列表查询(SQL-005)
        long total = userRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }

        // 请求页码超过总页数时按最后一页返回(API-009)
        int pageSize = reqVO.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        int pageNo = Math.min(reqVO.getPageNo(), totalPages);
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<UserEntity> page = userRepository.findAll(specification, pageRequest);

        List<Long> userIds = page.getContent().stream().map(UserEntity::getId).toList();
        Map<Long, List<RoleEntity>> rolesByUser = loadRolesByUserIds(userIds);
        List<UserRespVO> list = page.getContent().stream()
                .map(user -> toRespVO(user, rolesByUser.getOrDefault(user.getId(), List.of())))
                .toList();
        return PageResult.of(list, total, pageNo, pageSize);
    }

    /**
     * 校验用户存在且未删除。
     *
     * @param id 用户主键
     * @return 用户实体
     */
    private UserEntity validateUserExists(Long id) {
        return userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(SystemErrorCodeConstants.USER_NOT_EXISTS));
    }

    /**
     * 禁止对当前登录账号执行停用/删除，防止管理员自锁后无人恢复。
     *
     * @param id 目标用户主键
     */
    private void validateNotCurrentUser(Long id) {
        if (SecurityContextHolder.getRequiredLoginUserId().equals(id)) {
            throw new ServiceException(SystemErrorCodeConstants.USER_OPERATE_SELF_FORBIDDEN);
        }
    }

    /**
     * 校验待分配角色全部存在、未删除且启用。
     *
     * @param roleIds 角色主键列表
     */
    private void validateRolesUsable(List<Long> roleIds) {
        Set<Long> distinctIds = new HashSet<>(roleIds);
        List<RoleEntity> roles = roleRepository.findByIdInAndDeletedFalse(distinctIds);
        boolean allEnabled = roles.size() == distinctIds.size() && roles.stream()
                .allMatch(role -> CommonStatusEnum.ENABLED.getStatus().equals(role.getStatus()));
        if (!allEnabled) {
            throw new ServiceException(SystemErrorCodeConstants.USER_ROLE_INVALID);
        }
    }

    /**
     * 把用户角色关系对齐到目标集合。
     *
     * <p>uk_user_role 唯一键覆盖已逻辑删除的行，重新授予历史角色必须
     * "复活"旧行(deleted 置回 false)而不能新插入，否则触发唯一键冲突；
     * 不在目标集合中的有效行逻辑删除，全新角色才插入新行。
     *
     * @param userId      用户主键
     * @param newRoleIds 目标角色 id 集合
     */
    private void replaceUserRoles(Long userId, Collection<Long> newRoleIds) {
        Set<Long> pendingIds = new LinkedHashSet<>(newRoleIds);
        List<UserRoleEntity> changed = new ArrayList<>();
        for (UserRoleEntity relation : userRoleRepository.findByUserId(userId)) {
            boolean shouldHave = pendingIds.remove(relation.getRoleId());
            boolean currentlyDeleted = Boolean.TRUE.equals(relation.getDeleted());
            if (shouldHave && currentlyDeleted) {
                relation.setDeleted(false);
                changed.add(relation);
            } else if (!shouldHave && !currentlyDeleted) {
                relation.setDeleted(true);
                changed.add(relation);
            }
        }
        for (Long roleId : pendingIds) {
            UserRoleEntity relation = new UserRoleEntity();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            changed.add(relation);
        }
        if (!changed.isEmpty()) {
            userRoleRepository.saveAll(changed);
        }
    }

    /**
     * 批量装载多个用户的角色，避免分页列表逐行查询。
     *
     * @param userIds 用户主键集合
     * @return userId → 角色列表(按 id 升序)，无角色的用户不在 Map 中
     */
    private Map<Long, List<RoleEntity>> loadRolesByUserIds(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<UserRoleEntity> relations = userRoleRepository.findByUserIdInAndDeletedFalse(userIds);
        if (relations.isEmpty()) {
            return Map.of();
        }
        Set<Long> roleIds = relations.stream().map(UserRoleEntity::getRoleId).collect(Collectors.toSet());
        Map<Long, RoleEntity> roleById = roleRepository.findByIdInAndDeletedFalse(roleIds).stream()
                .collect(Collectors.toMap(RoleEntity::getId, Function.identity()));
        Map<Long, List<RoleEntity>> rolesByUser = relations.stream()
                .filter(relation -> roleById.containsKey(relation.getRoleId()))
                .collect(Collectors.groupingBy(UserRoleEntity::getUserId,
                        Collectors.mapping(relation -> roleById.get(relation.getRoleId()), Collectors.toList())));
        rolesByUser.values().forEach(roles -> roles.sort(Comparator.comparing(RoleEntity::getId)));
        return rolesByUser;
    }

    /**
     * 实体转响应 VO，手机号脱敏(SEC-002)，密码不出参。
     *
     * @param user  用户实体
     * @param roles 用户角色列表(按 id 升序)
     * @return 响应 VO
     */
    private UserRespVO toRespVO(UserEntity user, List<RoleEntity> roles) {
        UserRespVO respVO = new UserRespVO();
        respVO.setId(user.getId());
        respVO.setUserNo(user.getUserNo());
        respVO.setUserName(user.getUserName());
        respVO.setMobile(DesensitizeUtils.maskMobile(user.getMobile()));
        respVO.setWorkshopId(user.getWorkshopId());
        respVO.setLineId(user.getLineId());
        respVO.setStatus(user.getStatus());
        respVO.setRoleIds(roles.stream().map(RoleEntity::getId).toList());
        respVO.setRoleCodes(roles.stream().map(RoleEntity::getRoleCode).toList());
        respVO.setRoleNames(roles.stream().map(RoleEntity::getRoleName).toList());
        respVO.setCreateTime(user.getCreateTime());
        return respVO;
    }

    /**
     * 事务提交后再删除会话：避免事务回滚时数据未变却误踢用户，
     * 与 WorkOrderServiceImpl 缓存失效保持同一范式。
     * 无事务上下文时直接删除。
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
