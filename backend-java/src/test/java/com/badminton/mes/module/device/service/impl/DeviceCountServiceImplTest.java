package com.badminton.mes.module.device.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.device.constants.DeviceErrorCodeConstants;
import com.badminton.mes.module.device.controller.vo.DeviceCountExceptionResolveReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountReportReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountReportRespVO;
import com.badminton.mes.module.device.dal.entity.DeviceAccessConfigEntity;
import com.badminton.mes.module.device.dal.entity.DeviceCountExceptionEntity;
import com.badminton.mes.module.device.dal.entity.DeviceCountRecordEntity;
import com.badminton.mes.module.device.dal.redis.DeviceCache;
import com.badminton.mes.module.device.dal.redis.DeviceRedisKeyConstants;
import com.badminton.mes.module.device.dal.repository.DeviceAccessConfigRepository;
import com.badminton.mes.module.device.dal.repository.DeviceCountExceptionRepository;
import com.badminton.mes.module.device.dal.repository.DeviceCountRecordRepository;
import com.badminton.mes.module.equipment.controller.vo.EquipmentLedgerRespVO;
import com.badminton.mes.module.equipment.service.EquipmentLedgerService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DeviceCountServiceImpl} 的计数上报与计数异常处理单元测试。
 *
 * <p>通过 Mock 隔离访问配置、计数记录、异常记录、设备台账和 Redis 缓存等外部协作者，
 * 仅验证服务层的累计值换算、幂等保护、异常映射、通信时间维护以及处理结果落库逻辑。
 * 覆盖重点包括正常与回退分支、数据库唯一约束兜底、状态不可逆约束，以及服务是否请求缓存组件
 * 安排事务提交后的详情失效；真实 {@code afterCommit} 回调语义由缓存组件测试负责。
 */
@ExtendWith(MockitoExtension.class)
class DeviceCountServiceImplTest {

    // 测试夹具中的访问配置主键，用于串联配置查询、计数记录和缓存键断言。
    private static final Long CONFIG_ID = 100L;
    // 与访问配置绑定的设备台账主键，用于模拟跨模块设备可用性校验。
    private static final Long EQUIPMENT_ID = 200L;
    // 采集点所属工序主键，用于确认完整配置可参与计数上报。
    private static final Long PROCESS_ID = 300L;
    // 模拟计数记录持久化后回填的主键，用于校验响应和异常记录关联关系。
    private static final Long COUNT_RECORD_ID = 400L;
    // 待处理计数异常的固定主键，用于校验加锁查询、状态更新与缓存失效。
    private static final Long COUNT_EXCEPTION_ID = 500L;
    // 对外暴露的访问配置业务编码，是计数上报定位配置的入口。
    private static final String CONFIG_CODE = "CFG-001";
    // 请求携带的设备业务编码，用于匹配设备台账返回结果。
    private static final String EQUIPMENT_CODE = "EQ-001";

    /** 隔离访问配置的加锁查询与通信时间更新持久化。 */
    @Mock
    private DeviceAccessConfigRepository configRepository;

    /** 隔离历史计数查询、幂等检查以及新计数记录写入。 */
    @Mock
    private DeviceCountRecordRepository countRecordRepository;

    /** 隔离计数异常的创建、加锁读取和处理结果持久化。 */
    @Mock
    private DeviceCountExceptionRepository countExceptionRepository;

    /** 隔离设备模块之外的台账查询，避免依赖真实设备状态数据。 */
    @Mock
    private EquipmentLedgerService equipmentLedgerService;

    /** 隔离 Redis 操作，并用于观察事务提交后的详情缓存失效请求。 */
    @Mock
    private DeviceCache deviceCache;

    /** 使用上述 Mock 直接构造的被测服务，不加载 Spring 容器。 */
    private DeviceCountServiceImpl countService;

    @BeforeEach
    void setUp() {
        // 每个用例重新创建服务实例，避免被测对象在测试之间共享状态。
        countService = new DeviceCountServiceImpl(
                configRepository,
                countRecordRepository,
                countExceptionRepository,
                equipmentLedgerService,
                deviceCache);
    }

