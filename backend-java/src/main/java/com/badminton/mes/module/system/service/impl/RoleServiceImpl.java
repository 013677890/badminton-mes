package com.badminton.mes.module.system.service.impl;

import java.util.List;
import java.util.Set;

import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.system.constants.SystemErrorCodeConstants;
import com.badminton.mes.module.system.controller.vo.RoleRespVO;
import com.badminton.mes.module.system.controller.vo.RoleUserRespVO;
import com.badminton.mes.module.system.dal.entity.RoleEntity;
import com.badminton.mes.module.system.dal.entity.UserEntity;
import com.badminton.mes.module.system.dal.entity.UserRoleEntity;
import com.badminton.mes.module.system.dal.repository.RoleRepository;
import com.badminton.mes.module.system.dal.repository.UserRepository;
import com.badminton.mes.module.system.dal.repository.UserRoleRepository;
import com.badminton.mes.module.system.service.RoleService;

import org.springframework.stereotype.Service;

/**
 * 系统角色 Service 实现，仅查询。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Service
public class RoleServiceImpl implements RoleService {

    private static final Set<String> REGISTRATION_ROLE_CODES = Set.of(
            RoleCodeConstants.TEAM_LEADER,
            RoleCodeConstants.OPERATOR,
            RoleCodeConstants.INSPECTOR,
            RoleCodeConstants.CRAFT_ENGINEER
    );

    private final RoleRepository roleRepository;

    private final UserRoleRepository userRoleRepository;

    private final UserRepository userRepository;

    /**
     * 构造器注入，依赖不可变。
     *
     * @param roleRepository     角色 Repository
     * @param userRoleRepository 用户角色关系 Repository
     * @param userRepository     用户 Repository
     */
    public RoleServiceImpl(RoleRepository roleRepository, UserRoleRepository userRoleRepository,
                           UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<RoleRespVO> getEnabledRoles() {
        return roleRepository.findByStatusAndDeletedFalseOrderByIdAsc(CommonStatusEnum.ENABLED.getStatus())
                .stream()
                .map(this::toRespVO)
                .toList();
    }

    @Override
    public List<RoleRespVO> getRegistrationRoles() {
        return roleRepository.findByStatusAndDeletedFalseOrderByIdAsc(CommonStatusEnum.ENABLED.getStatus())
                .stream()
                .filter(role -> REGISTRATION_ROLE_CODES.contains(role.getRoleCode()))
                .map(this::toRespVO)
                .toList();
    }

    @Override
    public boolean isRegistrationRole(Long roleId) {
        return roleRepository.findById(roleId)
                .filter(role -> !Boolean.TRUE.equals(role.getDeleted()))
                .filter(role -> CommonStatusEnum.ENABLED.getStatus().equals(role.getStatus()))
                .map(RoleEntity::getRoleCode)
                .filter(REGISTRATION_ROLE_CODES::contains)
                .isPresent();
    }

    @Override
    public List<RoleUserRespVO> getRoleUsers(Long roleId) {
        roleRepository.findById(roleId)
                .filter(role -> !Boolean.TRUE.equals(role.getDeleted()))
                .orElseThrow(() -> new ServiceException(SystemErrorCodeConstants.ROLE_NOT_EXISTS));
        List<Long> userIds = userRoleRepository.findByRoleIdAndDeletedFalse(roleId).stream()
                .map(UserRoleEntity::getUserId)
                .toList();
        if (userIds.isEmpty()) {
            return List.of();
        }
        // 只返回启用用户：停用人员不应再被派单/通知
        return userRepository.findByIdInAndDeletedFalse(userIds).stream()
                .filter(user -> CommonStatusEnum.ENABLED.getStatus().equals(user.getStatus()))
                .map(this::toRoleUserRespVO)
                .toList();
    }

    /**
     * 角色实体转响应 VO。
     *
     * @param role 角色实体
     * @return 响应 VO
     */
    private RoleRespVO toRespVO(RoleEntity role) {
        RoleRespVO respVO = new RoleRespVO();
        respVO.setId(role.getId());
        respVO.setRoleCode(role.getRoleCode());
        respVO.setRoleName(role.getRoleName());
        respVO.setRemark(role.getRemark());
        respVO.setStatus(role.getStatus());
        return respVO;
    }

    /**
     * 用户实体转按角色反查的轻量 VO，不含手机号等敏感字段。
     *
     * @param user 用户实体
     * @return 轻量响应 VO
     */
    private RoleUserRespVO toRoleUserRespVO(UserEntity user) {
        RoleUserRespVO respVO = new RoleUserRespVO();
        respVO.setUserId(user.getId());
        respVO.setUserNo(user.getUserNo());
        respVO.setUserName(user.getUserName());
        respVO.setWorkshopId(user.getWorkshopId());
        respVO.setLineId(user.getLineId());
        return respVO;
    }
}
