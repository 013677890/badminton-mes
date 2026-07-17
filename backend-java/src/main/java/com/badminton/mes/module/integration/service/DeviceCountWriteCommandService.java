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
 * <p>同一事务内完成派工单锁定、累计值校验、成功记录或异常池写入及接口日志。设备上报值是
 * 单调递增的累计计数，服务根据上一条记录计算本次增量，并结合设备绑定阈值识别回退和跳变。
 * 业务异常不会简单丢弃，而是连同请求快照进入异常池；人工修正重试时原位更新失败日志，保证
 * 外部幂等键始终只有一个最终处理结果。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Service
public class DeviceCountWriteCommandService {

    /** 记录无法归类为已知幂等约束的数据库完整性冲突。 */
    private static final Logger logger =
            LoggerFactory.getLogger(DeviceCountWriteCommandService.class);

    /** 成功计数表的来源系统与外部键唯一索引名称。 */
    private static final String RECORD_IDEMPOTENCY_CONSTRAINT =
            "uk_device_count_source_external";

    /** 异常池表的来源系统与外部键唯一索引名称。 */
    private static final String EXCEPTION_IDEMPOTENCY_CONSTRAINT =
            "uk_device_exception_source_external";

    /** 接口日志表中设备计数幂等键唯一索引名称。 */
    private static final String LOG_IDEMPOTENCY_CONSTRAINT =
            "uk_device_count_idempotency_key";

    /** 成功计数仓储，用于读取上一累计值并保存本次计数及增量。 */
    private final DeviceCountRecordRepository recordRepository;

    /** 设备计数异常池仓储，负责异常暂存、分页和处理状态锁定。 */
    private final IntegrationDeviceCountExceptionRepository exceptionRepository;

    /** 接口写入日志仓储，用于幂等结果回查和修正重试定位。 */
    private final IntegrationWriteLogRepository writeLogRepository;

    /** 派工单仓储，按单号加写锁稳定任务状态及累计计数处理顺序。 */
    private final DispatchOrderRepository dispatchOrderRepository;

    /** 工序仓储，按编码加写锁验证设备计数归属的有效工序。 */
    private final CraftProcessRepository craftProcessRepository;

    /** 审计服务，协调成功、失败及重试结果与业务记录的事务一致性。 */
    private final IntegrationAuditService auditService;

    /** 设备绑定仓储，用于校验设备所属产线、工序范围和单次增量阈值。 */
    private final EquipmentBindingRepository equipmentBindingRepository;

    /** 现场报工服务，绑定配置启用时将有效计数增量自动转换为设备报工。 */
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
        // 来源系统和外部键统一规范化，避免大小写或空白差异绕过幂等约束。
        String sourceSystem = normalizeCode(reqVO.getSourceSystem());
        String externalKey = normalizeCode(reqVO.getExternalKey());

