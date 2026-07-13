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
import com.badminton.mes.module.integration.dal.entity.DeviceCountExceptionEntity;
import com.badminton.mes.module.integration.dal.entity.DeviceCountRecordEntity;
import com.badminton.mes.module.integration.dal.entity.IntegrationWriteLogEntity;
import com.badminton.mes.module.integration.dal.repository.DeviceCountExceptionRepository;
import com.badminton.mes.module.integration.dal.repository.DeviceCountExceptionSpecifications;
import com.badminton.mes.module.integration.dal.repository.DeviceCountRecordRepository;
import com.badminton.mes.module.integration.dal.repository.IntegrationWriteLogRepository;
import com.badminton.mes.module.integration.enums.DeviceCountExceptionTypeEnum;
import com.badminton.mes.module.integration.enums.IntegrationInterfaceTypeEnum;
import com.badminton.mes.module.integration.enums.IntegrationWriteStatusEnum;
import com.badminton.mes.module.production.dal.entity.DispatchOrderEntity;
import com.badminton.mes.module.production.dal.repository.DispatchOrderRepository;
import com.badminton.mes.module.production.enums.DispatchStatusEnum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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

    private final DeviceCountExceptionRepository exceptionRepository;

    private final IntegrationWriteLogRepository writeLogRepository;

    private final DispatchOrderRepository dispatchOrderRepository;

    private final CraftProcessRepository craftProcessRepository;

    private final IntegrationAuditService auditService;

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
    public DeviceCountWriteCommandService(
            DeviceCountRecordRepository recordRepository,
            DeviceCountExceptionRepository exceptionRepository,
            IntegrationWriteLogRepository writeLogRepository,
            DispatchOrderRepository dispatchOrderRepository,
            CraftProcessRepository craftProcessRepository,
            IntegrationAuditService auditService) {
        this.recordRepository = recordRepository;
        this.exceptionRepository = exceptionRepository;
        this.writeLogRepository = writeLogRepository;
        this.dispatchOrderRepository = dispatchOrderRepository;
        this.craftProcessRepository = craftProcessRepository;
        this.auditService = auditService;
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
            return processDeviceCount(reqVO, snapshot, sourceSystem, externalKey);
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
        Specification<DeviceCountExceptionEntity> specification =
                DeviceCountExceptionSpecifications.page(reqVO);
        long total = exceptionRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }

        int pageSize = reqVO.getPageSize();
        int pageNo = normalizePageNo(reqVO.getPageNo(), pageSize, total);
        PageRequest pageRequest = PageRequest.of(
                pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<DeviceCountExceptionEntity> page =
                exceptionRepository.findAll(specification, pageRequest);
        return PageResult.of(page.getContent().stream().map(this::toExceptionRespVO).toList(),
                total, pageNo, pageSize);
    }

    private IntegrationWriteResultRespVO processDeviceCount(
            DeviceCountWriteReqVO reqVO,
            String snapshot,
            String sourceSystem,
            String externalKey) {
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
                    IntegrationErrorCodeConstants.DEVICE_COUNT_DISPATCH_NOT_FOUND);
        }

        Optional<IntegrationWriteLogEntity> existing =
                findProcessedLog(sourceSystem, externalKey);
        if (existing.isPresent()) {
            return toDuplicateResult(existing.get());
        }

        if (!isCountableDispatchStatus(dispatchOrder.getDispatchStatus())) {
            return saveException(reqVO, snapshot, sourceSystem, externalKey,
                    equipmentCode, dispatchNo, processCode, dispatchOrder.getId(), null,
                    DeviceCountExceptionTypeEnum.DISPATCH_STATUS_INVALID,
                    IntegrationErrorCodeConstants.DEVICE_COUNT_DISPATCH_STATUS_INVALID);
        }

        CraftProcessEntity process = craftProcessRepository
                .findByProcessCodeAndDeletedFalseForUpdate(processCode)
                .orElse(null);
        if (process == null) {
            return saveException(reqVO, snapshot, sourceSystem, externalKey,
                    equipmentCode, dispatchNo, processCode, dispatchOrder.getId(), null,
                    DeviceCountExceptionTypeEnum.PROCESS_NOT_FOUND,
                    IntegrationErrorCodeConstants.DEVICE_COUNT_PROCESS_NOT_FOUND);
        }

        if (reqVO.getCountValue() <= 0L) {
            return saveException(reqVO, snapshot, sourceSystem, externalKey,
                    equipmentCode, dispatchNo, processCode, dispatchOrder.getId(), process.getId(),
                    DeviceCountExceptionTypeEnum.COUNT_NON_POSITIVE,
                    IntegrationErrorCodeConstants.DEVICE_COUNT_NON_POSITIVE);
        }

        Optional<DeviceCountRecordEntity> previous = recordRepository
                .findFirstBySourceSystemAndEquipmentCodeAndDispatchOrderIdAndProcessIdAndDeletedFalseOrderByIdDesc(
                        sourceSystem, equipmentCode, dispatchOrder.getId(), process.getId());
        if (previous.isPresent()
                && reqVO.getCountValue() < previous.get().getCountValue()) {
            return saveException(reqVO, snapshot, sourceSystem, externalKey,
                    equipmentCode, dispatchNo, processCode, dispatchOrder.getId(), process.getId(),
                    DeviceCountExceptionTypeEnum.COUNT_ROLLBACK,
                    IntegrationErrorCodeConstants.DEVICE_COUNT_ROLLBACK);
        }

        long incrementValue = previous
                .map(record -> reqVO.getCountValue() - record.getCountValue())
                .orElse(reqVO.getCountValue());
        return saveSuccess(reqVO, snapshot, sourceSystem, externalKey,
                equipmentCode, dispatchOrder, process, incrementValue);
    }

    private IntegrationWriteResultRespVO saveSuccess(
            DeviceCountWriteReqVO reqVO,
            String snapshot,
            String sourceSystem,
            String externalKey,
            String equipmentCode,
            DispatchOrderEntity dispatchOrder,
            CraftProcessEntity process,
            long incrementValue) {
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

        Long logId = auditService.recordResult(
                IntegrationInterfaceTypeEnum.DEVICE_COUNT_WRITE,
                sourceSystem,
                externalKey,
                snapshot,
                IntegrationWriteStatusEnum.SUCCESS,
                record.getId(),
                externalKey);
        IntegrationWriteResultRespVO result = new IntegrationWriteResultRespVO();
        result.setLogId(logId);
        result.setStatus(IntegrationWriteStatusEnum.SUCCESS.getCode());
        result.setBusinessId(record.getId());
        result.setBusinessNo(externalKey);
        result.setMessage("设备计数写入成功");
        return result;
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
            ErrorCode errorCode) {
        DeviceCountExceptionEntity exception = new DeviceCountExceptionEntity();
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

    private DeviceCountExceptionRespVO toExceptionRespVO(DeviceCountExceptionEntity entity) {
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
        response.setExceptionType(entity.getExceptionType());
        response.setErrorCode(entity.getErrorCode());
        response.setErrorMessage(entity.getErrorMessage());
        response.setHandleStatus(entity.getHandleStatus());
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

    private int normalizePageNo(int requestedPageNo, int pageSize, long total) {
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        return Math.min(requestedPageNo, totalPages);
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
