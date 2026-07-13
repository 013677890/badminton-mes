package com.badminton.mes.module.integration.controller;

import java.lang.reflect.Method;

import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.integration.controller.vo.ErpCraftSyncReqVO;
import com.badminton.mes.module.integration.controller.vo.ErpSyncLogPageReqVO;
import com.badminton.mes.module.integration.controller.vo.ErpTaskSyncReqVO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ErpSyncController} 方法级角色访问契约测试。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
class ErpSyncControllerRoleTest {

    @Test
    @DisplayName("ERP 任务接口：仅管理员和 PMC 可访问")
    void taskEndpointsRequireAdminOrPmc() throws NoSuchMethodException {
        assertRoles(
                ErpSyncController.class.getMethod(
                        "syncErpTasks", ErpTaskSyncReqVO.class),
                RoleCodeConstants.ADMIN,
                RoleCodeConstants.PMC);
        assertRoles(
                ErpSyncController.class.getMethod(
                        "getErpTaskSyncLogs", ErpSyncLogPageReqVO.class),
                RoleCodeConstants.ADMIN,
                RoleCodeConstants.PMC);
    }

    @Test
    @DisplayName("ERP 工艺接口：仅管理员和工艺工程师可访问")
    void craftEndpointsRequireAdminOrCraftEngineer() throws NoSuchMethodException {
        assertRoles(
                ErpSyncController.class.getMethod(
                        "syncErpCrafts", ErpCraftSyncReqVO.class),
                RoleCodeConstants.ADMIN,
                RoleCodeConstants.CRAFT_ENGINEER);
        assertRoles(
                ErpSyncController.class.getMethod(
                        "confirmPendingCraft", Long.class),
                RoleCodeConstants.ADMIN,
                RoleCodeConstants.CRAFT_ENGINEER);
    }

    /**
     * 断言 Controller 方法声明了预期角色且没有额外放宽访问范围。
     *
     * @param method        Controller 方法
     * @param expectedRoles 预期角色
     */
    private void assertRoles(Method method, String... expectedRoles) {
        RequiresRoles annotation = method.getAnnotation(RequiresRoles.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).containsExactlyInAnyOrder(expectedRoles);
    }
}
