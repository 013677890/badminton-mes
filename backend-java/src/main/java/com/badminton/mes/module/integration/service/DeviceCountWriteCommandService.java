package com.badminton.mes.module.integration.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

import com.badminton.mes.common.core.ErrorCode;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.craft.dal.entity.CraftProcessEntity;
import com.badminton.mes.module.craft.dal.repository.CraftProcessRepository;
import com.badminton.mes.module.integration.constants.IntegrationErrorCodeConstants;
import com.badminton.mes.module.integration.controller.vo.DeviceCountExceptionPageReqVO;
import com.badminton.mes.module.integration.controller.vo.DeviceCountExceptionRespVO;
import com.badminton.mes.module.integration.controller.vo.DeviceCountWriteReqVO;
import com.badminton.mes.module.integration.controller.vo.IntegrationWriteResultRespVO;
import com.badminton.mes.module.integration.dal.entity.IntegrationDeviceCountExceptionEntity;
import com.badminton.mes.module.integration.dal.entity.DeviceCountRecordEntity;
import com.badminton.mes.module.integration.dal.entity.EquipmentBindingEntity;
import com.badminton.mes.module.integration.dal.entity.IntegrationWriteLogEntity;
import com.badminton.mes.module.integration.dal.repository.IntegrationDeviceCountExceptionRepository;
import com.badminton.mes.module.integration.dal.repository.DeviceCountExceptionSpecifications;
import com.badminton.mes.module.integration.dal.repository.DeviceCountRecordRepository;
import com.badminton.mes.module.integration.dal.repository.EquipmentBindingRepository;
import com.badminton.mes.module.integration.dal.repository.IntegrationWriteLogRepository;
import com.badminton.mes.module.integration.enums.DeviceCountExceptionTypeEnum;
import com.badminton.mes.module.integration.enums.IntegrationInterfaceTypeEnum;
import com.badminton.mes.module.integration.enums.IntegrationWriteStatusEnum;
import com.badminton.mes.module.production.dal.entity.DispatchOrderEntity;
import com.badminton.mes.module.production.dal.repository.DispatchOrderRepository;
import com.badminton.mes.module.production.enums.DispatchStatusEnum;
import com.badminton.mes.module.scene.service.SceneWorkReportService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 设备累计计数写入命令服务。
 *
 * <p>同一事务内完成派工单锁定、累计值校验、成功记录或异常池写入及接口日志。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Service
public class DeviceCountWriteCommandService {

    private static final Logger logger =
            LoggerFactory.getLogger(DeviceCountWriteCommandService.class);

    private static final String RECORD_IDEMPOTENCY_CONSTRAINT =
            "uk_device_count_source_external";

    private static final String EXCEPTION_IDEMPOTENCY_CONSTRAINT =
            "uk_device_exception_source_external";

    private static final String LOG_IDEMPOTENCY_CONSTRAINT =
            "uk_device_count_idempotency_key";

    private final DeviceCountRecordRepository recordRepository;

    private final IntegrationDeviceCountExceptionRepository exceptionRepository;

    private final IntegrationWriteLogRepository writeLogRepository;

    private final DispatchOrderRepository dispatchOrderRepository;

    private final CraftProcessRepository craftProcessRepository;

    private final IntegrationAuditService auditService;

    private final EquipmentBindingRepository equipmentBindingRepository;

    private final SceneWorkReportService sceneWorkReportService;

    /**
     * 构造设备计数写入命令服务。
     *
     * @param recordRepository       成功计数 Repository
     * @param exceptionRepository    异常池 Repository
     * @param writeLogRepository     接口日志 Repository
     * @param dispatchOrderRepository 派工单 Repository
     * @param craftProcessRepository 工序 Repository
     * @param auditService           接口审计服务
     */
    @Autowired
    public DeviceCountWriteCommandService(
            DeviceCountRecordRepository recordRepository,
            IntegrationDeviceCountExceptionRepository exceptionRepository,
            IntegrationWriteLogRepository writeLogRepository,
            DispatchOrderRepository dispatchOrderRepository,
            CraftProcessRepository craftProcessRepository,
            IntegrationAuditService auditService,
            EquipmentBindingRepository equipmentBindingRepository,
            SceneWorkReportService sceneWorkReportService) {
        this.recordRepository = recordRepository;
        this.exceptionRepository = exceptionRepository;
        this.writeLogRepository = writeLogRepository;
        this.dispatchOrderRepository = dispatchOrderRepository;
        this.craftProcessRepository = craftProcessRepository;
        this.auditService = auditService;
        this.equipmentBindingRepository = equipmentBindingRepository;
        this.sceneWorkReportService = sceneWorkReportService;
    }

