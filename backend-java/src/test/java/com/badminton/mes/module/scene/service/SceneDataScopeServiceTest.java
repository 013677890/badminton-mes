package com.badminton.mes.module.scene.service;

import java.util.List;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

/** scene 对象级数据权限测试。 @author 刘涵 */
class SceneDataScopeServiceTest {
    private final SceneDataScopeService service = new SceneDataScopeService();
    @AfterEach void tearDown() { SecurityContextHolder.clear(); }

    @Test void operatorCannotAccessAnotherLine() {
        LoginUser user = new LoginUser();
        user.setUserId(1L); user.setWorkshopId(10L); user.setLineId(20L);
        user.setRoleCodes(List.of(RoleCodeConstants.OPERATOR));
        SecurityContextHolder.set("token", user);
        assertThatThrownBy(() -> service.check(10L, 21L)).isInstanceOf(ServiceException.class);
    }

    @Test void workshopManagerCanAccessAnyLineInOwnWorkshop() {
        LoginUser user = new LoginUser();
        user.setUserId(1L); user.setWorkshopId(10L); user.setLineId(20L);
        user.setRoleCodes(List.of(RoleCodeConstants.WORKSHOP_MANAGER));
        SecurityContextHolder.set("token", user);
        assertThatCode(() -> service.check(10L, 99L)).doesNotThrowAnyException();
    }
}
