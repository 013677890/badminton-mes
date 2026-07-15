package com.badminton.mes.module.scene.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.barcode.service.BarcodeSceneService;
import com.badminton.mes.module.production.service.WorkOrderExecutionSummaryService;
import com.badminton.mes.module.scene.constants.SceneErrorCodeConstants;
import com.badminton.mes.module.scene.constants.SceneParameterCodes;
import com.badminton.mes.module.scene.controller.vo.SceneEffectiveParameterReqVO;
import com.badminton.mes.module.scene.controller.vo.SceneWorkReportReverseReqVO;
import com.badminton.mes.module.scene.controller.vo.SceneWorkReportSubmitReqVO;
import com.badminton.mes.module.scene.dal.entity.SceneDispatchDetailEntity;
import com.badminton.mes.module.scene.dal.entity.SceneProductionTaskEntity;
import com.badminton.mes.module.scene.dal.entity.SceneWorkReportEntity;
import com.badminton.mes.module.scene.dal.repository.SceneDispatchDetailRepository;
import com.badminton.mes.module.scene.dal.repository.SceneProductionTaskRepository;
import com.badminton.mes.module.scene.dal.repository.SceneWorkReportRepository;
import com.badminton.mes.module.scene.service.SceneDataScopeService;
import com.badminton.mes.module.scene.service.SceneProductionParameterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 报工和全额冲销的数据库事务实现。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
@Service
public class SceneWorkReportTransactionalService {

    private static final int RECORD_TYPE_NORMAL = 1;
    private static final int RECORD_TYPE_REVERSAL = 2;
    private static final int BARCODE_USE_TYPE_REPORT = 3;
    private static final int TASK_STATUS_IN_PROGRESS = 3;
    private static final int OPERATION_STATUS_IN_PROGRESS = 1;
    private static final int OPERATION_STATUS_COMPLETED = 2;

    private final SceneWorkReportRepository reportRepository;
    private final SceneProductionTaskRepository taskRepository;
    private final SceneDispatchDetailRepository detailRepository;
    private final SceneDataScopeService dataScopeService;
    private final SceneProductionParameterService parameterService;
    private final BarcodeSceneService barcodeSceneService;
    private final WorkOrderExecutionSummaryService workOrderExecutionSummaryService;

    public SceneWorkReportTransactionalService(SceneWorkReportRepository reportRepository,
                                               SceneProductionTaskRepository taskRepository,
                                               SceneDispatchDetailRepository detailRepository,
                                               SceneDataScopeService dataScopeService,
                                               SceneProductionParameterService parameterService,
                                               BarcodeSceneService barcodeSceneService,
                                               WorkOrderExecutionSummaryService workOrderExecutionSummaryService) {
        this.reportRepository = reportRepository;
        this.taskRepository = taskRepository;
        this.detailRepository = detailRepository;
        this.dataScopeService = dataScopeService;
        this.parameterService = parameterService;
        this.barcodeSceneService = barcodeSceneService;
        this.workOrderExecutionSummaryService = workOrderExecutionSummaryService;
    }

    /**
     * 在单一事务中写入条码使用事实、报工记录和数量汇总。
     *
     * @param reqVO 报工请求
     * @param sourceType 来源类型
     * @return 报工主键
     */
    @Transactional(rollbackFor = Exception.class)
    public Long submit(SceneWorkReportSubmitReqVO reqVO, Integer sourceType) {
        var existed = reportRepository.findByRequestNoAndDeletedFalse(reqVO.getRequestNo());
        if (existed.isPresent()) {
            return existed.get().getId();
        }

        SceneDispatchDetailEntity detail = requireDetail(reqVO.getDispatchDetailId());
        SceneProductionTaskEntity task = requireTaskForUpdate(detail.getTaskId());
        validateReportStatus(task, detail);
        validateQuantity(reqVO.getInputQuantity(), reqVO.getGoodQuantity(),
                reqVO.getDefectQuantity(), reqVO.getReworkQuantity());
        validatePlanQuantity(task, reqVO.getInputQuantity());
        Long barcodeId = validateBarcode(reqVO, task, detail);

        SceneWorkReportEntity report = buildNormalReport(reqVO, sourceType, task, detail, barcodeId);
        reportRepository.saveAndFlush(report);
        apply(task, detail, report, 1);
        return report.getId();
    }