    /** 兼容既有聚焦单测的构造入口。 */
    public DeviceCountWriteCommandService(
            DeviceCountRecordRepository recordRepository,
            IntegrationDeviceCountExceptionRepository exceptionRepository,
            IntegrationWriteLogRepository writeLogRepository,
            DispatchOrderRepository dispatchOrderRepository,
            CraftProcessRepository craftProcessRepository,
            IntegrationAuditService auditService) {
        this(recordRepository, exceptionRepository, writeLogRepository,
                dispatchOrderRepository, craftProcessRepository, auditService, null, null);
    }

    /**
     * 幂等写入一条设备累计计数。
     *
     * @param reqVO    写入请求
     * @param snapshot 原始请求快照
     * @return 成功、失败或重复写入结果
     */
    @Transactional(rollbackFor = Exception.class)
    public IntegrationWriteResultRespVO writeDeviceCount(
            DeviceCountWriteReqVO reqVO, String snapshot) {
        String sourceSystem = normalizeCode(reqVO.getSourceSystem());
        String externalKey = normalizeCode(reqVO.getExternalKey());

        try {
            return processDeviceCount(reqVO, snapshot, sourceSystem, externalKey, null);
        } catch (DataIntegrityViolationException exception) {
            if (isIdempotencyConflict(exception)) {
                throw new ServiceException(IntegrationErrorCodeConstants.DEVICE_COUNT_DUPLICATE);
            }
            logger.error(
                    "[设备计数写入冲突] sourceSystem: {}, externalKey: {}, errorMessage: {}",
                    sourceSystem, externalKey, exception.getMessage(), exception);
            throw new ServiceException(IntegrationErrorCodeConstants.WRITE_CONFLICT);
        }
    }

    /**
     * 查询已处理设备计数日志，供并发唯一键竞争回滚后恢复稳定结果。
     *
     * @param sourceSystem 来源系统
     * @param externalKey  外部幂等键
     * @return 已处理日志
     */
    @Transactional(readOnly = true)
    public Optional<IntegrationWriteLogEntity> findProcessedLog(
            String sourceSystem, String externalKey) {
        return writeLogRepository
                .findFirstByInterfaceTypeAndSourceSystemAndBusinessKeyOrderByIdDesc(
                        IntegrationInterfaceTypeEnum.DEVICE_COUNT_WRITE.getValue(),
                        normalizeCode(sourceSystem), normalizeCode(externalKey));
    }

    /**
     * 分页查询设备计数异常池。
     *
     * @param reqVO 分页筛选条件
     * @return 异常池分页
     */
    @Transactional(readOnly = true)
    public PageResult<DeviceCountExceptionRespVO> getExceptionPage(
            DeviceCountExceptionPageReqVO reqVO) {
        validateExceptionTimeRange(reqVO);
        Specification<IntegrationDeviceCountExceptionEntity> specification =
                DeviceCountExceptionSpecifications.page(reqVO);
        long total = exceptionRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }

        int pageSize = reqVO.getPageSize();
        int pageNo = normalizePageNo(reqVO.getPageNo(), pageSize, total);
        PageRequest pageRequest = PageRequest.of(
                pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<IntegrationDeviceCountExceptionEntity> page =
                exceptionRepository.findAll(specification, pageRequest);
        return PageResult.of(page.getContent().stream().map(this::toExceptionRespVO).toList(),
                total, pageNo, pageSize);
    }

