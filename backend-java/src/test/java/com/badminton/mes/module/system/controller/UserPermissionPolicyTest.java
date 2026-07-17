package com.badminton.mes.module.system.controller;

import java.lang.reflect.Method;

import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.production.controller.ProductionLineController;
import com.badminton.mes.module.production.controller.WorkshopController;
import com.badminton.mes.module.system.controller.vo.UserAssignmentReqVO;
import com.badminton.mes.module.system.controller.vo.UserPageReqVO;
import com.badminton.mes.module.system.controller.vo.UserPasswordResetReqVO;
import com.badminton.mes.module.system.controller.vo.UserSaveReqVO;
import com.badminton.mes.module.system.controller.vo.UserStatusReqVO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 微信小程序用户查询与职位分配权限契约测试。
 *
 * @author Codex
 * @date 2026/07/17
 */
class UserPermissionPolicyTest {

    @Test
    void queryAndAssignmentEndpointsRequireLoginOnly() throws NoSuchMethodException {
        assertThat(UserController.class.getAnnotation(RequiresRoles.class)).isNull();
        assertLoginOnly(UserController.class.getMethod("getUser", Long.class));
        assertLoginOnly(UserController.class.getMethod("getUserPage", UserPageReqVO.class));
        assertLoginOnly(UserController.class.getMethod(
                "updateUserAssignment", Long.class, UserAssignmentReqVO.class));
        assertLoginOnly(RoleController.class.getMethod("getEnabledRoles"));
        assertThat(WorkshopController.class.getAnnotation(RequiresRoles.class)).isNull();
        assertThat(ProductionLineController.class.getAnnotation(RequiresRoles.class)).isNull();
    }

    @Test
    void accountAdministrationEndpointsRemainAdminOnly() throws NoSuchMethodException {
        assertAdminOnly(UserController.class.getMethod("createUser", UserSaveReqVO.class));
        assertAdminOnly(UserController.class.getMethod("updateUser", Long.class, UserSaveReqVO.class));
        assertAdminOnly(UserController.class.getMethod("deleteUser", Long.class));
        assertAdminOnly(UserController.class.getMethod(
                "updateUserStatus", Long.class, UserStatusReqVO.class));
        assertAdminOnly(UserController.class.getMethod(
                "resetPassword", Long.class, UserPasswordResetReqVO.class));
    }

    private void assertLoginOnly(Method method) {
        assertThat(method.getAnnotation(RequiresRoles.class)).isNull();
    }

    private void assertAdminOnly(Method method) {
        RequiresRoles requiresRoles = method.getAnnotation(RequiresRoles.class);
        assertThat(requiresRoles).isNotNull();
        assertThat(requiresRoles.value()).containsExactly(RoleCodeConstants.ADMIN);
    }
}
