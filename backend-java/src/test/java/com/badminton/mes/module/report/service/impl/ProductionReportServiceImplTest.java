package com.badminton.mes.module.report.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.report.controller.vo.ProductionReportRespVO;
import com.badminton.mes.module.report.controller.vo.ReportQueryReqVO;
import com.badminton.mes.module.report.dal.ReportQueryRepository;
import com.badminton.mes.module.report.dal.ReportQueryRows.Aggregate;
import com.badminton.mes.module.report.dal.ReportQueryRows.ReportDetail;
import com.badminton.mes.module.report.service.dto.ReportQueryCriteria;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 产量报表净额、审计明细和导出保护测试。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
class ProductionReportServiceImplTest {

    private final ReportQueryRepository repository = mock(ReportQueryRepository.class);
    private final ReportQuerySupport querySupport = mock(ReportQuerySupport.class);
    private final ProductionReportServiceImpl service = new ProductionReportServiceImpl(repository, querySupport);
    private final ReportQueryCriteria criteria = new ReportQueryCriteria(null, null, null, null,
            null, null, null, null, null, null, null);

    @Test
    void summaryReturnsNetAndOccurrenceReversalValues() {
        ReportQueryReqVO reqVO = new ReportQueryReqVO();
        when(querySupport.criteria(reqVO)).thenReturn(criteria);
        when(repository.aggregate(criteria)).thenReturn(new Aggregate(100, 80, 70, 10, 5, 60,
                90, 10, 75, 5, 12, 2));

        ProductionReportRespVO.Summary result = service.summary(reqVO);

        assertThat(result.getInputQuantity()).isEqualTo(80);
        assertThat(result.getOccurrenceInputQuantity()).isEqualTo(90);
        assertThat(result.getReversalInputQuantity()).isEqualTo(10);
        assertThat(result.getDefectRate()).isEqualByComparingTo("0.1250");
    }

    @Test
    void detailShowsReversalAsNegativeNet() {
        ReportQueryReqVO reqVO = new ReportQueryReqVO();
        when(querySupport.criteria(reqVO)).thenReturn(criteria);
        ReportDetail reversal = detail(2, 5);
        when(repository.pageReports(criteria, 1, 10))
                .thenReturn(PageResult.of(List.of(reversal), 1L, 1, 10));

        ProductionReportRespVO.Detail result = service.details(reqVO).getList().getFirst();

        assertThat(result.getOccurrenceInputQuantity()).isZero();
        assertThat(result.getReversalInputQuantity()).isEqualTo(5);
        assertThat(result.getNetInputQuantity()).isEqualTo(-5);
    }

    @Test
    void exportRejectsMoreThanTenThousandRows() {
        ReportQueryReqVO reqVO = new ReportQueryReqVO();
        when(querySupport.exportCriteria(reqVO)).thenReturn(criteria);
        when(repository.listReports(criteria, ReportQuerySupport.EXPORT_MAX_ROWS + 1))
                .thenReturn(java.util.Collections.nCopies(ReportQuerySupport.EXPORT_MAX_ROWS + 1, detail(1, 1)));

        assertThatThrownBy(() -> service.export(reqVO, "production"))
                .isInstanceOf(ServiceException.class);
    }

    private ReportDetail detail(int recordType, int quantity) {
        return new ReportDetail(1L, "R1", 2L, "T1", "W1", 3L, "产品", "B1",
                4L, "车间", 5L, "产线", 6L, "工序", recordType, recordType == 2 ? 9L : null,
                quantity, quantity, 0, 0, recordType == 2 ? "录入错误" : null, LocalDateTime.now());
    }
}