    private IntegrationWriteResultRespVO processDeviceCount(
            DeviceCountWriteReqVO reqVO,
            String snapshot,
            String sourceSystem,
            String externalKey,
            RetryContext retryContext) {
        String equipmentCode = normalizeCode(reqVO.getEquipmentCode());
        String dispatchNo = normalizeCode(reqVO.getDispatchNo());
        String processCode = normalizeCode(reqVO.getProcessCode());
        DispatchOrderEntity dispatchOrder = dispatchOrderRepository
                .findByDispatchNoForUpdate(dispatchNo)
                .orElse(null);
        if (dispatchOrder == null) {
            return saveException(reqVO, snapshot, sourceSystem, externalKey,
                    equipmentCode, dispatchNo, processCode, null, null,
                    DeviceCountExceptionTypeEnum.DISPATCH_NOT_FOUND,
                    IntegrationErrorCodeConstants.DEVICE_COUNT_DISPATCH_NOT_FOUND, retryContext);
        }

        Optional<IntegrationWriteLogEntity> existing =
                findProcessedLog(sourceSystem, externalKey);
        if (existing.isPresent()
                && (retryContext == null
                || !existing.get().getId().equals(retryContext.log().getId()))) {
            return toDuplicateResult(existing.get());
        }

        if (!isCountableDispatchStatus(dispatchOrder.getDispatchStatus())) {
            return saveException(reqVO, snapshot, sourceSystem, externalKey,
                    equipmentCode, dispatchNo, processCode, dispatchOrder.getId(), null,
                    DeviceCountExceptionTypeEnum.DISPATCH_STATUS_INVALID,
                    IntegrationErrorCodeConstants.DEVICE_COUNT_DISPATCH_STATUS_INVALID,
                    retryContext);
        }

        CraftProcessEntity process = craftProcessRepository
                .findByProcessCodeAndDeletedFalseForUpdate(processCode)
                .orElse(null);
        if (process == null) {
            return saveException(reqVO, snapshot, sourceSystem, externalKey,
                    equipmentCode, dispatchNo, processCode, dispatchOrder.getId(), null,
                    DeviceCountExceptionTypeEnum.PROCESS_NOT_FOUND,
                    IntegrationErrorCodeConstants.DEVICE_COUNT_PROCESS_NOT_FOUND, retryContext);
        }

        BindingValidation bindingValidation = validateEquipmentBinding(
                reqVO, snapshot, sourceSystem, externalKey, equipmentCode,
                dispatchOrder, process, dispatchNo, processCode, retryContext);
        if (bindingValidation.failureResult() != null) {
            return bindingValidation.failureResult();
        }
        EquipmentBindingEntity binding = bindingValidation.binding();

        if (reqVO.getCountValue() <= 0L) {
            return saveException(reqVO, snapshot, sourceSystem, externalKey,
                    equipmentCode, dispatchNo, processCode, dispatchOrder.getId(), process.getId(),
                    DeviceCountExceptionTypeEnum.COUNT_NON_POSITIVE,
                    IntegrationErrorCodeConstants.DEVICE_COUNT_NON_POSITIVE, retryContext);
        }

        Optional<DeviceCountRecordEntity> previous = recordRepository
                .findFirstBySourceSystemAndEquipmentCodeAndDispatchOrderIdAndProcessIdAndDeletedFalseOrderByIdDesc(
                        sourceSystem, equipmentCode, dispatchOrder.getId(), process.getId());
        if (previous.isPresent()
                && reqVO.getCountValue() < previous.get().getCountValue()) {
            return saveException(reqVO, snapshot, sourceSystem, externalKey,
                    equipmentCode, dispatchNo, processCode, dispatchOrder.getId(), process.getId(),
                    DeviceCountExceptionTypeEnum.COUNT_ROLLBACK,
                    IntegrationErrorCodeConstants.DEVICE_COUNT_ROLLBACK, retryContext);
        }

        long incrementValue = previous
                .map(record -> reqVO.getCountValue() - record.getCountValue())
                .orElse(reqVO.getCountValue());
        if (binding != null && incrementValue > binding.getMaxIncrement()) {
            return saveException(reqVO, snapshot, sourceSystem, externalKey,
                    equipmentCode, dispatchNo, processCode, dispatchOrder.getId(), process.getId(),
                    DeviceCountExceptionTypeEnum.COUNT_JUMP,
                    IntegrationErrorCodeConstants.DEVICE_COUNT_JUMP, retryContext);
        }
        return saveSuccess(reqVO, snapshot, sourceSystem, externalKey,
                equipmentCode, dispatchOrder, process, incrementValue, binding, retryContext);
    }

