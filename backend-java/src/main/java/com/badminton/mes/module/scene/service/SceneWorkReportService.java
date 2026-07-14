package com.badminton.mes.module.scene.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.craft.dal.repository.CraftProcessRepository;
import com.badminton.mes.module.integration.dal.entity.DeviceCountRecordEntity;
import com.badminton.mes.module.integration.dal.entity.EquipmentBindingEntity;
import com.badminton.mes.module.production.dal.entity.DispatchOrderEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.scene.constants.SceneErrorCodeConstants;
import com.badminton.mes.module.scene.dal.entity.SceneExecutionTaskEntity;
import com.badminton.mes.module.scene.dal.entity.SceneProcessTaskEntity;
import com.badminton.mes.module.scene.dal.entity.SceneWorkReportEntity;
import com.badminton.mes.module.scene.dal.repository.SceneProcessTaskRepository;
import com.badminton.mes.module.scene.dal.repository.SceneExecutionTaskRepository;
import com.badminton.mes.module.scene.dal.repository.SceneWorkReportRepository;
import com.badminton.mes.module.scene.enums.WorkReportAuditStatusEnum;
import com.badminton.mes.module.scene.enums.SceneExecutionTaskStatusEnum;
import com.badminton.mes.module.system.dal.repository.UserRepository;
import com.badminton.mes.module.wage.dal.repository.WageWorkRecordRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 现场报工及计件快照联动服务。
 *
 * @author Codex
 * @date 2026/07/13
 */
@Service
public class SceneWorkReportService {

    private static final int SOURCE_DEVICE = 2;

    private final SceneWorkReportRepository reportRepository;
    private final SceneExecutionTaskRepository taskRepository;
    private final SceneProcessTaskRepository processTaskRepository;
    private final WorkOrderRepository workOrderRepository;
    private final CraftProcessRepository craftProcessRepository;
    private final UserRepository userRepository;
    private final WageWorkRecordRepository wageWorkRecordRepository;

    public SceneWorkReportService(SceneWorkReportRepository reportRepository,
                                  SceneExecutionTaskRepository taskRepository,
                                  SceneProcessTaskRepository processTaskRepository,
                                  WorkOrderRepository workOrderRepository,
                                  CraftProcessRepository craftProcessRepository,
                                  UserRepository userRepository,
                                  WageWorkRecordRepository wageWorkRecordRepository) {
        this.reportRepository = reportRepository;
        this.taskRepository = taskRepository;
        this.processTaskRepository = processTaskRepository;
        this.workOrderRepository = workOrderRepository;
        this.craftProcessRepository = craftProcessRepository;
        this.userRepository = userRepository;
        this.wageWorkRecordRepository = wageWorkRecordRepository;
    }

