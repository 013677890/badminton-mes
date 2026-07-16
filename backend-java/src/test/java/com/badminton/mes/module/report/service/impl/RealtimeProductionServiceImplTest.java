package com.badminton.mes.module.report.service.impl;

import java.util.List;

import com.badminton.mes.module.report.controller.vo.RealtimeReportQueryReqVO;
import com.badminton.mes.module.report.dal.ReportQueryRepository;
import com.badminton.mes.module.report.dal.ReportQueryRows.RealtimeSupport;
import com.badminton.mes.module.report.service.ReportDataScopeService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RealtimeProductionServiceImplTest {

    @Test
    void kanbanOverviewUsesSystemScopeWithoutLoginContext() {
        ReportQueryRepository repository = mock(ReportQueryRepository.class);
        ReportDataScopeService dataScopeService = mock(ReportDataScopeService.class);
        when(repository.listRealtimeTasks(null, null, null)).thenReturn(List.of());
        when(repository.loadRealtimeSupport(null, null)).thenReturn(new RealtimeSupport(3, 2, 1, 4, 1));
        RealtimeProductionServiceImpl service = new RealtimeProductionServiceImpl(repository, dataScopeService);

        var result = service.overviewForKanban(new RealtimeReportQueryReqVO());

        assertThat(result.getEquipmentTotalCount()).isEqualTo(3);
        assertThat(result.getOpenAndonCount()).isEqualTo(4);
        verify(repository).listRealtimeTasks(null, null, null);
        verifyNoInteractions(dataScopeService);
    }
}