    private IntegrationWriteResultRespVO saveSuccess(
            DeviceCountWriteReqVO reqVO,
            String snapshot,
            String sourceSystem,
            String externalKey,
            String equipmentCode,
            DispatchOrderEntity dispatchOrder,
            CraftProcessEntity process,
            long incrementValue,
            EquipmentBindingEntity binding,
            RetryContext retryContext) {
        DeviceCountRecordEntity record = new DeviceCountRecordEntity();
        record.setSourceSystem(sourceSystem);
        record.setExternalKey(externalKey);
        record.setEquipmentCode(equipmentCode);
        record.setDispatchOrderId(dispatchOrder.getId());
        record.setDispatchNo(dispatchOrder.getDispatchNo());
        record.setProcessId(process.getId());
        record.setProcessCode(process.getProcessCode());
        record.setCollectTime(reqVO.getCollectTime());
        record.setCountValue(reqVO.getCountValue());
        record.setIncrementValue(incrementValue);
        record.setCreateBy(SecurityContextHolder.getRequiredLoginUserId());
        recordRepository.saveAndFlush(record);

        if (binding != null && sceneWorkReportService != null) {
            Long reportId = sceneWorkReportService.createDeviceReport(record, binding, dispatchOrder);
            record.setWorkReportId(reportId);
            recordRepository.save(record);
        }

        Long logId = retryContext == null
                ? auditService.recordResult(
                        IntegrationInterfaceTypeEnum.DEVICE_COUNT_WRITE,
                        sourceSystem, externalKey, snapshot,
                        IntegrationWriteStatusEnum.SUCCESS, record.getId(), externalKey)
                : auditService.replaceFailureResult(
                        retryContext.log().getId(), snapshot,
                        IntegrationWriteStatusEnum.SUCCESS, record.getId(), externalKey, null);
        IntegrationWriteResultRespVO result = new IntegrationWriteResultRespVO();
        result.setLogId(logId);
        result.setStatus(IntegrationWriteStatusEnum.SUCCESS.getCode());
        result.setBusinessId(record.getId());
        result.setBusinessNo(externalKey);
        result.setMessage("设备计数写入成功");
        return result;
    }

    private BindingValidation validateEquipmentBinding(
            DeviceCountWriteReqVO reqVO,
            String snapshot,
            String sourceSystem,
            String externalKey,
            String equipmentCode,
            DispatchOrderEntity dispatchOrder,
            CraftProcessEntity process,
            String dispatchNo,
            String processCode,
            RetryContext retryContext) {
        if (equipmentBindingRepository == null) {
            return new BindingValidation(null, null);
        }
        EquipmentBindingEntity binding = equipmentBindingRepository
                .findByEquipmentCodeAndDeletedFalse(equipmentCode)
                .filter(item -> Integer.valueOf(1).equals(item.getStatus()))
                .orElse(null);
        if (binding == null) {
            return failedBinding(reqVO, snapshot, sourceSystem, externalKey, equipmentCode,
                    dispatchNo, processCode, dispatchOrder, process,
                    DeviceCountExceptionTypeEnum.EQUIPMENT_NOT_BOUND,
                    IntegrationErrorCodeConstants.DEVICE_BINDING_NOT_AVAILABLE, retryContext);
        }
        if (!binding.getLineId().equals(dispatchOrder.getLineId())) {
            return failedBinding(reqVO, snapshot, sourceSystem, externalKey, equipmentCode,
                    dispatchNo, processCode, dispatchOrder, process,
                    DeviceCountExceptionTypeEnum.LINE_MISMATCH,
                    IntegrationErrorCodeConstants.DEVICE_BINDING_LINE_MISMATCH, retryContext);
        }
        if (binding.getProcessId() != null && !binding.getProcessId().equals(process.getId())) {
            return failedBinding(reqVO, snapshot, sourceSystem, externalKey, equipmentCode,
                    dispatchNo, processCode, dispatchOrder, process,
                    DeviceCountExceptionTypeEnum.PROCESS_MISMATCH,
                    IntegrationErrorCodeConstants.DEVICE_BINDING_PROCESS_MISMATCH, retryContext);
        }
        return new BindingValidation(binding, null);
    }

