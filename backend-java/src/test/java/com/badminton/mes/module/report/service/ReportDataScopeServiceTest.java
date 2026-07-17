package com.badminton.mes.module.report.service;

import java.util.List;

import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.common.security.SecurityContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * M4 报表车间、产线查询范围测试。
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
    void teamLeaderCanQueryAnotherWorkshopAndLine() {
        login(RoleCodeConstants.TEAM_LEADER, null, null);

        assertThat(service.resolve(11L, 99L))
                .isEqualTo(new ReportDataScopeService.ReportDataScope(11L, 99L));
    }

    @Test
    void operatorCanQueryRequestedScope() {
        login(RoleCodeConstants.OPERATOR, 10L, 20L);

        assertThat(service.resolve(12L, 21L))
                .isEqualTo(new ReportDataScopeService.ReportDataScope(12L, 21L));
    }

    @Test
    void userWithoutOrganizationCanQueryAllData() {
        login(RoleCodeConstants.PMC, null, null);

        assertThat(service.resolve(null, null))
                .isEqualTo(new ReportDataScopeService.ReportDataScope(null, null));
    }

    @Test
    void authenticatedUserCanQueryTraceObjectOutsideOwnOrganization() {
        login(RoleCodeConstants.INSPECTOR, 10L, 20L);

        service.checkObject(11L, 21L);
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