    /**
     * 根据设备计数增量创建自动或待确认报工。
     *
     * @return 报工主键
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createDeviceReport(DeviceCountRecordEntity sourceRecord,
                                   EquipmentBindingEntity binding,
                                   DispatchOrderEntity dispatch) {
        var existing = reportRepository.findBySourceTypeAndSourceRecordIdAndDeletedFalse(
                SOURCE_DEVICE, sourceRecord.getId());
        if (existing.isPresent()) {
            return existing.get().getId();
        }

        SceneExecutionTaskEntity task = taskRepository
                .findByDispatchOrderIdAndDeletedFalse(dispatch.getId())
                .orElseThrow(() -> new ServiceException(
                        SceneErrorCodeConstants.PRODUCTION_TASK_NOT_EXISTS));
        WorkOrderEntity workOrder = workOrderRepository
                .findByIdAndDeletedFalse(dispatch.getWorkOrderId())
                .orElseThrow(() -> new ServiceException(
                        SceneErrorCodeConstants.PRODUCTION_TASK_NOT_EXISTS));
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        boolean autoApprove = Boolean.TRUE.equals(binding.getAutoReport())
                && binding.getDefaultEmployeeId() != null;
        if (Boolean.TRUE.equals(binding.getAutoReport())) {
            validateAutoReportBinding(sourceRecord.getProcessId(), binding.getDefaultEmployeeId());
        }

        SceneWorkReportEntity report = new SceneWorkReportEntity();
        report.setReportNo("DR" + sourceRecord.getId());
        report.setSourceType(SOURCE_DEVICE);
        report.setSourceRecordId(sourceRecord.getId());
        report.setProductionTaskId(task.getId());
        report.setDispatchOrderId(dispatch.getId());
        report.setWorkOrderId(dispatch.getWorkOrderId());
        report.setProductId(workOrder.getProductId());
        report.setProcessId(sourceRecord.getProcessId());
        report.setEmployeeId(binding.getDefaultEmployeeId());
        report.setQualifiedQuantity(BigDecimal.valueOf(sourceRecord.getIncrementValue()));
        report.setDefectQuantity(BigDecimal.ZERO);
        report.setReportTime(sourceRecord.getCollectTime());
        report.setAuditStatus(autoApprove
                ? WorkReportAuditStatusEnum.APPROVED.getStatus()
                : WorkReportAuditStatusEnum.PENDING.getStatus());
        if (autoApprove) {
            report.setAuditBy(operatorId);
            report.setAuditTime(LocalDateTime.now());
        }
        report.setCreateBy(operatorId);
        reportRepository.saveAndFlush(report);
        if (autoApprove) {
            applyApprovedReport(report);
            importWageSnapshot(report, operatorId);
        }
        return report.getId();
    }

    /** 审核待确认报工并同步计件快照。 */
    @Transactional(rollbackFor = Exception.class)
    public void approveReport(Long id, Long employeeId) {
        var user = userRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> new ServiceException(
                        SceneErrorCodeConstants.WORK_REPORT_EMPLOYEE_REQUIRED));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(user.getStatus())) {
            throw new ServiceException(SceneErrorCodeConstants.WORK_REPORT_EMPLOYEE_REQUIRED);
        }

        SceneWorkReportEntity report = reportRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ServiceException(
                        SceneErrorCodeConstants.WORK_REPORT_NOT_EXISTS));
        if (!WorkReportAuditStatusEnum.PENDING.getStatus().equals(report.getAuditStatus())) {
            throw new ServiceException(SceneErrorCodeConstants.WORK_REPORT_STATUS_INVALID);
        }
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        report.setEmployeeId(employeeId);
        report.setAuditStatus(WorkReportAuditStatusEnum.APPROVED.getStatus());
        report.setAuditBy(operatorId);
        report.setAuditTime(LocalDateTime.now());
        reportRepository.save(report);
        applyApprovedReport(report);
        importWageSnapshot(report, operatorId);
    }

    private void validateAutoReportBinding(Long processId, Long employeeId) {
        if (employeeId == null) {
            throw new ServiceException(SceneErrorCodeConstants.WORK_REPORT_EMPLOYEE_REQUIRED);
        }
        craftProcessRepository.findByIdAndStatusAndDeletedFalse(
                        processId, CommonStatusEnum.ENABLED.getStatus())
                .orElseThrow(() -> new ServiceException(
                        SceneErrorCodeConstants.WORK_REPORT_PROCESS_INVALID));
        userRepository.findByIdAndStatusAndDeletedFalse(
                        employeeId, CommonStatusEnum.ENABLED.getStatus())
                .orElseThrow(() -> new ServiceException(
                        SceneErrorCodeConstants.WORK_REPORT_EMPLOYEE_REQUIRED));
    }

    private void applyApprovedReport(SceneWorkReportEntity report) {
        SceneExecutionTaskEntity task = taskRepository
                .findByIdForUpdate(report.getProductionTaskId())
                .orElseThrow(() -> new ServiceException(
                        SceneErrorCodeConstants.PRODUCTION_TASK_NOT_EXISTS));
        if (SceneExecutionTaskStatusEnum.CANCELLED.getStatus().equals(task.getTaskStatus())) {
            throw new ServiceException(SceneErrorCodeConstants.PRODUCTION_TASK_STATUS_INVALID);
        }

        var processTasks = processTaskRepository
                .findByProductionTaskIdForUpdate(task.getId());
        SceneProcessTaskEntity current = processTasks.stream()
                .filter(item -> item.getProcessId().equals(report.getProcessId()))
                .findFirst()
                .orElseThrow(() -> new ServiceException(
                        SceneErrorCodeConstants.PROCESS_TASK_NOT_EXISTS));
        current.setQualifiedQuantity(orZero(current.getQualifiedQuantity())
                .add(report.getQualifiedQuantity()));
        current.setDefectQuantity(orZero(current.getDefectQuantity())
                .add(report.getDefectQuantity()));
        BigDecimal reportedQuantity = current.getQualifiedQuantity()
                .add(current.getDefectQuantity());
        current.setTaskStatus(reportedQuantity.compareTo(
                BigDecimal.valueOf(task.getPlanQuantity())) >= 0
                ? SceneExecutionTaskStatusEnum.COMPLETED.getStatus()
                : SceneExecutionTaskStatusEnum.EXECUTING.getStatus());
        processTaskRepository.save(current);

        SceneProcessTaskEntity lastProcess = processTasks.get(processTasks.size() - 1);
        task.setQualifiedQuantity(orZero(lastProcess.getQualifiedQuantity()));
        task.setDefectQuantity(orZero(lastProcess.getDefectQuantity()));
        boolean allCompleted = processTasks.stream().allMatch(item ->
                SceneExecutionTaskStatusEnum.COMPLETED.getStatus().equals(item.getTaskStatus()));
        task.setTaskStatus(allCompleted
                ? SceneExecutionTaskStatusEnum.COMPLETED.getStatus()
                : SceneExecutionTaskStatusEnum.EXECUTING.getStatus());
        taskRepository.save(task);
    }

    private BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void importWageSnapshot(SceneWorkReportEntity report, Long operatorId) {
        wageWorkRecordRepository.insertIdempotently(
                report.getId(), report.getEmployeeId(), report.getReportTime().toLocalDate(),
                report.getWorkOrderId(), report.getProcessId(), report.getProductId(),
                report.getQualifiedQuantity(), report.getDefectQuantity(),
                report.getAuditTime(), operatorId);
    }
}