    private BindingValidation failedBinding(
            DeviceCountWriteReqVO reqVO,
            String snapshot,
            String sourceSystem,
            String externalKey,
            String equipmentCode,
            String dispatchNo,
            String processCode,
            DispatchOrderEntity dispatchOrder,
            CraftProcessEntity process,
            DeviceCountExceptionTypeEnum exceptionType,
            ErrorCode errorCode,
            RetryContext retryContext) {
        IntegrationWriteResultRespVO result = saveException(
                reqVO, snapshot, sourceSystem, externalKey, equipmentCode,
                dispatchNo, processCode, dispatchOrder.getId(), process.getId(),
                exceptionType, errorCode, retryContext);
        return new BindingValidation(null, result);
    }

    private record BindingValidation(
            EquipmentBindingEntity binding,
            IntegrationWriteResultRespVO failureResult) {
    }

    private IntegrationWriteResultRespVO saveException(
            DeviceCountWriteReqVO reqVO,
            String snapshot,
            String sourceSystem,
            String externalKey,
            String equipmentCode,
            String dispatchNo,
            String processCode,
            Long dispatchOrderId,
            Long processId,
            DeviceCountExceptionTypeEnum exceptionType,
            ErrorCode errorCode,
            RetryContext retryContext) {
        if (retryContext != null) {
            return refreshRetryFailure(reqVO, snapshot, sourceSystem, externalKey,
                    equipmentCode, dispatchNo, processCode, dispatchOrderId, processId,
                    exceptionType, errorCode, retryContext);
        }
        IntegrationDeviceCountExceptionEntity exception = new IntegrationDeviceCountExceptionEntity();
        exception.setSourceSystem(sourceSystem);
        exception.setExternalKey(externalKey);
        exception.setEquipmentCode(equipmentCode);
        exception.setDispatchOrderId(dispatchOrderId);
        exception.setDispatchNo(dispatchNo);
        exception.setProcessId(processId);
        exception.setProcessCode(processCode);
        exception.setCollectTime(reqVO.getCollectTime());
        exception.setCountValue(reqVO.getCountValue());
        exception.setRequestSnapshot(snapshot);
        exception.setExceptionType(exceptionType.getValue());
        exception.setErrorCode(errorCode.code());
        exception.setErrorMessage(errorCode.message());
        exception.setHandleStatus(0);
        exception.setCreateBy(SecurityContextHolder.getRequiredLoginUserId());
        exceptionRepository.saveAndFlush(exception);

        Long logId = auditService.recordFailureInCurrentTransaction(
                IntegrationInterfaceTypeEnum.DEVICE_COUNT_WRITE,
                sourceSystem,
                externalKey,
                snapshot,
                exception.getId(),
                externalKey,
                errorCode,
                errorCode.message());
        IntegrationWriteResultRespVO result = new IntegrationWriteResultRespVO();
        result.setLogId(logId);
        result.setStatus(IntegrationWriteStatusEnum.FAILED.getCode());
        result.setBusinessId(exception.getId());
        result.setBusinessNo(externalKey);
        result.setErrorCode(errorCode.code());
        result.setMessage(errorCode.message());
        return result;
    }

