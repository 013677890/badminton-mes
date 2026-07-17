package com.badminton.mes.module.report.controller;

import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.module.report.controller.vo.ReportQueryReqVO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 报表查询接口登录后全员可用的权限契约测试。
 *
 * @author Codex
 * @date 2026/07/17
 */
class ReportQueryPermissionPolicyTest {

    @Test
    void reportExportsHaveNoRoleRestriction() throws NoSuchMethodException {
        assertThat(ProductionOutputReportController.class
                .getMethod("export", ReportQueryReqVO.class)
                .getAnnotation(RequiresRoles.class)).isNull();
        assertThat(WorkshopPeriodReportController.class
                .getMethod("export", ReportQueryReqVO.class)
                .getAnnotation(RequiresRoles.class)).isNull();
        assertThat(DefectQueryController.class
                .getMethod("export", ReportQueryReqVO.class)
                .getAnnotation(RequiresRoles.class)).isNull();
    }
}
