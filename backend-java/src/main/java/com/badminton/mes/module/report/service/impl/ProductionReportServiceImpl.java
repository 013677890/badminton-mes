package com.badminton.mes.module.report.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.report.constants.ReportErrorCodeConstants;
import com.badminton.mes.module.report.controller.vo.ProductionReportRespVO;
import com.badminton.mes.module.report.controller.vo.ReportQueryReqVO;
import com.badminton.mes.module.report.dal.ReportQueryRepository;
import com.badminton.mes.module.report.dal.ReportQueryRows.Aggregate;
import com.badminton.mes.module.report.dal.ReportQueryRows.ReportDetail;
import com.badminton.mes.module.report.service.ProductionReportService;
import com.badminton.mes.module.report.service.ReportExportFile;
import com.badminton.mes.module.report.service.dto.ReportQueryCriteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 产量和车间时段报表实现，默认返回冲销后的净额。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
@Service
public class ProductionReportServiceImpl implements ProductionReportService {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ReportQueryRepository repository;
    private final ReportQuerySupport querySupport;

    public ProductionReportServiceImpl(ReportQueryRepository repository, ReportQuerySupport querySupport) {
        this.repository = repository;
        this.querySupport = querySupport;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductionReportRespVO.Summary summary(ReportQueryReqVO reqVO) {
        Aggregate row = repository.aggregate(querySupport.criteria(reqVO));
        ProductionReportRespVO.Summary result = new ProductionReportRespVO.Summary();
        result.setPlanQuantity(row.planQuantity());
        result.setInputQuantity(row.inputQuantity());
        result.setGoodQuantity(row.goodQuantity());
        result.setDefectQuantity(row.defectQuantity());
        result.setReworkQuantity(row.reworkQuantity());
        result.setFinishQuantity(row.finishQuantity());
        result.setOccurrenceInputQuantity(row.occurrenceInputQuantity());
        result.setReversalInputQuantity(row.reversalInputQuantity());
        result.setOccurrenceGoodQuantity(row.occurrenceGoodQuantity());
        result.setReversalGoodQuantity(row.reversalGoodQuantity());
        result.setOccurrenceDefectQuantity(row.occurrenceDefectQuantity());
        result.setReversalDefectQuantity(row.reversalDefectQuantity());
        result.setCompletionRate(rate(row.finishQuantity(), row.planQuantity()));
        result.setDefectRate(rate(row.defectQuantity(), row.inputQuantity()));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ProductionReportRespVO.Detail> details(ReportQueryReqVO reqVO) {
        PageResult<ReportDetail> page = repository.pageReports(querySupport.criteria(reqVO),
                reqVO.getPageNo(), reqVO.getPageSize());
        List<ProductionReportRespVO.Detail> rows = page.getList().stream().map(this::toDetail).toList();
        return PageResult.of(rows, page.getTotal(), page.getPageNo(), page.getPageSize());
    }

    @Override
    @Transactional(readOnly = true)
    public ReportExportFile export(ReportQueryReqVO reqVO, String filePrefix) {
        ReportQueryCriteria criteria = querySupport.exportCriteria(reqVO);
        List<ReportDetail> rows = repository.listReports(criteria, ReportQuerySupport.EXPORT_MAX_ROWS + 1);
        if (rows.size() > ReportQuerySupport.EXPORT_MAX_ROWS) {
            throw new ServiceException(ReportErrorCodeConstants.EXPORT_ROWS_EXCEEDED);
        }
        StringBuilder csv = new StringBuilder(1024 + rows.size() * 256);
        csv.append('\uFEFF').append("报工号,任务号,工单号,产品,批次,车间,产线,工序,记录类型,")
                .append("投入发生额,投入冲销额,投入净额,良品发生额,良品冲销额,良品净额,")
                .append("不良发生额,不良冲销额,不良净额,返修净额,报工时间\r\n");
        for (ReportDetail row : rows) {
            ProductionReportRespVO.Detail detail = toDetail(row);
            appendCsvRow(csv, Arrays.asList(row.reportNo(), row.taskNo(), row.workOrderNo(), row.productName(),
                    row.batchNo(), row.workshopName(), row.lineName(), value(row.processName()),
                    row.recordType() == 1 ? "正常" : "冲销",
                    String.valueOf(detail.getOccurrenceInputQuantity()),
                    String.valueOf(detail.getReversalInputQuantity()), String.valueOf(detail.getNetInputQuantity()),
                    String.valueOf(detail.getOccurrenceGoodQuantity()),
                    String.valueOf(detail.getReversalGoodQuantity()), String.valueOf(detail.getNetGoodQuantity()),
                    String.valueOf(detail.getOccurrenceDefectQuantity()),
                    String.valueOf(detail.getReversalDefectQuantity()), String.valueOf(detail.getNetDefectQuantity()),
                    String.valueOf(detail.getNetReworkQuantity()), String.valueOf(row.reportTime())));
        }
        String fileName = filePrefix + "_" + java.time.LocalDateTime.now().format(FILE_TIME) + ".csv";
        return new ReportExportFile(fileName, "text/csv;charset=UTF-8",
                csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private ProductionReportRespVO.Detail toDetail(ReportDetail row) {
        boolean reversal = row.recordType() == 2;
        ProductionReportRespVO.Detail result = new ProductionReportRespVO.Detail();
        result.setReportId(row.reportId());
        result.setReportNo(row.reportNo());
        result.setTaskId(row.taskId());
        result.setTaskNo(row.taskNo());
        result.setWorkOrderNo(row.workOrderNo());
        result.setProductId(row.productId());
        result.setProductName(row.productName());
        result.setBatchNo(row.batchNo());
        result.setWorkshopId(row.workshopId());
        result.setWorkshopName(row.workshopName());
        result.setLineId(row.lineId());
        result.setLineName(row.lineName());
        result.setProcessId(row.processId());
        result.setProcessName(row.processName());
        result.setRecordType(row.recordType());
        result.setSourceReportId(row.sourceReportId());
        result.setOccurrenceInputQuantity(reversal ? 0 : row.inputQuantity());
        result.setReversalInputQuantity(reversal ? row.inputQuantity() : 0);
        result.setNetInputQuantity(reversal ? -row.inputQuantity() : row.inputQuantity());
        result.setOccurrenceGoodQuantity(reversal ? 0 : row.goodQuantity());
        result.setReversalGoodQuantity(reversal ? row.goodQuantity() : 0);
        result.setNetGoodQuantity(reversal ? -row.goodQuantity() : row.goodQuantity());
        result.setOccurrenceDefectQuantity(reversal ? 0 : row.defectQuantity());
        result.setReversalDefectQuantity(reversal ? row.defectQuantity() : 0);
        result.setNetDefectQuantity(reversal ? -row.defectQuantity() : row.defectQuantity());
        result.setOccurrenceReworkQuantity(reversal ? 0 : row.reworkQuantity());
        result.setReversalReworkQuantity(reversal ? row.reworkQuantity() : 0);
        result.setNetReworkQuantity(reversal ? -row.reworkQuantity() : row.reworkQuantity());
        result.setReportTime(row.reportTime());
        return result;
    }

    private BigDecimal rate(long numerator, long denominator) {
        if (denominator <= 0L) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }

    private void appendCsvRow(StringBuilder csv, List<String> values) {
        List<String> escaped = new ArrayList<>(values.size());
        for (String item : values) {
            String safe = item == null ? "" : item.replace("\"", "\"\"");
            escaped.add("\"" + safe + "\"");
        }
        csv.append(String.join(",", escaped)).append("\r\n");
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