    private IntegrationWriteResultRespVO refreshRetryFailure(
            DeviceCountWriteReqVO reqVO,
            String snapshot,
            String sourceSystem,
            String externalKey,
            String equipmentCode,
            String dispatchNo,
            String processCode,
            Long dispatchOrderId,
            Long processId,
            DeviceCountExceptionTypeEnum exceptionType,
            ErrorCode errorCode,
            RetryContext retryContext) {
        IntegrationDeviceCountExceptionEntity exception = retryContext.exception();
        exception.setEquipmentCode(equipmentCode);
        exception.setDispatchOrderId(dispatchOrderId);
        exception.setDispatchNo(dispatchNo);
        exception.setProcessId(processId);
        exception.setProcessCode(processCode);
        exception.setCollectTime(reqVO.getCollectTime());
        exception.setCountValue(reqVO.getCountValue());
        exception.setRetryRequestSnapshot(snapshot);
        exception.setExceptionType(exceptionType.getValue());
        exception.setErrorCode(errorCode.code());
        exception.setErrorMessage(errorCode.message());
        exceptionRepository.save(exception);
        Long logId = auditService.replaceFailureResult(
                retryContext.log().getId(), snapshot, IntegrationWriteStatusEnum.FAILED,
                exception.getId(), externalKey, errorCode);
        IntegrationWriteResultRespVO result = new IntegrationWriteResultRespVO();
        result.setLogId(logId);
        result.setStatus(IntegrationWriteStatusEnum.FAILED.getCode());
        result.setBusinessId(exception.getId());
        result.setBusinessNo(externalKey);
        result.setErrorCode(errorCode.code());
        result.setMessage(errorCode.message());
        return result;
    }

    private IntegrationWriteResultRespVO toDuplicateResult(IntegrationWriteLogEntity log) {
        IntegrationWriteResultRespVO result = new IntegrationWriteResultRespVO();
        result.setLogId(log.getId());
        result.setStatus(IntegrationWriteStatusEnum.DUPLICATE.getCode());
        result.setBusinessId(log.getResultId());
        result.setBusinessNo(log.getResultNo());
        result.setErrorCode(log.getErrorCode());
        result.setMessage("重复请求，沿用原设备计数处理结果");
        return result;
    }

    private DeviceCountExceptionRespVO toExceptionRespVO(IntegrationDeviceCountExceptionEntity entity) {
        DeviceCountExceptionRespVO response = new DeviceCountExceptionRespVO();
        response.setId(entity.getId());
        response.setSourceSystem(entity.getSourceSystem());
        response.setExternalKey(entity.getExternalKey());
        response.setEquipmentCode(entity.getEquipmentCode());
        response.setDispatchNo(entity.getDispatchNo());
        response.setProcessCode(entity.getProcessCode());
        response.setCollectTime(entity.getCollectTime());
        response.setCountValue(entity.getCountValue());
        response.setRequestSnapshot(entity.getRequestSnapshot());
        response.setRetryRequestSnapshot(entity.getRetryRequestSnapshot());
        response.setExceptionType(entity.getExceptionType());
        response.setErrorCode(entity.getErrorCode());
        response.setErrorMessage(entity.getErrorMessage());
        response.setHandleStatus(entity.getHandleStatus());
        response.setHandleBy(entity.getHandleBy());
        response.setHandleTime(entity.getHandleTime());
        response.setHandleRemark(entity.getHandleRemark());
        response.setRetryLogId(entity.getRetryLogId());
        response.setRetryRecordId(entity.getRetryRecordId());
        response.setCreateTime(entity.getCreateTime());
        response.setUpdateTime(entity.getUpdateTime());
        return response;
    }

    private boolean isCountableDispatchStatus(Integer status) {
        return DispatchStatusEnum.ISSUED.getStatus().equals(status)
                || DispatchStatusEnum.EXECUTING.getStatus().equals(status);
    }

    private boolean isIdempotencyConflict(DataIntegrityViolationException exception) {
        return containsConstraint(exception, RECORD_IDEMPOTENCY_CONSTRAINT)
                || containsConstraint(exception, EXCEPTION_IDEMPOTENCY_CONSTRAINT)
                || containsConstraint(exception, LOG_IDEMPOTENCY_CONSTRAINT);
    }

