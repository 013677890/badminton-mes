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
 * <p>上报入口在单个事务内完成接入资格校验、幂等判定、增量计算、原始记录落库、
 * 异常记录落库及最近通信时间更新。配置行上的悲观锁把同一接入点的并发上报串行化，
 * 从而保护累计差分的读取基线，并配合数据库唯一约束形成完整的幂等防线。</p>
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceCountReportRespVO reportCount(DeviceCountReportReqVO request) {
        // 以配置行为并发边界加悲观锁：同一配置的上报依次计算差分，事务回滚时全部写入一并撤销。
        DeviceAccessConfigEntity config = configRepository
                .findByConfigCodeAndDeletedFalseForUpdate(request.getConfigCode())
                .orElseThrow(() -> new ServiceException(DeviceErrorCodeConstants.ACCESS_CONFIG_NOT_EXISTS));
        validateConfigCanCollect(config);
        validateCollectionTime(request.getCollectedAt());

        // 配置关联关系是可信基线，上报中的设备编码必须与台账快照一致，防止数据串入错误设备。
        EquipmentLedgerRespVO equipment = equipmentLedgerService.getEquipmentLedger(config.getEquipmentId());
        if (!request.getEquipmentCode().equals(equipment.getEquipmentCode())) {
            throw new ServiceException(DeviceErrorCodeConstants.COUNT_REPORT_EQUIPMENT_MISMATCH);
        }

        // 业务幂等键由稳定字段生成 SHA-256 摘要；先友好预检，再由数据库唯一约束兜住并发竞态。
        String deduplicationKey = buildDeduplicationKey(config.getId(), request);
        if (countRecordRepository.existsByDeduplicationKey(deduplicationKey)) {
            throw new ServiceException(DeviceErrorCodeConstants.COUNT_REPORT_DUPLICATE);
        }

        // 原始事实始终先落库；业务异常不会丢弃报文，而是将记录标记为异常并建立待处理工单。
        DeviceCountRecordEntity record = DeviceCountRecordConvert.toEntity(request, config, deduplicationKey);
        CountEvaluation evaluation = evaluateCount(config, equipment, request);
        record.setIncrementCount(evaluation.incrementCount());
        record.setMatchStatus(evaluation.exceptionType() == null
                ? MATCH_STATUS_PENDING : MATCH_STATUS_EXCEPTION);
        record.setReportStatus(REPORT_STATUS_NOT_CREATED);
        saveCountRecord(record);

        // 异常记录与计数记录同事务提交，保证不存在“有异常状态但无异常详情”的不一致窗口。
        if (evaluation.exceptionType() != null) {
            saveCountException(record, evaluation);
        }
        // 迟到报文仍可入库，但只能把最近通信时间向前推进，不能覆盖更新的在线证据。
        updateLastCommunicationTime(config, request.getCollectedAt());
        return DeviceCountRecordConvert.toReportRespVO(
                record, evaluation.exceptionType(), evaluation.processingMessage());
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceCountRecordRespVO getCountRecord(Long id) {
        // 缓存保存转换后的详情快照；未命中时才读取实体，避免把持久化对象直接暴露给调用方。
        return deviceCache.getOrLoadDetail(DeviceRedisKeyConstants.COUNT_RECORD_RESOURCE,
                id, DeviceCountRecordRespVO.class, () -> {
            DeviceCountRecordEntity record = countRecordRepository.findById(id)
                    .orElseThrow(() -> new ServiceException(DeviceErrorCodeConstants.COUNT_RECORD_NOT_EXISTS));
            DeviceCountRecordRespVO response = DeviceCountRecordConvert.toRespVO(record);
            return response;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DeviceCountRecordRespVO> getCountRecordPage(DeviceCountRecordPageReqVO request) {
        // 采集时间相同时再按主键倒序，保证跨页顺序稳定；越界页统一回退到实际末页。
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

    @Override
    @Transactional(readOnly = true)
    public DeviceCountExceptionRespVO getCountException(Long id) {
        // 异常详情采用缓存旁路读取，缓存内容包含处理状态及处理结果的当前快照。
        return deviceCache.getOrLoadDetail(DeviceRedisKeyConstants.COUNT_EXCEPTION_RESOURCE,
                id, DeviceCountExceptionRespVO.class, () -> {
            DeviceCountExceptionEntity exception = countExceptionRepository.findById(id)
                    .orElseThrow(() -> new ServiceException(DeviceErrorCodeConstants.COUNT_EXCEPTION_NOT_EXISTS));
            DeviceCountExceptionRespVO response = DeviceCountExceptionConvert.toRespVO(exception);
            return response;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DeviceCountExceptionRespVO> getCountExceptionPage(DeviceCountExceptionPageReqVO request) {
        // 创建时间与主键组成稳定倒序，便于处理人员优先审阅最新异常。
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processCountException(Long id, DeviceCountExceptionResolveReqVO request) {
        // 悲观锁将“仍待处理”的检查与处置写入绑定在同一事务内，防止两个处理人重复结案。
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
        // 只在提交成功后失效详情缓存，事务回滚时继续保留与数据库一致的旧快照。
        deviceCache.evictDetailAfterCommit(DeviceRedisKeyConstants.COUNT_EXCEPTION_RESOURCE, id);
    }

    /**
     * 按设备可用性、工序配置、累计差分、回退和跳变的固定优先级评估一次计数上报。
     *
     * <p>设备停用、状态异常、缺少工序和累计值回退返回零有效增量；跳变异常保留计算出的增量，
     * 供人工处置时判断是否接受。正常结果进入待匹配生产任务状态。
     *
     * @param config 已锁定且通过启用、联调校验的接入配置
     * @param equipment 当前设备台账快照
     * @param request 本次设备计数上报
     * @return 有效增量、异常类型和处理提示组成的评估结果
     */
    private CountEvaluation evaluateCount(DeviceAccessConfigEntity config,
                                          EquipmentLedgerRespVO equipment,
                                          DeviceCountReportReqVO request) {
        // 设备停用、运行状态异常或未配置工序均属于可追溯业务异常，而非拒绝保存原始计数。
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

        // 先得到业务有效增量，再区分累计表回退与超过阈值的突增；回退不计入有效产量。
        long incrementCount = calculateIncrement(config, request);
        if (incrementCount < 0) {
            return CountEvaluation.exception(0L, EXCEPTION_COUNT_ROLLBACK, "累计计数值小于此前记录，疑似设备计数回退");
        }
        // 跳变保留计算出的增量，供后续人工判断是否接受；异常类型负责阻断自动匹配流程。
        if (config.getSpikeThreshold() != null && incrementCount > config.getSpikeThreshold()) {
            return CountEvaluation.exception(incrementCount, EXCEPTION_COUNT_SPIKE,
                    "本次有效增量超过配置的异常跳变阈值");
        }
        return CountEvaluation.normal(incrementCount, WAITING_TASK_MESSAGE);
    }

    /**
     * 计算本次上报对应的业务增量。
     *
     * <p>非累计模式直接采用上报值。累计模式仅使用采集时间严格早于本次的最近记录作为基线，
     * 避免同一采集时刻的并发记录产生不确定顺序；没有历史基线时按设备从零开始累计处理。
     *
     * @param config 决定计数模式的接入配置
     * @param request 包含采集时间和原始计数值的上报请求
     * @return 本次业务增量；负数表示累计计数发生回退
     */
    private long calculateIncrement(DeviceAccessConfigEntity config, DeviceCountReportReqVO request) {
        // 非累计模式把本次上报值直接视为增量，不依赖任何历史基线。
        if (!COUNT_MODE_CUMULATIVE.equals(config.getCountMode())) {
            return request.getCountValue();
        }
        // 累计模式只查采集时间严格早于本次的最近记录，避免同时间点记录成为不确定差分基线。
        Optional<DeviceCountRecordEntity> previousRecord = countRecordRepository
                .findTopByAccessConfigIdAndCollectedAtLessThanOrderByCollectedAtDescIdDesc(
                        config.getId(), request.getCollectedAt());
        // 首条累计数据没有历史基线，按设备从零累计处理；后续增量等于当前累计值减前值。
        if (previousRecord.isEmpty()) {
            return request.getCountValue();
        }
        return request.getCountValue() - previousRecord.get().getRawCount();
    }

    private void validateConfigCanCollect(DeviceAccessConfigEntity config) {
        // 启用标志与联调通过缺一不可，避免仅修改启用状态便绕过设备接入验收。
        boolean canCollect = Integer.valueOf(ENABLED).equals(config.getEnabledStatus())
                && COMMISSIONING_PASSED.equals(config.getCommissioningStatus());
        if (!canCollect) {
            throw new ServiceException(DeviceErrorCodeConstants.COUNT_REPORT_CONFIG_UNAVAILABLE);
        }
    }

    private void validateCollectionTime(LocalDateTime collectedAt) {
        // 拒绝未来采集时间，避免未来记录污染累计差分顺序及最近通信时间。
        if (collectedAt.isAfter(LocalDateTime.now())) {
            throw new ServiceException(DeviceErrorCodeConstants.COUNT_COLLECTION_TIME_INVALID);
        }
    }

    private boolean isNormalEquipmentStatus(String equipmentStatus) {
        // 仅空闲和运行允许进入正常计数链路，其他台账状态均转为可处置异常。
        return NORMAL_EQUIPMENT_STATUS_IDLE.equals(equipmentStatus)
                || NORMAL_EQUIPMENT_STATUS_RUNNING.equals(equipmentStatus);
    }

    private String buildDeduplicationKey(Long configId, DeviceCountReportReqVO request) {
        // 配置、采集时间、设备流水号共同标识一次上报；分隔符避免字段拼接边界产生歧义。
        String source = configId + "|" + request.getCollectedAt() + "|" + request.getSerialNumber();
        try {
            // 使用 UTF-8 与 SHA-256 得到跨进程稳定的定长键，既便于唯一索引，也不保存冗长原始组合。
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 Java 运行环境不支持 SHA-256", exception);
        }
    }

    private void saveCountRecord(DeviceCountRecordEntity record) {
        try {
            // 立即 flush 触发幂等键唯一索引，把并发重复插入统一转换为业务重复上报错误。
            countRecordRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException exception) {
            throw new ServiceException(DeviceErrorCodeConstants.COUNT_REPORT_DUPLICATE);
        }
    }

    private void saveCountException(DeviceCountRecordEntity record, CountEvaluation evaluation) {
        // 异常表引用已落库计数主键，并复制配置、设备和原因快照，支持脱离原始报文独立处置。
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
        // 单调更新规则仅接受更晚时间；乱序或补传数据不会让设备最近通信时间倒退。
        LocalDateTime lastCommunicationTime = config.getLastCommunicationTime();
        if (lastCommunicationTime == null || collectedAt.isAfter(lastCommunicationTime)) {
            config.setLastCommunicationTime(collectedAt);
            configRepository.save(config);
            // 只有数据库值实际前移时才在提交后失效配置缓存，减少无意义缓存抖动。
            deviceCache.evictDetailAfterCommit(DeviceRedisKeyConstants.ACCESS_CONFIG_RESOURCE, config.getId());
        }
    }

    private Long getCurrentOperatorId() {
        // 受控内部调用没有登录上下文时使用约定账号，确保异常处置审计字段始终完整。
        if (SecurityContextHolder.getLoginUser() == null) {
            return DEFAULT_OPERATOR_ID;
        }
        return SecurityContextHolder.getRequiredLoginUserId();
    }

    /** 将增量判定结果作为不可变值在计算、落库与响应转换之间传递。 */
    private record CountEvaluation(Long incrementCount,
                                   String exceptionType,
                                   String processingMessage) {

        /** 正常结果不携带异常类型，后续进入待匹配状态。 */
        private static CountEvaluation normal(Long incrementCount, String processingMessage) {
            return new CountEvaluation(incrementCount, null, processingMessage);
        }

        /** 异常结果同时携带可落库的类型和面向处理人的原因说明。 */
        private static CountEvaluation exception(Long incrementCount,
                                                 String exceptionType,
                                                 String processingMessage) {
            return new CountEvaluation(incrementCount, exceptionType, processingMessage);
        }
    }
}
