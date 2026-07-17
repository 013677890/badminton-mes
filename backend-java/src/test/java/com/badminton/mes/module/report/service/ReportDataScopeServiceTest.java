package com.badminton.mes.module.report.service;

import java.util.List;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.common.security.SecurityContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * M4 报表车间、产线数据权限测试。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
class ReportDataScopeServiceTest {

    private final ReportDataScopeService service = new ReportDataScopeService();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    void adminCanQueryRequestedScope() {
        login(RoleCodeConstants.ADMIN, null, null);

        assertThat(service.resolve(10L, 20L))
                .isEqualTo(new ReportDataScopeService.ReportDataScope(10L, 20L));
    }

    @Test
    void workshopManagerCanQueryAnotherLineInOwnWorkshop() {
        login(RoleCodeConstants.WORKSHOP_MANAGER, 10L, 20L);

        assertThat(service.resolve(10L, 99L))
                .isEqualTo(new ReportDataScopeService.ReportDataScope(10L, 99L));
    }

    @Test
    void operatorCannotExpandToAnotherLine() {
        login(RoleCodeConstants.OPERATOR, 10L, 20L);

        assertThatThrownBy(() -> service.resolve(10L, 21L)).isInstanceOf(ServiceException.class);
    }

    @Test
    void nonAdminWithoutWorkshopIsDenied() {
        login(RoleCodeConstants.PMC, null, null);

        assertThatThrownBy(() -> service.resolve(null, null)).isInstanceOf(ServiceException.class);
    }

    @Test
    void pmcWithoutLineCanQueryWholeOwnWorkshop() {
        login(RoleCodeConstants.PMC, 10L, null);

        assertThat(service.resolve(null, null))
                .isEqualTo(new ReportDataScopeService.ReportDataScope(10L, null));
    }

    private void login(String role, Long workshopId, Long lineId) {
        LoginUser user = new LoginUser();
        user.setUserId(1L);
        user.setWorkshopId(workshopId);
        user.setLineId(lineId);
        user.setRoleCodes(List.of(role));
        SecurityContextHolder.set("m4-scope", user);
    }
}