    private boolean containsConstraint(Throwable throwable, String constraintName) {
        Throwable current = throwable;
        String expected = constraintName.toLowerCase(Locale.ROOT);
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains(expected)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void validateExceptionTimeRange(DeviceCountExceptionPageReqVO reqVO) {
        LocalDateTime startTime = reqVO.getStartTime();
        LocalDateTime endTime = reqVO.getEndTime();
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new ServiceException(
                    IntegrationErrorCodeConstants.DEVICE_EXCEPTION_TIME_RANGE_INVALID);
        }
    }

    /** 将待处理异常标记为忽略。 */
    @Transactional(rollbackFor = Exception.class)
    public void ignoreException(Long id, String remark) {
        IntegrationDeviceCountExceptionEntity exception = requirePendingException(id);
        exception.setHandleStatus(2);
        exception.setHandleBy(SecurityContextHolder.getRequiredLoginUserId());
        exception.setHandleTime(LocalDateTime.now());
        exception.setHandleRemark(remark);
        exceptionRepository.save(exception);
    }

    /** 使用修正后的请求重新处理异常，成功后关闭原异常。 */
    @Transactional(rollbackFor = Exception.class)
    public IntegrationWriteResultRespVO retryException(Long id, DeviceCountWriteReqVO reqVO) {
        return retryException(id, reqVO, auditService.serializeRequest(reqVO));
    }

    /** 使用指定审计快照重新处理异常，保留供聚焦测试调用。 */
    @Transactional(rollbackFor = Exception.class)
    public IntegrationWriteResultRespVO retryException(
            Long id, DeviceCountWriteReqVO reqVO, String snapshot) {
        IntegrationDeviceCountExceptionEntity exception = requirePendingException(id);
        String sourceSystem = normalizeCode(reqVO.getSourceSystem());
        String externalKey = normalizeCode(reqVO.getExternalKey());
        Optional<IntegrationWriteLogEntity> existingLog = findProcessedLog(
                sourceSystem, externalKey);
        boolean retriesOriginalFailure = sourceSystem.equals(exception.getSourceSystem())
                && externalKey.equals(exception.getExternalKey())
                && existingLog.isPresent()
                && IntegrationWriteStatusEnum.FAILED.getStatus()
                        .equals(existingLog.get().getWriteStatus());
        IntegrationWriteResultRespVO result = retriesOriginalFailure
                ? processDeviceCount(reqVO, snapshot, sourceSystem, externalKey,
                        new RetryContext(exception, existingLog.get()))
                : writeDeviceCount(reqVO, snapshot);
        boolean processed = IntegrationWriteStatusEnum.SUCCESS.getCode().equals(result.getStatus())
                || (IntegrationWriteStatusEnum.DUPLICATE.getCode().equals(result.getStatus())
                && result.getErrorCode() == null);
        if (processed) {
            exception.setHandleStatus(1);
            exception.setHandleBy(SecurityContextHolder.getRequiredLoginUserId());
            exception.setHandleTime(LocalDateTime.now());
            exception.setHandleRemark("修正后重新处理成功");
            exception.setRetryRequestSnapshot(snapshot);
            exception.setRetryLogId(result.getLogId());
            exception.setRetryRecordId(result.getBusinessId());
            exceptionRepository.save(exception);
        }
        return result;
    }

    private record RetryContext(
            IntegrationDeviceCountExceptionEntity exception,
            IntegrationWriteLogEntity log) {
    }

    private IntegrationDeviceCountExceptionEntity requirePendingException(Long id) {
        IntegrationDeviceCountExceptionEntity exception = exceptionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ServiceException(
                        IntegrationErrorCodeConstants.DEVICE_EXCEPTION_NOT_EXISTS));
        if (!Integer.valueOf(0).equals(exception.getHandleStatus())) {
            throw new ServiceException(IntegrationErrorCodeConstants.DEVICE_EXCEPTION_STATUS_INVALID);
        }
        return exception;
    }

    private int normalizePageNo(int requestedPageNo, int pageSize, long total) {
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        return Math.min(requestedPageNo, totalPages);
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
