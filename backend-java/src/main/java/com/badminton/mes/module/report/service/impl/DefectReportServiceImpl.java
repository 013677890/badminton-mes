package com.badminton.mes.module.report.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.report.constants.ReportErrorCodeConstants;
import com.badminton.mes.module.report.controller.vo.DefectReportRespVO;
import com.badminton.mes.module.report.controller.vo.ProductionReportRespVO;
import com.badminton.mes.module.report.controller.vo.ReportQueryReqVO;
import com.badminton.mes.module.report.service.DefectReportService;
import com.badminton.mes.module.report.service.DefectSourceProvider;
import com.badminton.mes.module.report.service.ProductionReportService;
import com.badminton.mes.module.report.service.ReportExportFile;
import com.badminton.mes.module.report.service.dto.DefectSourceBatch;
import com.badminton.mes.module.report.service.dto.DefectSourceRecord;
import com.badminton.mes.module.report.service.dto.ReportQueryCriteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 不良来源明细、defectGroupNo 综合归并和同步导出实现。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
@Service
public class DefectReportServiceImpl implements DefectReportService {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final List<DefectSourceProvider> sourceProviders;
    private final ReportQuerySupport querySupport;
    private final ProductionReportService productionReportService;

    public DefectReportServiceImpl(List<DefectSourceProvider> sourceProviders,
                                   ReportQuerySupport querySupport,
                                   ProductionReportService productionReportService) {
        this.sourceProviders = List.copyOf(sourceProviders);
        this.querySupport = querySupport;
        this.productionReportService = productionReportService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DefectReportRespVO.Detail> sourceDetails(ReportQueryReqVO reqVO) {
        LoadedDefects loaded = load(querySupport.criteria(reqVO));
        List<DefectReportRespVO.Detail> rows = loaded.records().stream()
                .sorted(recordOrder()).map(this::toSourceDetail).toList();
        return page(rows, reqVO.getPageNo(), reqVO.getPageSize());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DefectReportRespVO.Detail> comprehensiveDetails(ReportQueryReqVO reqVO) {
        LoadedDefects loaded = load(querySupport.criteria(reqVO));
        List<DefectReportRespVO.Detail> rows = comprehensive(loaded.records()).stream()
                .sorted(Comparator.comparing(DefectReportRespVO.Detail::getDetectedTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return page(rows, reqVO.getPageNo(), reqVO.getPageSize());
    }

    @Override
    @Transactional(readOnly = true)
    public DefectReportRespVO.Summary summary(ReportQueryReqVO reqVO) {
        LoadedDefects loaded = load(querySupport.criteria(reqVO));
        List<DefectReportRespVO.Detail> comprehensiveRows = comprehensive(loaded.records());
        long sceneNet = sourceNet(loaded.records(), "SCENE_WORK_REPORT");
        long qualityNet = sourceNet(loaded.records(), "QUALITY_INSPECTION");
        long repairNet = sourceNet(loaded.records(), "REPAIR_RECHECK");
        long comprehensiveNet = comprehensiveRows.stream().mapToLong(DefectReportRespVO.Detail::getNetQuantity).sum();
        long sceneOccurrence = loaded.records().stream()
                .filter(row -> "SCENE_WORK_REPORT".equals(row.sourceType()))
                .mapToLong(DefectSourceRecord::occurrenceQuantity).sum();
        long sceneReversal = loaded.records().stream()
                .filter(row -> "SCENE_WORK_REPORT".equals(row.sourceType()))
                .mapToLong(DefectSourceRecord::reversalQuantity).sum();
        ProductionReportRespVO.Summary production = productionReportService.summary(reqVO);
        DefectReportRespVO.Summary result = new DefectReportRespVO.Summary();
        result.setSceneDefectQuantity(sceneNet);
        result.setQualityDefectQuantity(qualityNet);
        result.setRepairRecheckDefectQuantity(repairNet);
        result.setComprehensiveDefectQuantity(comprehensiveNet);
        result.setSceneOccurrenceQuantity(sceneOccurrence);
        result.setSceneReversalQuantity(sceneReversal);
        result.setSourceRecordCount(loaded.records().size());
        result.setComprehensiveEventCount(comprehensiveRows.size());
        result.setMergedDuplicateCount(Math.max(0L, loaded.records().size() - comprehensiveRows.size()));
        result.setReportInputQuantity(production.getInputQuantity());
        result.setSceneDefectRate(rate(sceneNet, production.getInputQuantity()));
        result.setComprehensiveDefectRate(rate(comprehensiveNet, production.getInputQuantity()));
        result.setWarnings(loaded.warnings());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ReportExportFile export(ReportQueryReqVO reqVO) {
        ReportQueryCriteria criteria = querySupport.exportCriteria(reqVO);
        LoadedDefects loaded = load(criteria);
        StringBuilder csv = new StringBuilder(1024 + loaded.records().size() * 192);
        csv.append('\uFEFF').append("来源,来源单ID,归并号,任务号,工单号,产品,批次,工序ID,不良原因,")
                .append("发生数量,冲销数量,净额,发现时间\r\n");
        for (DefectSourceRecord row : loaded.records().stream().sorted(recordOrder()).toList()) {
            appendCsvRow(csv, row.sourceType(), String.valueOf(row.sourceId()), value(row.defectGroupNo()),
                    row.taskNo(), row.workOrderNo(), row.productName(), row.batchNo(),
                    String.valueOf(row.processId()), value(row.defectName()),
                    String.valueOf(row.occurrenceQuantity()), String.valueOf(row.reversalQuantity()),
                    String.valueOf(row.netQuantity()), String.valueOf(row.detectedTime()));
        }
        return new ReportExportFile("defects_" + LocalDateTime.now().format(FILE_TIME) + ".csv",
                "text/csv;charset=UTF-8", csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private LoadedDefects load(ReportQueryCriteria criteria) {
        List<DefectSourceRecord> records = new ArrayList<>();
        Set<String> warnings = new LinkedHashSet<>();
        for (DefectSourceProvider provider : sourceProviders) {
            DefectSourceBatch batch = provider.load(criteria, ReportQuerySupport.DEFECT_MAX_ROWS + 1);
            records.addAll(batch.records());
            warnings.addAll(batch.warnings());
            if (records.size() > ReportQuerySupport.DEFECT_MAX_ROWS) {
                throw new ServiceException(ReportErrorCodeConstants.DEFECT_ROWS_EXCEEDED);
            }
        }
        return new LoadedDefects(List.copyOf(records), List.copyOf(warnings));
    }

    private List<DefectReportRespVO.Detail> comprehensive(List<DefectSourceRecord> records) {
        Map<String, GroupAccumulator> groups = new LinkedHashMap<>();
        for (DefectSourceRecord record : records) {
            String key = StringUtils.hasText(record.defectGroupNo())
                    ? "GROUP:" + record.defectGroupNo()
                    : "SOURCE:" + record.sourceType() + ":" + record.sourceId() + ":" + record.sourceDetailId();
            groups.computeIfAbsent(key, ignored -> new GroupAccumulator(record)).add(record);
        }
        return groups.values().stream().map(GroupAccumulator::toDetail).toList();
    }

    private DefectReportRespVO.Detail toSourceDetail(DefectSourceRecord row) {
        DefectReportRespVO.Detail result = baseDetail(row);
        result.setSourceType(row.sourceType());
        result.setSourceId(row.sourceId());
        result.setSourceDetailId(row.sourceDetailId());
        result.setOccurrenceQuantity(row.occurrenceQuantity());
        result.setReversalQuantity(row.reversalQuantity());
        result.setNetQuantity(row.netQuantity());
        return result;
    }

    private DefectReportRespVO.Detail baseDetail(DefectSourceRecord row) {
        DefectReportRespVO.Detail result = new DefectReportRespVO.Detail();
        result.setDefectGroupNo(row.defectGroupNo());
        result.setTaskId(row.taskId());
        result.setTaskNo(row.taskNo());
        result.setWorkOrderNo(row.workOrderNo());
        result.setProductId(row.productId());
        result.setProductName(row.productName());
        result.setBatchNo(row.batchNo());
        result.setWorkshopId(row.workshopId());
        result.setLineId(row.lineId());
        result.setProcessId(row.processId());
        result.setProcessName(row.processName());
        result.setDefectCode(row.defectCode());
        result.setDefectName(row.defectName());
        result.setDetectedTime(row.detectedTime());
        return result;
    }

    private long sourceNet(List<DefectSourceRecord> records, String sourceType) {
        return records.stream().filter(row -> sourceType.equals(row.sourceType()))
                .mapToLong(DefectSourceRecord::netQuantity).sum();
    }

    private BigDecimal rate(long numerator, long denominator) {
        if (denominator <= 0L) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }

    private Comparator<DefectSourceRecord> recordOrder() {
        return Comparator.comparing(DefectSourceRecord::detectedTime,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(DefectSourceRecord::sourceId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private <T> PageResult<T> page(List<T> rows, int pageNo, int pageSize) {
        if (rows.isEmpty()) {
            return PageResult.empty(pageNo, pageSize);
        }
        int pages = (rows.size() + pageSize - 1) / pageSize;
        int effectivePageNo = Math.min(pageNo, pages);
        int from = (effectivePageNo - 1) * pageSize;
        int to = Math.min(from + pageSize, rows.size());
        return PageResult.of(rows.subList(from, to), (long) rows.size(), effectivePageNo, pageSize);
    }

    private void appendCsvRow(StringBuilder csv, String... values) {
        List<String> escaped = new ArrayList<>(values.length);
        for (String item : values) {
            String safe = item == null ? "" : item.replace("\"", "\"\"");
            escaped.add("\"" + safe + "\"");
        }
        csv.append(String.join(",", escaped)).append("\r\n");
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private record LoadedDefects(List<DefectSourceRecord> records, List<String> warnings) {
    }

    private final class GroupAccumulator {
        private final DefectSourceRecord representative;
        private final Map<String, long[]> sourceQuantities = new LinkedHashMap<>();

        private GroupAccumulator(DefectSourceRecord representative) {
            this.representative = representative;
        }

        private void add(DefectSourceRecord record) {
            long[] quantities = sourceQuantities.computeIfAbsent(record.sourceType(), ignored -> new long[2]);
            quantities[0] += record.occurrenceQuantity();
            quantities[1] += record.reversalQuantity();
        }

        private DefectReportRespVO.Detail toDetail() {
            DefectReportRespVO.Detail result = baseDetail(representative);
            result.setSourceType("COMPREHENSIVE");
            long occurrence = sourceQuantities.values().stream().mapToLong(value -> value[0]).max().orElse(0L);
            long reversal = sourceQuantities.values().stream().mapToLong(value -> value[1]).max().orElse(0L);
            long net = sourceQuantities.values().stream()
                    .mapToLong(value -> Math.max(0L, value[0] - value[1])).max().orElse(0L);
            result.setOccurrenceQuantity(occurrence);
            result.setReversalQuantity(reversal);
            result.setNetQuantity(net);
            return result;
        }
    }
}
