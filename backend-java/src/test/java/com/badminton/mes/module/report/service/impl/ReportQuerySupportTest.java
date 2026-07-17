package com.badminton.mes.module.report.service.impl;

import java.time.LocalDateTime;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.report.controller.vo.ReportQueryReqVO;
import com.badminton.mes.module.report.service.ReportDataScopeService;
import com.badminton.mes.module.report.service.ReportDataScopeService.ReportDataScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * M4 查询和同步导出范围保护测试。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
class ReportQuerySupportTest {

    private final ReportDataScopeService dataScopeService = mock(ReportDataScopeService.class);
    private final ReportQuerySupport support = new ReportQuerySupport(dataScopeService);

    @Test
    void criteriaUsesServerResolvedScope() {
        ReportQueryReqVO reqVO = request(10);
        reqVO.setWorkshopId(10L);
        reqVO.setLineId(20L);
        when(dataScopeService.resolve(10L, 20L)).thenReturn(new ReportDataScope(10L, 20L));

        assertThat(support.criteria(reqVO).workshopId()).isEqualTo(10L);
        assertThat(support.criteria(reqVO).lineId()).isEqualTo(20L);
    }

    @Test
    void exportRejectsMoreThanThirtyOneDays() {
        ReportQueryReqVO reqVO = request(32);

        assertThatThrownBy(() -> support.exportCriteria(reqVO)).isInstanceOf(ServiceException.class);
    }

    @Test
    void queryRejectsReversedTimeRange() {
        ReportQueryReqVO reqVO = request(1);
        reqVO.setEndTime(reqVO.getStartTime().minusSeconds(1));

        assertThatThrownBy(() -> support.criteria(reqVO)).isInstanceOf(ServiceException.class);
    }

    private ReportQueryReqVO request(int days) {
        ReportQueryReqVO reqVO = new ReportQueryReqVO();
        reqVO.setStartTime(LocalDateTime.of(2026, 7, 1, 0, 0));
        reqVO.setEndTime(reqVO.getStartTime().plusDays(days));
        return reqVO;
    }
}