    @Test
    @DisplayName("累计计数上报：使用前序记录计算增量并更新通信时间")
    void reportCumulativeCountCalculatesIncrementAndUpdatesCommunicationTime() {
        LocalDateTime collectedAt = LocalDateTime.now().minusMinutes(1);
        DeviceAccessConfigEntity config = buildCollectableConfig();
        DeviceCountRecordEntity previousRecord = new DeviceCountRecordEntity();
        previousRecord.setRawCount(100L);
        DeviceCountReportReqVO request = buildReportRequest(collectedAt, 135L);
        stubReportPrerequisites(config);
        // 仅返回采集时刻之前的最近记录，使累计模式按 135 - 100 计算本次增量。
        when(countRecordRepository.findTopByAccessConfigIdAndCollectedAtLessThanOrderByCollectedAtDescIdDesc(
                CONFIG_ID, collectedAt)).thenReturn(Optional.of(previousRecord));
        stubSavedRecordId();

        DeviceCountReportRespVO response = countService.reportCount(request);

        assertThat(response.getCountRecordId()).isEqualTo(COUNT_RECORD_ID);
        assertThat(response.getIncrementCount()).isEqualTo(35L);
        assertThat(response.getMatchStatus()).isEqualTo("PENDING");
        assertThat(response.getExceptionType()).isNull();
        // 捕获真正写入的记录，验证服务内部派生字段，而非只检查对外响应。
        ArgumentCaptor<DeviceCountRecordEntity> recordCaptor =
                ArgumentCaptor.forClass(DeviceCountRecordEntity.class);
        verify(countRecordRepository).saveAndFlush(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getRawCount()).isEqualTo(135L);
        assertThat(recordCaptor.getValue().getIncrementCount()).isEqualTo(35L);
        assertThat(recordCaptor.getValue().getReportStatus()).isEqualTo("NOT_CREATED");
        assertThat(recordCaptor.getValue().getDeduplicationKey()).hasSize(64);
        // 新采集时间应推进配置状态，并在持久化后触发对应详情缓存失效。
        assertThat(config.getLastCommunicationTime()).isEqualTo(collectedAt);
        verify(configRepository).save(config);
        verify(deviceCache).evictDetailAfterCommit(
                DeviceRedisKeyConstants.ACCESS_CONFIG_RESOURCE,
                CONFIG_ID);
        // 正常递增不得误入计数异常分支。
        verify(countExceptionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("计数上报：幂等键已存在时拒绝重复记录")
    void reportCountRejectsDuplicateDeduplicationKey() {
        DeviceAccessConfigEntity config = buildCollectableConfig();
        DeviceCountReportReqVO request = buildReportRequest(LocalDateTime.now().minusMinutes(1), 20L);
        stubReportPrerequisites(config);
        // 在落库前命中既有幂等键，模拟同一采集报文已被受理的快速拒绝路径。
        when(countRecordRepository.existsByDeduplicationKey(anyString())).thenReturn(true);

        assertThatThrownBy(() -> countService.reportCount(request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                DeviceErrorCodeConstants.COUNT_REPORT_DUPLICATE));
        // 幂等校验失败后不应产生记录，也不应推进配置通信时间。
        verify(countRecordRepository, never()).saveAndFlush(any());
        verify(configRepository, never()).save(any());
    }

    @Test
    @DisplayName("累计计数上报：计数回退时保存异常记录")
    void reportCumulativeCountCreatesRollbackException() {
        LocalDateTime collectedAt = LocalDateTime.now().minusMinutes(1);
        DeviceAccessConfigEntity config = buildCollectableConfig();
        DeviceCountRecordEntity previousRecord = new DeviceCountRecordEntity();
        previousRecord.setRawCount(150L);
        DeviceCountReportReqVO request = buildReportRequest(collectedAt, 120L);
        stubReportPrerequisites(config);
        // 构造当前累计值小于历史累计值的前序数据，定向触发设备计数回退识别。
        when(countRecordRepository.findTopByAccessConfigIdAndCollectedAtLessThanOrderByCollectedAtDescIdDesc(
                CONFIG_ID, collectedAt)).thenReturn(Optional.of(previousRecord));
        stubSavedRecordId();

        DeviceCountReportRespVO response = countService.reportCount(request);

        assertThat(response.getIncrementCount()).isZero();
        assertThat(response.getMatchStatus()).isEqualTo("EXCEPTION");
        assertThat(response.getExceptionType()).isEqualTo("COUNT_ROLLBACK");
        // 捕获异常实体，确认它关联已保存的计数记录，并以待处理状态进入处置流程。
        ArgumentCaptor<DeviceCountExceptionEntity> exceptionCaptor =
                ArgumentCaptor.forClass(DeviceCountExceptionEntity.class);
        verify(countExceptionRepository).saveAndFlush(exceptionCaptor.capture());
        DeviceCountExceptionEntity savedException = exceptionCaptor.getValue();
        assertThat(savedException.getCountRecordId()).isEqualTo(COUNT_RECORD_ID);
        assertThat(savedException.getExceptionType()).isEqualTo("COUNT_ROLLBACK");
        assertThat(savedException.getProcessingStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("计数上报：数据库幂等约束冲突转换为重复上报异常")
    void reportCountTranslatesDatabaseDeduplicationConflict() {
        DeviceAccessConfigEntity config = buildCollectableConfig();
        DeviceCountReportReqVO request = buildReportRequest(LocalDateTime.now().minusMinutes(1), 20L);
        config.setCountMode("INCREMENTAL");
        stubReportPrerequisites(config);
        // 模拟并发请求绕过预检查后在数据库唯一约束处冲突，验证最终一致的幂等兜底。
        when(countRecordRepository.saveAndFlush(any(DeviceCountRecordEntity.class)))
                .thenThrow(new DataIntegrityViolationException("uk_device_count_deduplication"));

        assertThatThrownBy(() -> countService.reportCount(request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                DeviceErrorCodeConstants.COUNT_REPORT_DUPLICATE));
        // 主记录未成功落库时不得创建孤立异常，也不得更新配置状态。
        verify(countExceptionRepository, never()).saveAndFlush(any());
        verify(configRepository, never()).save(any());
    }

    @Test
    @DisplayName("计数上报：较早采集时间不覆盖最新通信时间")
    void reportCountDoesNotMoveLastCommunicationTimeBackward() {
        LocalDateTime collectedAt = LocalDateTime.now().minusMinutes(10);
        DeviceAccessConfigEntity config = buildCollectableConfig();
        LocalDateTime latestCommunicationTime = collectedAt.plusMinutes(5);
        config.setLastCommunicationTime(latestCommunicationTime);
        config.setCountMode("INCREMENTAL");
        stubReportPrerequisites(config);
        stubSavedRecordId();

        countService.reportCount(buildReportRequest(collectedAt, 20L));

        // 乱序到达的旧报文只能形成计数记录，不能让设备在线时间倒退或污染缓存。
        assertThat(config.getLastCommunicationTime()).isEqualTo(latestCommunicationTime);
        verify(configRepository, never()).save(any());
        verify(deviceCache, never()).evictDetailAfterCommit(
                DeviceRedisKeyConstants.ACCESS_CONFIG_RESOURCE,
                CONFIG_ID);
    }

    @Test
    @DisplayName("处理计数异常：保存处理结果并失效异常详情缓存")
    void processCountExceptionSavesResultAndEvictsCache() {
        DeviceCountExceptionEntity countException = new DeviceCountExceptionEntity();
        countException.setId(COUNT_EXCEPTION_ID);
        countException.setProcessingStatus("PENDING");
        DeviceCountExceptionResolveReqVO request = new DeviceCountExceptionResolveReqVO();
        request.setProcessingStatus("RESOLVED");
        request.setProcessingResult("已核实并修正设备累计值");
        // 返回受锁保护的待处理实体，使测试聚焦一次性状态迁移及其副作用。
        when(countExceptionRepository.findByIdForUpdate(COUNT_EXCEPTION_ID))
                .thenReturn(Optional.of(countException));

        countService.processCountException(COUNT_EXCEPTION_ID, request);

        // 直接检查同一实体的状态变化，覆盖处理人和处理时间等服务端补全字段。
        assertThat(countException.getProcessingStatus()).isEqualTo("RESOLVED");
        assertThat(countException.getProcessingResult()).isEqualTo("已核实并修正设备累计值");
        assertThat(countException.getProcessedBy()).isEqualTo(1L);
        assertThat(countException.getProcessedAt()).isNotNull();
        // 成功保存处理结果后应失效异常详情缓存，防止继续读取旧的待处理状态。
        verify(countExceptionRepository).save(countException);
        verify(deviceCache).evictDetailAfterCommit(
                DeviceRedisKeyConstants.COUNT_EXCEPTION_RESOURCE,
                COUNT_EXCEPTION_ID);
    }

    @Test
    @DisplayName("处理计数异常：已处理异常不允许重复处理")
    void processCountExceptionRejectsAlreadyProcessedException() {
        DeviceCountExceptionEntity countException = new DeviceCountExceptionEntity();
        countException.setId(COUNT_EXCEPTION_ID);
        countException.setProcessingStatus("RESOLVED");
        // 加锁读取后仍为终态，模拟并发或重复提交下的服务端二次防护。
        when(countExceptionRepository.findByIdForUpdate(COUNT_EXCEPTION_ID))
                .thenReturn(Optional.of(countException));

        assertThatThrownBy(() -> countService.processCountException(
                COUNT_EXCEPTION_ID, new DeviceCountExceptionResolveReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                DeviceErrorCodeConstants.COUNT_EXCEPTION_ALREADY_PROCESSED));
        // 拒绝重复处理时必须保持数据库和缓存均无副作用。
        verify(countExceptionRepository, never()).save(any());
        verify(deviceCache, never()).evictDetailAfterCommit(anyString(), any());
    }

    /**
     * 统一桩定计数上报的前置依赖：配置存在且设备台账处于可采集状态。
     *
     * @param config 当前用例需要返回的访问配置实体
     */
    private void stubReportPrerequisites(DeviceAccessConfigEntity config) {
        when(configRepository.findByConfigCodeAndDeletedFalseForUpdate(CONFIG_CODE))
                .thenReturn(Optional.of(config));
        when(equipmentLedgerService.getEquipmentLedger(EQUIPMENT_ID))
                .thenReturn(buildAvailableEquipment());
    }

    /** 模拟 ORM 保存后回填主键，保留传入实体的其他派生字段以供后续捕获或关联。 */
    private void stubSavedRecordId() {
        when(countRecordRepository.saveAndFlush(any(DeviceCountRecordEntity.class)))
                .thenAnswer(invocation -> {
                    DeviceCountRecordEntity record = invocation.getArgument(0);
                    record.setId(COUNT_RECORD_ID);
                    return record;
                });
    }

    /** 构造通过启用、调试和删除状态校验的累计计数访问配置。 */
    private DeviceAccessConfigEntity buildCollectableConfig() {
        DeviceAccessConfigEntity config = new DeviceAccessConfigEntity();
        config.setId(CONFIG_ID);
        config.setConfigCode(CONFIG_CODE);
        config.setEquipmentId(EQUIPMENT_ID);
        config.setCollectionPointCode("COUNT-001");
        config.setProcessId(PROCESS_ID);
        config.setCountMode("CUMULATIVE");
        config.setSpikeThreshold(1000L);
        config.setCommissioningStatus("PASSED");
        config.setEnabledStatus(1);
        config.setDeleted(false);
        return config;
    }

    /** 构造业务启用且运行状态允许采集的设备台账视图。 */
    private EquipmentLedgerRespVO buildAvailableEquipment() {
        EquipmentLedgerRespVO equipment = new EquipmentLedgerRespVO();
        equipment.setId(EQUIPMENT_ID);
        equipment.setEquipmentCode(EQUIPMENT_CODE);
        equipment.setEquipmentStatus("IDLE");
        equipment.setStatus(1);
        return equipment;
    }

    /**
     * 构造字段完整的计数上报请求，仅将采集时间和计数值暴露给各分支用例定制。
     *
     * @param collectedAt 设备侧采集时间
     * @param countValue 设备上报的原始计数值
     * @return 可直接提交给被测服务的请求对象
     */
    private DeviceCountReportReqVO buildReportRequest(LocalDateTime collectedAt, Long countValue) {
        DeviceCountReportReqVO request = new DeviceCountReportReqVO();
        request.setConfigCode(CONFIG_CODE);
        request.setEquipmentCode(EQUIPMENT_CODE);
        request.setCollectedAt(collectedAt);
        request.setSerialNumber("SERIAL-001");
        request.setCountValue(countValue);
        request.setRuntimeStatus("RUNNING");
        return request;
    }
}
