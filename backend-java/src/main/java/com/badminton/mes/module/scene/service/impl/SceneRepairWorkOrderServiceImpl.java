package com.badminton.mes.module.scene.service.impl;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.scene.constants.SceneErrorCodeConstants;
import com.badminton.mes.module.scene.controller.vo.*;
import com.badminton.mes.module.scene.dal.entity.*;
import com.badminton.mes.module.scene.dal.repository.*;
import com.badminton.mes.module.scene.enums.SceneRepairStatusEnum;
import com.badminton.mes.module.scene.service.SceneRepairWorkOrderService;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 返修工单服务实现。 @author 刘涵 */
@Service
public class SceneRepairWorkOrderServiceImpl implements SceneRepairWorkOrderService {
    private final SceneRepairWorkOrderRepository workOrderRepository;
    private final SceneRepairRecordRepository recordRepository;
    private final SceneWorkReportRepository workReportRepository;
    private final SceneProductionTaskRepository taskRepository;
    private final SceneRepairRecheckRecordRepository recheckRecordRepository;
    private final com.badminton.mes.module.scene.service.SceneDataScopeService dataScopeService;

    public SceneRepairWorkOrderServiceImpl(SceneRepairWorkOrderRepository workOrderRepository,
                                           SceneRepairRecordRepository recordRepository,
                                           SceneWorkReportRepository workReportRepository,
                                           SceneProductionTaskRepository taskRepository,
                                           SceneRepairRecheckRecordRepository recheckRecordRepository,
                                           com.badminton.mes.module.scene.service.SceneDataScopeService dataScopeService) {
        this.workOrderRepository = workOrderRepository;
        this.recordRepository = recordRepository;
        this.workReportRepository = workReportRepository;
        this.taskRepository = taskRepository;
        this.recheckRecordRepository = recheckRecordRepository;
        this.dataScopeService = dataScopeService;
    }

