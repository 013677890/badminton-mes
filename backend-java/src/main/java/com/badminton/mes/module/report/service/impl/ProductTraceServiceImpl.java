package com.badminton.mes.module.report.service.impl;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.report.constants.ReportErrorCodeConstants;
import com.badminton.mes.module.report.controller.vo.ProductTraceQueryReqVO;
import com.badminton.mes.module.report.controller.vo.ProductTraceRespVO;
import com.badminton.mes.module.report.dal.ReportQueryRepository;
import com.badminton.mes.module.report.dal.ReportQueryRows.ReportDetail;
import com.badminton.mes.module.report.dal.ReportQueryRows.TraceTask;
import com.badminton.mes.module.report.service.ProductTraceService;
import com.badminton.mes.module.report.service.ReportDataScopeService;
import com.badminton.mes.module.scene.dal.repository.SceneRepairWorkOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 产品批次和条码主链路追溯实现。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
@Service
public class ProductTraceServiceImpl implements ProductTraceService {

    private final ReportQueryRepository repository;
    private final ReportDataScopeService dataScopeService;
    private final SceneRepairWorkOrderRepository repairRepository;

    public ProductTraceServiceImpl(ReportQueryRepository repository,
                                   ReportDataScopeService dataScopeService,
                                   SceneRepairWorkOrderRepository repairRepository) {
        this.repository = repository;
        this.dataScopeService = dataScopeService;
        this.repairRepository = repairRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductTraceRespVO trace(ProductTraceQueryReqVO reqVO) {
        if (!StringUtils.hasText(reqVO.getBatchCode()) && !StringUtils.hasText(reqVO.getBarcodeValue())
                && !StringUtils.hasText(reqVO.getWorkOrderNo()) && !StringUtils.hasText(reqVO.getTaskNo())) {
            throw new ServiceException(ReportErrorCodeConstants.TRACE_NOT_FOUND);
        }
        TraceTask task = repository.findTraceTask(reqVO.getBatchCode(), reqVO.getBarcodeValue(),
                        reqVO.getWorkOrderNo(), reqVO.getTaskNo())
                .orElseThrow(() -> new ServiceException(ReportErrorCodeConstants.TRACE_NOT_FOUND));
        dataScopeService.checkObject(task.workshopId(), task.lineId());
        return build(task);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductTraceRespVO traceByBarcode(String barcodeValue) {
        ProductTraceQueryReqVO reqVO = new ProductTraceQueryReqVO();
        reqVO.setBarcodeValue(barcodeValue);
        return trace(reqVO);
    }

    private ProductTraceRespVO build(TraceTask task) {
        ProductTraceRespVO result = new ProductTraceRespVO();
        result.setTask(toTask(task));
        repository.findWorkOrder(task.workOrderId()).ifPresent(row -> {
            ProductTraceRespVO.WorkOrder workOrder = new ProductTraceRespVO.WorkOrder();
            workOrder.setId(row.id());
            workOrder.setWorkOrderNo(row.workOrderNo());
            workOrder.setBatchNo(row.batchNo());
            workOrder.setProductId(row.productId());
            workOrder.setProductName(row.productName());
            workOrder.setSpec(row.spec());
            workOrder.setPlanQuantity(row.planQuantity());
            workOrder.setInputQuantity(row.inputQuantity());
            workOrder.setFinishQuantity(row.finishQuantity());
            workOrder.setDefectQuantity(row.defectQuantity());
            workOrder.setReworkQuantity(row.reworkQuantity());
            workOrder.setOrderStatus(row.orderStatus());
            result.setWorkOrder(workOrder);
        });
        result.setBarcodes(repository.listTraceBarcodes(task).stream().map(row -> {
            ProductTraceRespVO.Barcode item = new ProductTraceRespVO.Barcode();
            item.setId(row.id());
            item.setBarcodeValue(row.barcodeValue());
            item.setBarcodeTypeId(row.barcodeTypeId());
            item.setBarcodeMode(row.barcodeMode());
            item.setProductId(row.productId());
            item.setMaterialId(row.materialId());
            item.setBatchNo(row.batchNo());
            item.setBarcodeStatus(row.barcodeStatus());
            item.setCreateTime(row.createTime());
            return item;
        }).toList());
        result.setBarcodeUses(repository.listTraceBarcodeUses(task.id()).stream().map(row -> {
            ProductTraceRespVO.BarcodeUse item = new ProductTraceRespVO.BarcodeUse();
            item.setId(row.id());
            item.setBarcodeId(row.barcodeId());
            item.setProcessId(row.processId());
            item.setUserId(row.userId());
            item.setEquipmentId(row.equipmentId());
            item.setUseType(row.useType());
            item.setBusinessTime(row.businessTime());
            return item;
        }).toList());
        result.setProcessHistories(repository.listTraceProcessHistories(task.id()).stream().map(row -> {
            ProductTraceRespVO.ProcessHistory item = new ProductTraceRespVO.ProcessHistory();
            item.setId(row.id());
            item.setProcessId(row.processId());
            item.setProcessCode(row.processCode());
            item.setProcessName(row.processName());
            item.setActionType(row.actionType());
            item.setOperatorId(row.operatorId());
            item.setActionReason(row.actionReason());
            item.setOperateTime(row.operateTime());
            return item;
        }).toList());
        result.setWorkReports(repository.listTraceReports(task.id()).stream().map(this::toWorkReport).toList());
        result.setMaterials(repository.listTraceMaterials(task.workOrderId()).stream().map(row -> {
            ProductTraceRespVO.Material item = new ProductTraceRespVO.Material();
            item.setMaterialId(row.materialId());
            item.setMaterialCode(row.materialCode());
            item.setMaterialName(row.materialName());
            item.setRequireQuantity(row.requireQuantity());
            item.setIssuedQuantity(row.issuedQuantity());
            return item;
        }).toList());
        result.setRepairRecords(repairRepository.findByBatchNoAndDeletedFalseOrderByCreatedTimeAsc(task.batchNo())
                .stream().map(row -> {
                    ProductTraceRespVO.OptionalSourceItem item = new ProductTraceRespVO.OptionalSourceItem();
                    item.setSourceType("REPAIR"); item.setSourceId(row.getRepairNo());
                    item.setSummary(row.getStatus() + "，返修数量 " + row.getRepairQuantity());
                    item.setEventTime(row.getUpdatedTime()); return item;
                }).toList());
        List<String> warnings = new ArrayList<>();
        warnings.add("MATERIAL_BATCH：A组当前仅提供工单物料需求和领料数量，未提供实际消耗物料批次");
        warnings.add("PACKING：装箱明细表尚未落库，当前追溯不含装箱信息");
        warnings.add("QUALITY_INSPECTION：C组质量结果表契约尚未落库，当前追溯不含质量不良");
        if (result.getRepairRecords().isEmpty()) {
            warnings.add("REPAIR：当前批次没有返修记录");
        }
        warnings.add("EQUIPMENT：C组设备状态表契约尚未落库，当前追溯不含设备状态");
        warnings.add("ANDON：C组安灯事件表契约尚未落库，当前追溯不含安灯异常");
        result.setWarnings(List.copyOf(warnings));
        result.setDataCompleteness("PARTIAL");
        return result;
    }

    private ProductTraceRespVO.Task toTask(TraceTask row) {
        ProductTraceRespVO.Task result = new ProductTraceRespVO.Task();
        result.setId(row.id());
        result.setTaskNo(row.taskNo());
        result.setProductId(row.productId());
        result.setProductCode(row.productCode());
        result.setProductName(row.productName());
        result.setBatchNo(row.batchNo());
        result.setWorkshopId(row.workshopId());
        result.setWorkshopName(row.workshopName());
        result.setLineId(row.lineId());
        result.setLineName(row.lineName());
        result.setPlanQuantity(row.planQuantity());
        result.setInputQuantity(row.inputQuantity());
        result.setGoodQuantity(row.goodQuantity());
        result.setDefectQuantity(row.defectQuantity());
        result.setReworkQuantity(row.reworkQuantity());
        result.setFinishQuantity(row.finishQuantity());
        result.setTaskStatus(row.taskStatus());
        result.setActualStartTime(row.actualStartTime());
        result.setActualEndTime(row.actualEndTime());
        return result;
    }

    private ProductTraceRespVO.WorkReport toWorkReport(ReportDetail row) {
        boolean reversal = row.recordType() == 2;
        ProductTraceRespVO.WorkReport result = new ProductTraceRespVO.WorkReport();
        result.setId(row.reportId());
        result.setReportNo(row.reportNo());
        result.setRecordType(row.recordType());
        result.setSourceReportId(row.sourceReportId());
        result.setProcessId(row.processId());
        result.setOccurrenceInputQuantity(reversal ? 0 : row.inputQuantity());
        result.setReversalInputQuantity(reversal ? row.inputQuantity() : 0);
        result.setNetInputQuantity(reversal ? -row.inputQuantity() : row.inputQuantity());
        result.setOccurrenceGoodQuantity(reversal ? 0 : row.goodQuantity());
        result.setReversalGoodQuantity(reversal ? row.goodQuantity() : 0);
        result.setNetGoodQuantity(reversal ? -row.goodQuantity() : row.goodQuantity());
        result.setOccurrenceDefectQuantity(reversal ? 0 : row.defectQuantity());
        result.setReversalDefectQuantity(reversal ? row.defectQuantity() : 0);
        result.setNetDefectQuantity(reversal ? -row.defectQuantity() : row.defectQuantity());
        result.setReverseReason(row.reverseReason());
        result.setReportTime(row.reportTime());
        return result;
    }
}