        try {
            // 普通写入不携带重试上下文，由处理流程创建新的成功记录或异常池记录。
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
        // 时间区间属于跨字段约束，必须在生成 Criteria 条件前显式检查先后顺序。
        validateExceptionTimeRange(reqVO);
        Specification<IntegrationDeviceCountExceptionEntity> specification =
                DeviceCountExceptionSpecifications.page(reqVO);
        // 先聚合总数，空结果时跳过列表查询，减少一次数据库往返。
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

    /**
     * 执行设备累计计数的完整校验和落库流程。
     *
     * <p>先锁派工单，再检查接口幂等结果、派工状态、工序和设备绑定，最后基于上一累计值计算增量。
     * 任一业务校验失败都会转入异常池；重试上下文非空时原位刷新原异常与原失败日志。
     */
    private IntegrationWriteResultRespVO processDeviceCount(
            DeviceCountWriteReqVO reqVO,
            String snapshot,
            String sourceSystem,
            String externalKey,
            RetryContext retryContext) {
        // 设备、派工单和工序编码统一大写，确保绑定及历史累计查询使用稳定业务键。
        String equipmentCode = normalizeCode(reqVO.getEquipmentCode());
        String dispatchNo = normalizeCode(reqVO.getDispatchNo());
        String processCode = normalizeCode(reqVO.getProcessCode());
        // 悲观锁定派工单，使同一任务下的状态判断和累计增量计算在事务内保持稳定。
        DispatchOrderEntity dispatchOrder = dispatchOrderRepository
                .findByDispatchNoForUpdate(dispatchNo)
                .orElse(null);
        if (dispatchOrder == null) {
            return saveException(reqVO, snapshot, sourceSystem, externalKey,
                    equipmentCode, dispatchNo, processCode, null, null,
                    DeviceCountExceptionTypeEnum.DISPATCH_NOT_FOUND,
                    IntegrationErrorCodeConstants.DEVICE_COUNT_DISPATCH_NOT_FOUND, retryContext);
        }

        // 派工单存在后再查幂等日志；修正原失败时允许当前重试继续处理同一日志主键。
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

        // 工序按编码加锁读取，避免处理期间被并发停用或删除而产生失效计数归属。
        CraftProcessEntity process = craftProcessRepository
                .findByProcessCodeAndDeletedFalseForUpdate(processCode)
                .orElse(null);
        if (process == null) {
            return saveException(reqVO, snapshot, sourceSystem, externalKey,
                    equipmentCode, dispatchNo, processCode, dispatchOrder.getId(), null,
                    DeviceCountExceptionTypeEnum.PROCESS_NOT_FOUND,
                    IntegrationErrorCodeConstants.DEVICE_COUNT_PROCESS_NOT_FOUND, retryContext);
        }

        // 新版链路启用设备绑定后，设备必须与派工产线及可选工序范围一致。
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

        // 历史基线按“来源系统 + 设备 + 派工单 + 工序”读取最近一条有效累计值。
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

        // 首次上报的增量等于累计值；后续上报只取与上一累计值的差额。
        long incrementValue = previous
                .map(record -> reqVO.getCountValue() - record.getCountValue())
                .orElse(reqVO.getCountValue());
        // 单次增量超过绑定阈值视为计数跳变，防止设备重置或脏数据直接形成巨额报工。
        if (binding != null && incrementValue > binding.getMaxIncrement()) {
            return saveException(reqVO, snapshot, sourceSystem, externalKey,
                    equipmentCode, dispatchNo, processCode, dispatchOrder.getId(), process.getId(),
                    DeviceCountExceptionTypeEnum.COUNT_JUMP,
                    IntegrationErrorCodeConstants.DEVICE_COUNT_JUMP, retryContext);
        }
        return saveSuccess(reqVO, snapshot, sourceSystem, externalKey,
                equipmentCode, dispatchOrder, process, incrementValue, binding, retryContext);
    }

    /**
     * 保存通过校验的累计计数，并在配置完整时联动生成现场报工。
     *
     * <p>计数记录、自动报工和成功审计位于同一事务；修正重试时复用原失败日志主键并替换结果。
     */
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
        // 固化本次上报的原始累计值和计算后增量，二者分别服务设备追溯与生产报工。
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
        // 立即刷新以尽早触发成功表幂等唯一约束，避免重复请求继续进入自动报工。
        recordRepository.saveAndFlush(record);

        if (binding != null && sceneWorkReportService != null) {
            // 只有正式绑定链路才自动报工；兼容构造场景缺少依赖时仅保留计数记录。
            Long reportId = sceneWorkReportService.createDeviceReport(record, binding, dispatchOrder);
            // 回写报工主键建立计数来源到现场报工结果的可追溯关系。
            record.setWorkReportId(reportId);
            recordRepository.save(record);
        }

        // 首次成功新增日志；原异常修正成功则原位把 FAILED 日志替换为 SUCCESS。
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

    /**
     * 校验设备绑定是否存在、是否属于派工产线及是否允许当前工序。
     *
     * @return 成功时携带绑定实体；失败时携带已经写入异常池的接口结果
     */
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
            // 仅为旧版聚焦单测构造器保留兼容路径，生产注入场景始终具备绑定仓储。
            return new BindingValidation(null, null);
        }
        // 设备编码只允许命中一条未删除且启用的绑定，停用绑定按未绑定处理。
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
            // 设备不能把其他产线的累计值写入当前派工单，避免跨线串产量。
            return failedBinding(reqVO, snapshot, sourceSystem, externalKey, equipmentCode,
                    dispatchNo, processCode, dispatchOrder, process,
                    DeviceCountExceptionTypeEnum.LINE_MISMATCH,
                    IntegrationErrorCodeConstants.DEVICE_BINDING_LINE_MISMATCH, retryContext);
        }
        if (binding.getProcessId() != null && !binding.getProcessId().equals(process.getId())) {
            // 绑定未限定工序时可采集任意工序；已限定时必须与上报工序完全一致。
            return failedBinding(reqVO, snapshot, sourceSystem, externalKey, equipmentCode,
                    dispatchNo, processCode, dispatchOrder, process,
                    DeviceCountExceptionTypeEnum.PROCESS_MISMATCH,
                    IntegrationErrorCodeConstants.DEVICE_BINDING_PROCESS_MISMATCH, retryContext);
        }
        return new BindingValidation(binding, null);
    }

    /** 将绑定校验失败统一转换为异常池记录，并以包装结果终止后续累计值处理。 */
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

    /** 绑定校验的互斥结果：成功绑定或已生成的失败响应。 */
    private record BindingValidation(
            EquipmentBindingEntity binding,
            IntegrationWriteResultRespVO failureResult) {
    }

    /**
     * 保存首次设备计数异常及对应失败审计。
     *
     * <p>异常数据是待人工处理的业务记录，因此与 FAILED 日志在当前事务中原子提交；若属于原异常
     * 修正重试，则转交原位刷新逻辑，不创建第二条相同幂等键的异常。
     */
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
            // 原幂等键重试仍失败时只更新原异常和原日志，避免触发三张表的唯一约束。
            return refreshRetryFailure(reqVO, snapshot, sourceSystem, externalKey,
                    equipmentCode, dispatchNo, processCode, dispatchOrderId, processId,
                    exceptionType, errorCode, retryContext);
        }
        // 同时保存可检索字段和完整请求快照，便于异常池筛选、定位及人工修正。
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
        // 处理状态 0 表示待处理，后续只能在写锁保护下进入已处理或已忽略。
        exception.setHandleStatus(0);
        exception.setCreateBy(SecurityContextHolder.getRequiredLoginUserId());
        exceptionRepository.saveAndFlush(exception);

        // 异常记录需要保留，故失败日志加入当前事务而不是使用独立失败事务。
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

    /** 原幂等键修正后仍失败时，刷新原异常详情并原位替换失败日志内容。 */
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
        // 保留原始 requestSnapshot 作为首次失败证据，新的快照单独写入 retryRequestSnapshot。
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
        // 日志主键和幂等身份保持不变，仅更新本次快照及最新失败原因。
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

    /** 将既有审计结果转换为重复响应，不再次修改计数或异常数据。 */
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

    /** 将异常池实体显式转换为响应快照，隐藏内部关联主键和逻辑删除控制字段。 */
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

    /** 判断派工状态是否允许接收设备计数；未下达和终态任务均不接受新增产量。 */
    private boolean isCountableDispatchStatus(Integer status) {
        return DispatchStatusEnum.ISSUED.getStatus().equals(status)
                || DispatchStatusEnum.EXECUTING.getStatus().equals(status);
    }

    /** 判断数据库异常是否来自成功、异常或日志表任一设备计数幂等唯一索引。 */
    private boolean isIdempotencyConflict(DataIntegrityViolationException exception) {
        return containsConstraint(exception, RECORD_IDEMPOTENCY_CONSTRAINT)
                || containsConstraint(exception, EXCEPTION_IDEMPOTENCY_CONSTRAINT)
                || containsConstraint(exception, LOG_IDEMPOTENCY_CONSTRAINT);
    }

    /** 沿异常因果链匹配数据库约束名，兼容不同驱动的多层异常包装。 */
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

    /** 校验异常池查询结束时间不得早于开始时间。 */
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
        // requirePendingException 先加写锁，避免忽略与修正重试并发处理同一异常。
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
        // 锁定原异常，整个重试和处理状态回写期间不允许另一位操作人再次处理。
        IntegrationDeviceCountExceptionEntity exception = requirePendingException(id);
        String sourceSystem = normalizeCode(reqVO.getSourceSystem());
        String externalKey = normalizeCode(reqVO.getExternalKey());
        Optional<IntegrationWriteLogEntity> existingLog = findProcessedLog(
                sourceSystem, externalKey);
        // 只有来源系统、外部键和原 FAILED 日志都一致时，才允许原位覆盖原幂等结果。
        boolean retriesOriginalFailure = sourceSystem.equals(exception.getSourceSystem())
                && externalKey.equals(exception.getExternalKey())
                && existingLog.isPresent()
                && IntegrationWriteStatusEnum.FAILED.getStatus()
                        .equals(existingLog.get().getWriteStatus());
        // 改用新幂等键时按全新请求处理；沿用原键时携带重试上下文绕过自身重复判断。
        IntegrationWriteResultRespVO result = retriesOriginalFailure
                ? processDeviceCount(reqVO, snapshot, sourceSystem, externalKey,
                        new RetryContext(exception, existingLog.get()))
                : writeDeviceCount(reqVO, snapshot);
        // 成功或无错误码的重复结果都表示数据已有效处理，可关闭原异常。
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

    /** 将原异常与其唯一失败日志绑定，供重试流程原位更新两类记录。 */
    private record RetryContext(
            IntegrationDeviceCountExceptionEntity exception,
            IntegrationWriteLogEntity log) {
    }

    /** 悲观锁定一条待处理异常，已处理或已忽略记录不可再次操作。 */
    private IntegrationDeviceCountExceptionEntity requirePendingException(Long id) {
        IntegrationDeviceCountExceptionEntity exception = exceptionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ServiceException(
                        IntegrationErrorCodeConstants.DEVICE_EXCEPTION_NOT_EXISTS));
        if (!Integer.valueOf(0).equals(exception.getHandleStatus())) {
            throw new ServiceException(IntegrationErrorCodeConstants.DEVICE_EXCEPTION_STATUS_INVALID);
        }
        return exception;
    }

    /** 将超过最后一页的请求页码收敛到有效末页。 */
    private int normalizePageNo(int requestedPageNo, int pageSize, long total) {
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        return Math.min(requestedPageNo, totalPages);
    }

    /** 去除编码首尾空白并按固定 Locale 转大写，形成稳定查询和幂等键。 */
    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