    @Override @Transactional
    public SceneRepairRespVO create(SceneRepairCreateReqVO request) {
        var existing = workOrderRepository.findBySourceReportIdAndDeletedFalse(request.getSourceReportId());
        if (existing.isPresent()) return toResponse(existing.get());
        var sourceReport = workReportRepository.findByIdAndDeletedFalse(request.getSourceReportId())
                .orElseThrow(() -> new ServiceException(SceneErrorCodeConstants.REPORT_NOT_EXISTS));
        var task = taskRepository.findByIdAndDeletedFalse(sourceReport.getTaskId())
                .orElseThrow(() -> new ServiceException(SceneErrorCodeConstants.TASK_NOT_EXISTS));
        dataScopeService.check(task.getWorkshopId(), task.getLineId());
        boolean invalidSource = sourceReport.getRecordType() != 1 || sourceReport.getDefectQuantity() == null
                || sourceReport.getDefectQuantity() <= 0 || workReportRepository.existsBySourceReportIdAndDeletedFalse(sourceReport.getId());
        if (invalidSource || request.getRepairQuantity() > sourceReport.getDefectQuantity()) {
            throw new ServiceException(SceneErrorCodeConstants.REPAIR_QUANTITY_INVALID);
        }
        var now = LocalDateTime.now();
        var entity = new SceneRepairWorkOrderEntity();
        entity.setRepairNo("RW-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        entity.setSourceReportId(request.getSourceReportId());
        entity.setTaskId(sourceReport.getTaskId());
        entity.setBatchNo(sourceReport.getBatchNo());
        entity.setDefectQuantity(sourceReport.getDefectQuantity());
        entity.setRepairQuantity(request.getRepairQuantity());
        entity.setReason(request.getReason());
        entity.setStatus(SceneRepairStatusEnum.PENDING_ASSIGN);
        entity.setCreatedBy(SecurityContextHolder.getRequiredLoginUserId());
        entity.setCreatedTime(now); entity.setUpdatedTime(now); entity.setDeleted(false);
        return toResponse(workOrderRepository.save(entity));
    }

    @Override @Transactional public void assign(Long id, Long assigneeId) {
        var entity = getEntity(id); requireStatus(entity, SceneRepairStatusEnum.PENDING_ASSIGN);
        entity.setAssigneeId(assigneeId); entity.setStatus(SceneRepairStatusEnum.PENDING_REPAIR); touch(entity);
    }

    @Override @Transactional public void start(Long id) {
        var entity = getEntity(id);
        if (entity.getStatus() != SceneRepairStatusEnum.PENDING_REPAIR
                && entity.getStatus() != SceneRepairStatusEnum.CONTINUE_REPAIR) {
            throw new ServiceException(SceneErrorCodeConstants.REPAIR_STATUS_INVALID);
        }
        entity.setStatus(SceneRepairStatusEnum.REPAIRING); touch(entity);
    }

    @Override @Transactional public void addRecord(Long id, SceneRepairRecordCreateReqVO request) {
        var entity = getEntity(id); requireStatus(entity, SceneRepairStatusEnum.REPAIRING);
        var record = new SceneRepairRecordEntity(); record.setRepairWorkOrderId(id);
        record.setQuantity(request.getQuantity()); record.setDescription(request.getDescription());
        record.setOperatorId(SecurityContextHolder.getRequiredLoginUserId()); record.setCreatedTime(LocalDateTime.now());
        recordRepository.save(record); entity.setStatus(SceneRepairStatusEnum.PENDING_RECHECK); touch(entity);
    }

    @Override @Transactional public void recheck(Long id, SceneRepairRecheckReqVO request) {
        var entity = getEntity(id); requireStatus(entity, SceneRepairStatusEnum.PENDING_RECHECK);
        if (request.getQuantity() > entity.getRepairQuantity()) throw new ServiceException(SceneErrorCodeConstants.REPAIR_RECHECK_INVALID);
        entity.setRecheckResult(request.getResult()); entity.setRecheckQuantity(request.getQuantity());
        var record = new SceneRepairRecheckRecordEntity(); record.setRepairWorkOrderId(id);
        record.setResult(request.getResult()); record.setQuantity(request.getQuantity());
        record.setInspectorId(SecurityContextHolder.getRequiredLoginUserId()); record.setCreatedTime(LocalDateTime.now());
        recheckRecordRepository.save(record);
        entity.setStatus(SceneRepairStatusEnum.valueOf(request.getResult())); touch(entity);
    }

    @Override @Transactional public void close(Long id) {
        var entity = getEntity(id);
        if (entity.getStatus() != SceneRepairStatusEnum.RELEASED && entity.getStatus() != SceneRepairStatusEnum.SCRAPPED) {
            throw new ServiceException(SceneErrorCodeConstants.REPAIR_STATUS_INVALID);
        }
        entity.setStatus(SceneRepairStatusEnum.CLOSED); touch(entity);
    }

    @Override @Transactional(readOnly = true) public SceneRepairRespVO get(Long id) { return toResponse(getEntity(id)); }

    private SceneRepairWorkOrderEntity getEntity(Long id) {
        var entity = workOrderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(SceneErrorCodeConstants.REPAIR_NOT_EXISTS));
        var task = taskRepository.findByIdAndDeletedFalse(entity.getTaskId())
                .orElseThrow(() -> new ServiceException(SceneErrorCodeConstants.TASK_NOT_EXISTS));
        dataScopeService.check(task.getWorkshopId(), task.getLineId());
        return entity;
    }

    private SceneRepairRespVO toResponse(SceneRepairWorkOrderEntity entity) {
        var response = new SceneRepairRespVO(); response.setId(entity.getId()); response.setRepairNo(entity.getRepairNo());
        response.setSourceReportId(entity.getSourceReportId()); response.setTaskId(entity.getTaskId()); response.setBatchNo(entity.getBatchNo());
        response.setDefectQuantity(entity.getDefectQuantity()); response.setRepairQuantity(entity.getRepairQuantity());
        response.setStatus(entity.getStatus().name()); response.setReason(entity.getReason()); response.setAssigneeId(entity.getAssigneeId());
        response.setRecheckResult(entity.getRecheckResult()); response.setRecheckQuantity(entity.getRecheckQuantity());
        response.setCreatedTime(entity.getCreatedTime()); response.setUpdatedTime(entity.getUpdatedTime()); return response;
    }

    private void requireStatus(SceneRepairWorkOrderEntity entity, SceneRepairStatusEnum status) {
        if (entity.getStatus() != status) throw new ServiceException(SceneErrorCodeConstants.REPAIR_STATUS_INVALID);
    }
    private void touch(SceneRepairWorkOrderEntity entity) { entity.setUpdatedTime(LocalDateTime.now()); workOrderRepository.save(entity); }
}
