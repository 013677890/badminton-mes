package com.badminton.mes.module.system.service.impl;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.system.constants.SystemErrorCodeConstants;
import com.badminton.mes.module.system.controller.vo.RoleRespVO;
import com.badminton.mes.module.system.controller.vo.RoleUserRespVO;
import com.badminton.mes.module.system.dal.entity.RoleEntity;
import com.badminton.mes.module.system.dal.entity.UserEntity;
import com.badminton.mes.module.system.dal.entity.UserRoleEntity;
import com.badminton.mes.module.system.dal.repository.RoleRepository;
import com.badminton.mes.module.system.dal.repository.UserRepository;
import com.badminton.mes.module.system.dal.repository.UserRoleRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RoleServiceImpl} 单元测试。
 *
 * <p>覆盖角色查询与按角色反查启用用户，依赖全部 Mock。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    private static final Long ROLE_ID = 2L;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    @Test
    @DisplayName("查询启用角色：返回列表并转 VO")
    void getEnabledRolesReturnsList() {
        RoleEntity role = buildRole(ROLE_ID, "PMC", "PMC计划员", 1);
        when(roleRepository.findByStatusAndDeletedFalseOrderByIdAsc(CommonStatusEnum.ENABLED.getStatus()))
                .thenReturn(List.of(role));

        List<RoleRespVO> result = roleService.getEnabledRoles();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRoleCode()).isEqualTo("PMC");
        assertThat(result.get(0).getRoleName()).isEqualTo("PMC计划员");
    }

    @Test
    @DisplayName("查询启用角色：无数据时返回空集合")
    void getEnabledRolesReturnsEmpty() {
        when(roleRepository.findByStatusAndDeletedFalseOrderByIdAsc(CommonStatusEnum.ENABLED.getStatus()))
                .thenReturn(List.of());

        List<RoleRespVO> result = roleService.getEnabledRoles();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("按角色反查用户：返回启用用户列表")
    void getRoleUsersReturnsEnabledUsers() {
        RoleEntity role = buildRole(ROLE_ID, "PMC", "PMC计划员", 1);
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role));
        UserRoleEntity relation = new UserRoleEntity();
        relation.setUserId(10L);
        relation.setRoleId(ROLE_ID);
        when(userRoleRepository.findByRoleIdAndDeletedFalse(ROLE_ID)).thenReturn(List.of(relation));
        UserEntity user = buildUser(10L, "EMP010", "张三", 1);
        when(userRepository.findByIdInAndDeletedFalse(List.of(10L))).thenReturn(List.of(user));

        List<RoleUserRespVO> result = roleService.getRoleUsers(ROLE_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(10L);
        assertThat(result.get(0).getUserNo()).isEqualTo("EMP010");
    }

    @Test
    @DisplayName("按角色反查用户：角色不存在抛 A0402")
    void getRoleUsersRejectsMissingRole() {
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.getRoleUsers(ROLE_ID))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(SystemErrorCodeConstants.ROLE_NOT_EXISTS));
    }

    @Test
    @DisplayName("按角色反查用户：角色已逻辑删除抛 A0402")
    void getRoleUsersRejectsDeletedRole() {
        RoleEntity role = buildRole(ROLE_ID, "PMC", "PMC计划员", 1);
        role.setDeleted(true);
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role));

        assertThatThrownBy(() -> roleService.getRoleUsers(ROLE_ID))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(SystemErrorCodeConstants.ROLE_NOT_EXISTS));
    }

    @Test
    @DisplayName("按角色反查用户：过滤停用用户，只返回启用的")
    void getRoleUsersFiltersDisabledUsers() {
        RoleEntity role = buildRole(ROLE_ID, "PMC", "PMC计划员", 1);
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role));
        UserRoleEntity relation1 = new UserRoleEntity();
        relation1.setUserId(10L);
        relation1.setRoleId(ROLE_ID);
        UserRoleEntity relation2 = new UserRoleEntity();
        relation2.setUserId(11L);
        relation2.setRoleId(ROLE_ID);
        when(userRoleRepository.findByRoleIdAndDeletedFalse(ROLE_ID)).thenReturn(List.of(relation1, relation2));
        UserEntity enabledUser = buildUser(10L, "EMP010", "张三", 1);
        UserEntity disabledUser = buildUser(11L, "EMP011", "李四", 0);
        when(userRepository.findByIdInAndDeletedFalse(List.of(10L, 11L)))
                .thenReturn(List.of(enabledUser, disabledUser));

        List<RoleUserRespVO> result = roleService.getRoleUsers(ROLE_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("按角色反查用户：角色下无用户时返回空集合")
    void getRoleUsersReturnsEmptyWhenNoRelations() {
        RoleEntity role = buildRole(ROLE_ID, "PMC", "PMC计划员", 1);
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role));
        when(userRoleRepository.findByRoleIdAndDeletedFalse(ROLE_ID)).thenReturn(List.of());

        List<RoleUserRespVO> result = roleService.getRoleUsers(ROLE_ID);

        assertThat(result).isEmpty();
        verify(userRepository, org.mockito.Mockito.never()).findByIdInAndDeletedFalse(org.mockito.ArgumentMatchers.any());
    }

    /**
     * 构造角色实体。
     *
     * @param id       角色主键
     * @param code     角色编码
     * @param name     角色名称
     * @param status   状态(1 启用 0 停用)
     * @return 角色实体
     */
    private RoleEntity buildRole(Long id, String code, String name, int status) {
        RoleEntity role = new RoleEntity();
        role.setId(id);
        role.setRoleCode(code);
        role.setRoleName(name);
        role.setStatus(status);
        role.setDeleted(false);
        return role;
    }

    /**
     * 构造用户实体。
     *
     * @param id      用户主键
     * @param userNo  工号
     * @param name    姓名
     * @param status  状态(1 启用 0 停用)
     * @return 用户实体
     */
    private UserEntity buildUser(Long id, String userNo, String name, int status) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUserNo(userNo);
        user.setUserName(name);
        user.setStatus(status);
        user.setDeleted(false);
        return user;
    }
}