    /**
     * 在单一事务中新增全额冲销记录并反向更新数量汇总。
     *
     * @param id 原报工主键
     * @param reqVO 冲销请求
     * @return 冲销记录主键
     */
    @Transactional(rollbackFor = Exception.class)
    public Long reverse(Long id, SceneWorkReportReverseReqVO reqVO) {
        var existed = reportRepository.findByRequestNoAndDeletedFalse(reqVO.getRequestNo());
        if (existed.isPresent()) {
            return existed.get().getId();
        }

        SceneWorkReportEntity source = reportRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(SceneErrorCodeConstants.REPORT_NOT_EXISTS));
        if (!Integer.valueOf(RECORD_TYPE_NORMAL).equals(source.getRecordType())
                || reportRepository.existsBySourceReportIdAndDeletedFalse(id)) {
            throw new ServiceException(SceneErrorCodeConstants.REPORT_ALREADY_REVERSED);
        }

        SceneProductionTaskEntity task = requireTaskForUpdate(source.getTaskId());
        SceneDispatchDetailEntity detail = requireDetail(source.getDispatchDetailId());
        SceneWorkReportEntity reversal = buildReversal(source, reqVO);
        reportRepository.saveAndFlush(reversal);
        apply(task, detail, reversal, -1);
        return reversal.getId();
    }

    private SceneDispatchDetailEntity requireDetail(Long id) {
        return detailRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(SceneErrorCodeConstants.OPERATION_NOT_EXISTS));
    }

    private SceneProductionTaskEntity requireTaskForUpdate(Long id) {
        SceneProductionTaskEntity task = taskRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(SceneErrorCodeConstants.TASK_NOT_EXISTS));
        dataScopeService.check(task.getWorkshopId(), task.getLineId());
        return task;
    }

    private void validateReportStatus(SceneProductionTaskEntity task, SceneDispatchDetailEntity detail) {
        boolean reportableOperation = Integer.valueOf(OPERATION_STATUS_IN_PROGRESS).equals(detail.getDetailStatus())
                || Integer.valueOf(OPERATION_STATUS_COMPLETED).equals(detail.getDetailStatus());
        if (!Integer.valueOf(TASK_STATUS_IN_PROGRESS).equals(task.getTaskStatus())
                || !reportableOperation || Boolean.TRUE.equals(detail.getPaused())) {
            throw new ServiceException(SceneErrorCodeConstants.REPORT_STATUS_INVALID);
        }
    }

    private void validateQuantity(int input, int good, int defect, int rework) {
        if (good + defect != input || rework > defect) {
            throw new ServiceException(SceneErrorCodeConstants.REPORT_QUANTITY_INVALID);
        }
    }

    private void validatePlanQuantity(SceneProductionTaskEntity task, int inputQuantity) {
        int netReported = reportRepository.findByTaskIdAndDeletedFalse(task.getId()).stream()
                .mapToInt(report -> (Integer.valueOf(RECORD_TYPE_REVERSAL).equals(report.getRecordType()) ? -1 : 1)
                        * report.getInputQuantity())
                .sum();
        if (netReported + inputQuantity > task.getPlanQuantity()) {
            throw new ServiceException(SceneErrorCodeConstants.TASK_QUANTITY_EXCEEDED);
        }
    }

    private Long validateBarcode(SceneWorkReportSubmitReqVO reqVO, SceneProductionTaskEntity task,
                                 SceneDispatchDetailEntity detail) {
        SceneEffectiveParameterReqVO parameterReqVO = new SceneEffectiveParameterReqVO();
        parameterReqVO.setParamCode(SceneParameterCodes.MUST_SCAN_REPORT);
        parameterReqVO.setWorkshopId(task.getWorkshopId());
        parameterReqVO.setLineId(task.getLineId());
        parameterReqVO.setProductId(task.getProductId());
        boolean scanRequired = "1".equals(parameterService.getEffectiveParameter(parameterReqVO).getParamValue());
        boolean barcodePresent = StringUtils.hasText(reqVO.getBarcodeValue());
        if (scanRequired && !barcodePresent) {
            throw new ServiceException(SceneErrorCodeConstants.REPORT_BARCODE_REQUIRED);
        }
        if (!barcodePresent) {
            return null;
        }

        return barcodeSceneService.validateAndRecordUse(reqVO.getBarcodeValue(), task.getId(), task.getProductId(),
                task.getBatchNo(), detail.getProcessId(), SecurityContextHolder.getRequiredLoginUserId(),
                detail.getEquipmentId(), BARCODE_USE_TYPE_REPORT).barcodeId();
    }

    private SceneWorkReportEntity buildNormalReport(SceneWorkReportSubmitReqVO reqVO, Integer sourceType,
                                                    SceneProductionTaskEntity task,
                                                    SceneDispatchDetailEntity detail, Long barcodeId) {
        SceneWorkReportEntity report = new SceneWorkReportEntity();
        report.setReportNo(number("BG"));
        report.setRequestNo(reqVO.getRequestNo());
        report.setTaskId(task.getId());
        report.setDispatchDetailId(detail.getId());
        report.setProcessId(detail.getProcessId());
        report.setBatchNo(task.getBatchNo());
        report.setBarcodeId(barcodeId);
        report.setReportType(1);
        report.setRecordType(RECORD_TYPE_NORMAL);
        report.setUserId(SecurityContextHolder.getRequiredLoginUserId());
        report.setEquipmentId(detail.getEquipmentId());
        report.setStationId(detail.getStationId());
        report.setInputQuantity(reqVO.getInputQuantity());
        report.setGoodQuantity(reqVO.getGoodQuantity());
        report.setDefectQuantity(reqVO.getDefectQuantity());
        report.setReworkQuantity(reqVO.getReworkQuantity());
        report.setSourceType(sourceType);
        report.setReportTime(reqVO.getReportTime());
        return report;
    }

    private SceneWorkReportEntity buildReversal(SceneWorkReportEntity source,
                                                 SceneWorkReportReverseReqVO reqVO) {
        SceneWorkReportEntity reversal = new SceneWorkReportEntity();
        reversal.setReportNo(number("CX"));
        reversal.setRequestNo(reqVO.getRequestNo());
        reversal.setTaskId(source.getTaskId());
        reversal.setDispatchDetailId(source.getDispatchDetailId());
        reversal.setProcessId(source.getProcessId());
        reversal.setBatchNo(source.getBatchNo());
        reversal.setBarcodeId(source.getBarcodeId());
        reversal.setReportType(source.getReportType());
        reversal.setRecordType(RECORD_TYPE_REVERSAL);
        reversal.setSourceReportId(source.getId());
        reversal.setUserId(SecurityContextHolder.getRequiredLoginUserId());
        reversal.setEquipmentId(source.getEquipmentId());
        reversal.setStationId(source.getStationId());
        reversal.setInputQuantity(source.getInputQuantity());
        reversal.setGoodQuantity(source.getGoodQuantity());
        reversal.setDefectQuantity(source.getDefectQuantity());
        reversal.setReworkQuantity(source.getReworkQuantity());
        reversal.setSourceType(source.getSourceType());
        reversal.setReverseReason(reqVO.getReason());
        reversal.setReportTime(LocalDateTime.now());
        return reversal;
    }

    private void apply(SceneProductionTaskEntity task, SceneDispatchDetailEntity detail,
                       SceneWorkReportEntity report, int direction) {
        task.setInputQuantity(task.getInputQuantity() + direction * report.getInputQuantity());
        task.setGoodQuantity(task.getGoodQuantity() + direction * report.getGoodQuantity());
        task.setDefectQuantity(task.getDefectQuantity() + direction * report.getDefectQuantity());
        task.setReworkQuantity(task.getReworkQuantity() + direction * report.getReworkQuantity());
        detail.setGoodQuantity(detail.getGoodQuantity() + direction * report.getGoodQuantity());
        detail.setDefectQuantity(detail.getDefectQuantity() + direction * report.getDefectQuantity());
        taskRepository.save(task);
        detailRepository.save(detail);
        workOrderExecutionSummaryService.adjustReportedQuantities(task.getWorkOrderId(),
                direction * report.getInputQuantity(), direction * report.getDefectQuantity(),
                direction * report.getReworkQuantity());
    }

    private String number(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
