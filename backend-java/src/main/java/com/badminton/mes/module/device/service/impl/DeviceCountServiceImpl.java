package com.badminton.mes.module.device.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.device.constants.DeviceErrorCodeConstants;
import com.badminton.mes.module.device.controller.vo.DeviceCountExceptionPageReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountExceptionResolveReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountExceptionRespVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountRecordPageReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountRecordRespVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountReportReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountReportRespVO;
import com.badminton.mes.module.device.convert.DeviceCountExceptionConvert;
import com.badminton.mes.module.device.convert.DeviceCountRecordConvert;
import com.badminton.mes.module.device.dal.entity.DeviceAccessConfigEntity;
import com.badminton.mes.module.device.dal.entity.DeviceCountExceptionEntity;
import com.badminton.mes.module.device.dal.entity.DeviceCountRecordEntity;
import com.badminton.mes.module.device.dal.redis.DeviceCache;
import com.badminton.mes.module.device.dal.redis.DeviceRedisKeyConstants;
import com.badminton.mes.module.device.dal.repository.DeviceAccessConfigRepository;
import com.badminton.mes.module.device.dal.repository.DeviceCountExceptionRepository;
import com.badminton.mes.module.device.dal.repository.DeviceCountExceptionSpecifications;
import com.badminton.mes.module.device.dal.repository.DeviceCountRecordRepository;
import com.badminton.mes.module.device.dal.repository.DeviceCountRecordSpecifications;
import com.badminton.mes.module.device.service.DeviceCountService;
import com.badminton.mes.module.equipment.controller.vo.EquipmentLedgerRespVO;
import com.badminton.mes.module.equipment.service.EquipmentLedgerService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 设备计数接入及异常处理 Service 实现。
 *
 * <p>设备上报调用 {@link #reportCount(DeviceCountReportReqVO)}；后台页面调用查询和异常
 * 处理方法。实现类用摘要键和数据库唯一约束实现幂等，并把设备停用、计数回退、突增等
 * 情况落成异常记录，避免异常数据直接进入生产报工。
 *
 * @author MES 开发组
 * @date 2026/07/16
 */
@Service
public class DeviceCountServiceImpl implements DeviceCountService {

    private static final int ENABLED = 1;
    private static final String COMMISSIONING_PASSED = "PASSED";
    private static final String COUNT_MODE_CUMULATIVE = "CUMULATIVE";
    private static final String MATCH_STATUS_PENDING = "PENDING";
    private static final String MATCH_STATUS_EXCEPTION = "EXCEPTION";
    private static final String REPORT_STATUS_NOT_CREATED = "NOT_CREATED";
    private static final String PROCESSING_STATUS_PENDING = "PENDING";
    private static final String EXCEPTION_EQUIPMENT_DISABLED = "EQUIPMENT_DISABLED";
    private static final String EXCEPTION_EQUIPMENT_STATUS = "EQUIPMENT_STATUS_ABNORMAL";
    private static final String EXCEPTION_PROCESS_MISSING = "PROCESS_NOT_CONFIGURED";
    private static final String EXCEPTION_COUNT_ROLLBACK = "COUNT_ROLLBACK";
    private static final String EXCEPTION_COUNT_SPIKE = "COUNT_SPIKE";
    private static final String WAITING_TASK_MESSAGE = "计数数据已接收，等待生产任务匹配";
    private static final String NORMAL_EQUIPMENT_STATUS_IDLE = "IDLE";
    private static final String NORMAL_EQUIPMENT_STATUS_RUNNING = "RUNNING";
    private static final Long DEFAULT_OPERATOR_ID = 1L;

    private final DeviceAccessConfigRepository configRepository;
    private final DeviceCountRecordRepository countRecordRepository;
    private final DeviceCountExceptionRepository countExceptionRepository;
    private final EquipmentLedgerService equipmentLedgerService;
    private final DeviceCache deviceCache;

    public DeviceCountServiceImpl(DeviceAccessConfigRepository configRepository,
                                  DeviceCountRecordRepository countRecordRepository,
                                  DeviceCountExceptionRepository countExceptionRepository,
                                  EquipmentLedgerService equipmentLedgerService,
                                  DeviceCache deviceCache) {
        this.configRepository = configRepository;
        this.countRecordRepository = countRecordRepository;
        this.countExceptionRepository = countExceptionRepository;
        this.equipmentLedgerService = equipmentLedgerService;
        this.deviceCache = deviceCache;
    }

    /** 校验接入配置和设备状态，计算增量，幂等保存计数记录并按需生成异常。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceCountReportRespVO reportCount(DeviceCountReportReqVO request) {
        DeviceAccessConfigEntity config = configRepository
                .findByConfigCodeAndDeletedFalseForUpdate(request.getConfigCode())
                .orElseThrow(() -> new ServiceException(DeviceErrorCodeConstants.ACCESS_CONFIG_NOT_EXISTS));
        validateConfigCanCollect(config);
        validateCollectionTime(request.getCollectedAt());

        EquipmentLedgerRespVO equipment = equipmentLedgerService.getEquipmentLedger(config.getEquipmentId());
        if (!request.getEquipmentCode().equals(equipment.getEquipmentCode())) {
            throw new ServiceException(DeviceErrorCodeConstants.COUNT_REPORT_EQUIPMENT_MISMATCH);
        }

        String deduplicationKey = buildDeduplicationKey(config.getId(), request);
        if (countRecordRepository.existsByDeduplicationKey(deduplicationKey)) {
            throw new ServiceException(DeviceErrorCodeConstants.COUNT_REPORT_DUPLICATE);
        }

        DeviceCountRecordEntity record = DeviceCountRecordConvert.toEntity(request, config, deduplicationKey);
        CountEvaluation evaluation = evaluateCount(config, equipment, request);
        record.setIncrementCount(evaluation.incrementCount());
        record.setMatchStatus(evaluation.exceptionType() == null
                ? MATCH_STATUS_PENDING : MATCH_STATUS_EXCEPTION);
        record.setReportStatus(REPORT_STATUS_NOT_CREATED);
        saveCountRecord(record);

        if (evaluation.exceptionType() != null) {
            saveCountException(record, evaluation);
        }
        updateLastCommunicationTime(config, request.getCollectedAt());
        return DeviceCountRecordConvert.toReportRespVO(
                record, evaluation.exceptionType(), evaluation.processingMessage());
    }

    /** 查询单条计数记录，使用 Cache Aside 降低重复详情查询成本。 */
    @Override
    @Transactional(readOnly = true)
    public DeviceCountRecordRespVO getCountRecord(Long id) {
        return deviceCache.getOrLoadDetail(DeviceRedisKeyConstants.COUNT_RECORD_RESOURCE,
                id, DeviceCountRecordRespVO.class, () -> {
            DeviceCountRecordEntity record = countRecordRepository.findById(id)
                    .orElseThrow(() -> new ServiceException(DeviceErrorCodeConstants.COUNT_RECORD_NOT_EXISTS));
            DeviceCountRecordRespVO response = DeviceCountRecordConvert.toRespVO(record);
            return response;
        });
    }

    /** 分页查询计数记录。 */
    @Override
    @Transactional(readOnly = true)
    public PageResult<DeviceCountRecordRespVO> getCountRecordPage(DeviceCountRecordPageReqVO request) {
        var specification = DeviceCountRecordSpecifications.page(request);
        long total = countRecordRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(request.getPageNo(), request.getPageSize());
        }
        int pageSize = request.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        int pageNo = Math.min(request.getPageNo(), totalPages);
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize,
                Sort.by(Sort.Direction.DESC, "collectedAt").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<DeviceCountRecordEntity> page = countRecordRepository.findAll(specification, pageRequest);
        List<DeviceCountRecordRespVO> list = DeviceCountRecordConvert.toRespVOList(page.getContent());
        return PageResult.of(list, total, pageNo, pageSize);
    }

    /** 查询单条计数异常，优先读取详情缓存。 */
    @Override
    @Transactional(readOnly = true)
    public DeviceCountExceptionRespVO getCountException(Long id) {
        return deviceCache.getOrLoadDetail(DeviceRedisKeyConstants.COUNT_EXCEPTION_RESOURCE,
                id, DeviceCountExceptionRespVO.class, () -> {
            DeviceCountExceptionEntity exception = countExceptionRepository.findById(id)
                    .orElseThrow(() -> new ServiceException(DeviceErrorCodeConstants.COUNT_EXCEPTION_NOT_EXISTS));
            DeviceCountExceptionRespVO response = DeviceCountExceptionConvert.toRespVO(exception);
            return response;
        });
    }

    /** 分页查询计数异常。 */
    @Override
    @Transactional(readOnly = true)
    public PageResult<DeviceCountExceptionRespVO> getCountExceptionPage(DeviceCountExceptionPageReqVO request) {
        var specification = DeviceCountExceptionSpecifications.page(request);
        long total = countExceptionRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(request.getPageNo(), request.getPageSize());
        }
        int pageSize = request.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        int pageNo = Math.min(request.getPageNo(), totalPages);
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize,
                Sort.by(Sort.Direction.DESC, "createTime").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<DeviceCountExceptionEntity> page = countExceptionRepository.findAll(specification, pageRequest);
        List<DeviceCountExceptionRespVO> list = DeviceCountExceptionConvert.toRespVOList(page.getContent());
        return PageResult.of(list, total, pageNo, pageSize);
    }

    /** 锁定异常记录后写入处理结果，防止多个处理人并发重复处理。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processCountException(Long id, DeviceCountExceptionResolveReqVO request) {
        DeviceCountExceptionEntity exception = countExceptionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ServiceException(DeviceErrorCodeConstants.COUNT_EXCEPTION_NOT_EXISTS));
        if (!PROCESSING_STATUS_PENDING.equals(exception.getProcessingStatus())) {
            throw new ServiceException(DeviceErrorCodeConstants.COUNT_EXCEPTION_ALREADY_PROCESSED);
        }
        exception.setProcessingStatus(request.getProcessingStatus());
        exception.setProcessingResult(request.getProcessingResult());
        exception.setProcessedBy(getCurrentOperatorId());
        exception.setProcessedAt(LocalDateTime.now());
        countExceptionRepository.save(exception);
        deviceCache.evictDetailAfterCommit(DeviceRedisKeyConstants.COUNT_EXCEPTION_RESOURCE, id);
    }

    /**
     * 按配置和设备现状评估本次计数：返回可入账增量，或返回应记录的异常类型与说明。
     */
    private CountEvaluation evaluateCount(DeviceAccessConfigEntity config,
                                          EquipmentLedgerRespVO equipment,
                                          DeviceCountReportReqVO request) {
        if (!Integer.valueOf(ENABLED).equals(equipment.getStatus())) {
            return CountEvaluation.exception(0L, EXCEPTION_EQUIPMENT_DISABLED, "设备台账已停用，计数转入异常处理");
        }
        if (!isNormalEquipmentStatus(equipment.getEquipmentStatus())) {
            return CountEvaluation.exception(0L, EXCEPTION_EQUIPMENT_STATUS,
                    "设备状态为 " + equipment.getEquipmentStatus() + "，计数转入异常处理");
        }
        if (config.getProcessId() == null) {
            return CountEvaluation.exception(0L, EXCEPTION_PROCESS_MISSING, "接入配置未关联工序，计数转入异常处理");
        }

        long incrementCount = calculateIncrement(config, request);
        if (incrementCount < 0) {
            return CountEvaluation.exception(0L, EXCEPTION_COUNT_ROLLBACK, "累计计数值小于此前记录，疑似设备计数回退");
        }
        if (config.getSpikeThreshold() != null && incrementCount > config.getSpikeThreshold()) {
            return CountEvaluation.exception(incrementCount, EXCEPTION_COUNT_SPIKE,
                    "本次有效增量超过配置的异常跳变阈值");
        }
        return CountEvaluation.normal(incrementCount, WAITING_TASK_MESSAGE);
    }

    private long calculateIncrement(DeviceAccessConfigEntity config, DeviceCountReportReqVO request) {
        if (!COUNT_MODE_CUMULATIVE.equals(config.getCountMode())) {
            return request.getCountValue();
        }
        Optional<DeviceCountRecordEntity> previousRecord = countRecordRepository
                .findTopByAccessConfigIdAndCollectedAtLessThanOrderByCollectedAtDescIdDesc(
                        config.getId(), request.getCollectedAt());
        if (previousRecord.isEmpty()) {
            return request.getCountValue();
        }
        return request.getCountValue() - previousRecord.get().getRawCount();
    }

    private void validateConfigCanCollect(DeviceAccessConfigEntity config) {
        boolean canCollect = Integer.valueOf(ENABLED).equals(config.getEnabledStatus())
                && COMMISSIONING_PASSED.equals(config.getCommissioningStatus());
        if (!canCollect) {
            throw new ServiceException(DeviceErrorCodeConstants.COUNT_REPORT_CONFIG_UNAVAILABLE);
        }
    }

    private void validateCollectionTime(LocalDateTime collectedAt) {
        if (collectedAt.isAfter(LocalDateTime.now())) {
            throw new ServiceException(DeviceErrorCodeConstants.COUNT_COLLECTION_TIME_INVALID);
        }
    }

    private boolean isNormalEquipmentStatus(String equipmentStatus) {
        return NORMAL_EQUIPMENT_STATUS_IDLE.equals(equipmentStatus)
                || NORMAL_EQUIPMENT_STATUS_RUNNING.equals(equipmentStatus);
    }

    /**
     * 使用配置、采集时间和设备序列号生成稳定摘要，供数据库唯一索引拦截重复上报。
     */
    private String buildDeduplicationKey(Long configId, DeviceCountReportReqVO request) {
        String source = configId + "|" + request.getCollectedAt() + "|" + request.getSerialNumber();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 Java 运行环境不支持 SHA-256", exception);
        }
    }

    private void saveCountRecord(DeviceCountRecordEntity record) {
        try {
            countRecordRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException exception) {
            throw new ServiceException(DeviceErrorCodeConstants.COUNT_REPORT_DUPLICATE);
        }
    }

    private void saveCountException(DeviceCountRecordEntity record, CountEvaluation evaluation) {
        DeviceCountExceptionEntity exception = new DeviceCountExceptionEntity();
        exception.setCountRecordId(record.getId());
        exception.setAccessConfigId(record.getAccessConfigId());
        exception.setEquipmentId(record.getEquipmentId());
        exception.setExceptionType(evaluation.exceptionType());
        exception.setExceptionReason(evaluation.processingMessage());
        exception.setProcessingStatus(PROCESSING_STATUS_PENDING);
        countExceptionRepository.saveAndFlush(exception);
    }

    private void updateLastCommunicationTime(DeviceAccessConfigEntity config, LocalDateTime collectedAt) {
        LocalDateTime lastCommunicationTime = config.getLastCommunicationTime();
        if (lastCommunicationTime == null || collectedAt.isAfter(lastCommunicationTime)) {
            config.setLastCommunicationTime(collectedAt);
            configRepository.save(config);
            deviceCache.evictDetailAfterCommit(DeviceRedisKeyConstants.ACCESS_CONFIG_RESOURCE, config.getId());
        }
    }

    private Long getCurrentOperatorId() {
        if (SecurityContextHolder.getLoginUser() == null) {
            return DEFAULT_OPERATOR_ID;
        }
        return SecurityContextHolder.getRequiredLoginUserId();
    }

    private record CountEvaluation(Long incrementCount,
                                   String exceptionType,
                                   String processingMessage) {

        private static CountEvaluation normal(Long incrementCount, String processingMessage) {
            return new CountEvaluation(incrementCount, null, processingMessage);
        }

        private static CountEvaluation exception(Long incrementCount,
                                                 String exceptionType,
                                                 String processingMessage) {
            return new CountEvaluation(incrementCount, exceptionType, processingMessage);
        }
    }
}
