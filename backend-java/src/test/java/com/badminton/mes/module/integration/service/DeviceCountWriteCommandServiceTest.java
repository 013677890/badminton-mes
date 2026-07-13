package com.badminton.mes.module.integration.service;

import java.time.LocalDateTime;
import java.util.Optional;

import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.craft.dal.entity.CraftProcessEntity;
import com.badminton.mes.module.craft.dal.repository.CraftProcessRepository;
import com.badminton.mes.module.integration.constants.IntegrationErrorCodeConstants;
import com.badminton.mes.module.integration.controller.vo.DeviceCountExceptionPageReqVO;
import com.badminton.mes.module.integration.controller.vo.DeviceCountWriteReqVO;
import com.badminton.mes.module.integration.controller.vo.IntegrationWriteResultRespVO;
import com.badminton.mes.module.integration.dal.entity.DeviceCountExceptionEntity;
import com.badminton.mes.module.integration.dal.entity.DeviceCountRecordEntity;
import com.badminton.mes.module.integration.dal.entity.IntegrationWriteLogEntity;
import com.badminton.mes.module.integration.dal.repository.DeviceCountExceptionRepository;
import com.badminton.mes.module.integration.dal.repository.DeviceCountRecordRepository;
import com.badminton.mes.module.integration.dal.repository.IntegrationWriteLogRepository;
import com.badminton.mes.module.integration.enums.DeviceCountExceptionTypeEnum;
import com.badminton.mes.module.integration.enums.IntegrationInterfaceTypeEnum;
import com.badminton.mes.module.integration.enums.IntegrationWriteStatusEnum;
import com.badminton.mes.module.production.dal.entity.DispatchOrderEntity;
import com.badminton.mes.module.production.dal.repository.DispatchOrderRepository;
import com.badminton.mes.module.production.enums.DispatchStatusEnum;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DeviceCountWriteCommandService} 单元测试。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@ExtendWith(MockitoExtension.class)
class DeviceCountWriteCommandServiceTest {

    private static final Long OPERATOR_ID = 9L;

    @Mock
    private DeviceCountRecordRepository recordRepository;

    @Mock
    private DeviceCountExceptionRepository exceptionRepository;

    @Mock
    private IntegrationWriteLogRepository writeLogRepository;

    @Mock
    private DispatchOrderRepository dispatchOrderRepository;

    @Mock
    private CraftProcessRepository craftProcessRepository;

    @Mock
    private IntegrationAuditService auditService;

    private DeviceCountWriteCommandService commandService;

    @BeforeEach
    void setUp() {
        commandService = new DeviceCountWriteCommandService(
                recordRepository, exceptionRepository, writeLogRepository,
                dispatchOrderRepository, craftProcessRepository, auditService);
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(OPERATOR_ID);
        SecurityContextHolder.set("device-count-test", loginUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    @DisplayName("设备计数：已下发派工单和已存在工序计算增量并写成功日志")
    void writeDeviceCountCalculatesIncrementAndWritesSuccess() {
        DeviceCountWriteReqVO reqVO = buildReqVO(130L);
        when(writeLogRepository
                .findFirstByInterfaceTypeAndSourceSystemAndBusinessKeyOrderByIdDesc(
                        "DEVICE_COUNT_WRITE", "DEVICE-GATEWAY", "COUNT-001"))
                .thenReturn(Optional.empty());
        when(dispatchOrderRepository.findByDispatchNoForUpdate("DO202607130001"))
                .thenReturn(Optional.of(buildDispatch(DispatchStatusEnum.ISSUED.getStatus())));
        when(craftProcessRepository.findByProcessCodeAndDeletedFalseForUpdate("PR001"))
                .thenReturn(Optional.of(buildProcess()));
        DeviceCountRecordEntity previous = new DeviceCountRecordEntity();
        previous.setCountValue(100L);
        when(recordRepository
                .findFirstBySourceSystemAndEquipmentCodeAndDispatchOrderIdAndProcessIdAndDeletedFalseOrderByIdDesc(
                        "DEVICE-GATEWAY", "EQP-01", 20L, 30L))
                .thenReturn(Optional.of(previous));
        doAnswer(invocation -> {
            DeviceCountRecordEntity entity = invocation.getArgument(0);
            entity.setId(40L);
            return entity;
        }).when(recordRepository).saveAndFlush(any(DeviceCountRecordEntity.class));
        when(auditService.recordResult(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(50L);

        IntegrationWriteResultRespVO result = commandService.writeDeviceCount(reqVO, "{}");

        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getBusinessId()).isEqualTo(40L);
        ArgumentCaptor<DeviceCountRecordEntity> captor =
                ArgumentCaptor.forClass(DeviceCountRecordEntity.class);
        verify(recordRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getIncrementValue()).isEqualTo(30L);
        assertThat(captor.getValue().getCreateBy()).isEqualTo(OPERATOR_ID);
        verify(auditService).recordResult(
                IntegrationInterfaceTypeEnum.DEVICE_COUNT_WRITE,
                "DEVICE-GATEWAY", "COUNT-001", "{}",
                IntegrationWriteStatusEnum.SUCCESS, 40L, "COUNT-001");
    }

    @Test
    @DisplayName("设备计数：派工单不存在时异常池与失败日志原子记录")
    void writeDeviceCountStoresExceptionWhenDispatchMissing() {
        when(dispatchOrderRepository.findByDispatchNoForUpdate("DO202607130001"))
                .thenReturn(Optional.empty());
        doAnswer(invocation -> {
            DeviceCountExceptionEntity entity = invocation.getArgument(0);
            entity.setId(60L);
            return entity;
        }).when(exceptionRepository).saveAndFlush(any(DeviceCountExceptionEntity.class));
        when(auditService.recordFailureInCurrentTransaction(
                any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(61L);

        IntegrationWriteResultRespVO result =
                commandService.writeDeviceCount(buildReqVO(130L), "{}");

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getErrorCode()).isEqualTo("A0402");
        ArgumentCaptor<DeviceCountExceptionEntity> captor =
                ArgumentCaptor.forClass(DeviceCountExceptionEntity.class);
        verify(exceptionRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getExceptionType()).isEqualTo(
                DeviceCountExceptionTypeEnum.DISPATCH_NOT_FOUND.getValue());
        verify(craftProcessRepository, never()).findByProcessCodeAndDeletedFalseForUpdate(any());
    }

    @Test
    @DisplayName("设备计数：累计值小于最近记录时进入倒退异常池")
    void writeDeviceCountStoresRollbackException() {
        stubMatchedContext();
        DeviceCountRecordEntity previous = new DeviceCountRecordEntity();
        previous.setCountValue(150L);
        when(recordRepository
                .findFirstBySourceSystemAndEquipmentCodeAndDispatchOrderIdAndProcessIdAndDeletedFalseOrderByIdDesc(
                        any(), any(), any(), any())).thenReturn(Optional.of(previous));
        doAnswer(invocation -> {
            DeviceCountExceptionEntity entity = invocation.getArgument(0);
            entity.setId(70L);
            return entity;
        }).when(exceptionRepository).saveAndFlush(any(DeviceCountExceptionEntity.class));
        when(auditService.recordFailureInCurrentTransaction(
                any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(71L);

        IntegrationWriteResultRespVO result =
                commandService.writeDeviceCount(buildReqVO(130L), "{}");

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getErrorCode()).isEqualTo(
                IntegrationErrorCodeConstants.DEVICE_COUNT_ROLLBACK.code());
        ArgumentCaptor<DeviceCountExceptionEntity> captor =
                ArgumentCaptor.forClass(DeviceCountExceptionEntity.class);
        verify(exceptionRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getExceptionType()).isEqualTo(
                DeviceCountExceptionTypeEnum.COUNT_ROLLBACK.getValue());
        verify(recordRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("设备计数：锁定派工单后复查到处理日志时返回重复结果")
    void writeDeviceCountReturnsDuplicateFromExistingLog() {
        IntegrationWriteLogEntity existing = new IntegrationWriteLogEntity();
        existing.setId(80L);
        existing.setResultId(81L);
        existing.setResultNo("COUNT-001");
        when(writeLogRepository
                .findFirstByInterfaceTypeAndSourceSystemAndBusinessKeyOrderByIdDesc(
                        "DEVICE_COUNT_WRITE", "DEVICE-GATEWAY", "COUNT-001"))
                .thenReturn(Optional.of(existing));
        when(dispatchOrderRepository.findByDispatchNoForUpdate("DO202607130001"))
                .thenReturn(Optional.of(buildDispatch(DispatchStatusEnum.ISSUED.getStatus())));

        IntegrationWriteResultRespVO result =
                commandService.writeDeviceCount(buildReqVO(130L), "{}");

        assertThat(result.getStatus()).isEqualTo("DUPLICATE");
        assertThat(result.getLogId()).isEqualTo(80L);
        verify(dispatchOrderRepository).findByDispatchNoForUpdate("DO202607130001");
        verify(craftProcessRepository, never()).findByProcessCodeAndDeletedFalseForUpdate(any());
    }

    @Test
    @DisplayName("设备异常重试：原幂等键的失败日志被原子替换为成功结果")
    void retryExceptionReusesOriginalFailedIdempotencyKey() {
        DeviceCountExceptionEntity exception = buildPendingException();
        IntegrationWriteLogEntity failedLog = new IntegrationWriteLogEntity();
        failedLog.setId(91L);
        failedLog.setWriteStatus(IntegrationWriteStatusEnum.FAILED.getStatus());
        when(exceptionRepository.findByIdForUpdate(90L)).thenReturn(Optional.of(exception));
        when(writeLogRepository
                .findFirstByInterfaceTypeAndSourceSystemAndBusinessKeyOrderByIdDesc(
                        "DEVICE_COUNT_WRITE", "DEVICE-GATEWAY", "COUNT-001"))
                .thenReturn(Optional.of(failedLog));
        when(dispatchOrderRepository.findByDispatchNoForUpdate("DO202607130001"))
                .thenReturn(Optional.of(buildDispatch(DispatchStatusEnum.ISSUED.getStatus())));
        when(craftProcessRepository.findByProcessCodeAndDeletedFalseForUpdate("PR001"))
                .thenReturn(Optional.of(buildProcess()));
        doAnswer(invocation -> {
            DeviceCountRecordEntity entity = invocation.getArgument(0);
            entity.setId(92L);
            return entity;
        }).when(recordRepository).saveAndFlush(any(DeviceCountRecordEntity.class));
        when(auditService.replaceFailureResult(
                91L, "{\"countValue\":130}", IntegrationWriteStatusEnum.SUCCESS,
                92L, "COUNT-001", null)).thenReturn(91L);

        IntegrationWriteResultRespVO result = commandService.retryException(
                90L, buildReqVO(130L), "{\"countValue\":130}");

        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(exception.getHandleStatus()).isEqualTo(1);
        assertThat(exception.getRetryLogId()).isEqualTo(91L);
        assertThat(exception.getRetryRecordId()).isEqualTo(92L);
        verify(auditService).replaceFailureResult(
                91L, "{\"countValue\":130}", IntegrationWriteStatusEnum.SUCCESS,
                92L, "COUNT-001", null);
    }

    @Test
    @DisplayName("设备异常重试：修正为已成功的新幂等键时只关闭异常不重复写入")
    void retryExceptionAcceptsDuplicateOnlyWhenOriginalResultSucceeded() {
        DeviceCountExceptionEntity exception = buildPendingException();
        IntegrationWriteLogEntity successLog = new IntegrationWriteLogEntity();
        successLog.setId(93L);
        successLog.setWriteStatus(IntegrationWriteStatusEnum.SUCCESS.getStatus());
        successLog.setResultId(94L);
        successLog.setResultNo("COUNT-002");
        when(exceptionRepository.findByIdForUpdate(90L)).thenReturn(Optional.of(exception));
        when(writeLogRepository
                .findFirstByInterfaceTypeAndSourceSystemAndBusinessKeyOrderByIdDesc(
                        "DEVICE_COUNT_WRITE", "DEVICE-GATEWAY", "COUNT-002"))
                .thenReturn(Optional.of(successLog));
        when(dispatchOrderRepository.findByDispatchNoForUpdate("DO202607130001"))
                .thenReturn(Optional.of(buildDispatch(DispatchStatusEnum.ISSUED.getStatus())));
        DeviceCountWriteReqVO request = buildReqVO(130L);
        request.setExternalKey("COUNT-002");

        IntegrationWriteResultRespVO result = commandService.retryException(
                90L, request, "{\"externalKey\":\"COUNT-002\"}");

        assertThat(result.getStatus()).isEqualTo("DUPLICATE");
        assertThat(exception.getHandleStatus()).isEqualTo(1);
        assertThat(exception.getRetryRecordId()).isEqualTo(94L);
        verify(recordRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("设备异常分页：无匹配数据时不执行分页查询")
    @SuppressWarnings("unchecked")
    void getExceptionPageSkipsQueryWhenCountIsZero() {
        when(exceptionRepository.count(any(Specification.class))).thenReturn(0L);
        DeviceCountExceptionPageReqVO reqVO = new DeviceCountExceptionPageReqVO();

        var result = commandService.getExceptionPage(reqVO);

        assertThat(result.getTotal()).isZero();
        assertThat(result.getList()).isEmpty();
        verify(exceptionRepository, never()).findAll(
                any(Specification.class), any(org.springframework.data.domain.Pageable.class));
    }

    private void stubMatchedContext() {
        when(writeLogRepository
                .findFirstByInterfaceTypeAndSourceSystemAndBusinessKeyOrderByIdDesc(
                        any(), any(), any())).thenReturn(Optional.empty());
        when(dispatchOrderRepository.findByDispatchNoForUpdate("DO202607130001"))
                .thenReturn(Optional.of(buildDispatch(DispatchStatusEnum.EXECUTING.getStatus())));
        when(craftProcessRepository.findByProcessCodeAndDeletedFalseForUpdate("PR001"))
                .thenReturn(Optional.of(buildProcess()));
    }

    private DeviceCountWriteReqVO buildReqVO(Long countValue) {
        DeviceCountWriteReqVO reqVO = new DeviceCountWriteReqVO();
        reqVO.setSourceSystem("device-gateway");
        reqVO.setExternalKey("COUNT-001");
        reqVO.setEquipmentCode("eqp-01");
        reqVO.setDispatchNo("DO202607130001");
        reqVO.setProcessCode("pr001");
        reqVO.setCollectTime(LocalDateTime.of(2026, 7, 13, 10, 30));
        reqVO.setCountValue(countValue);
        return reqVO;
    }

    private DeviceCountExceptionEntity buildPendingException() {
        DeviceCountExceptionEntity exception = new DeviceCountExceptionEntity();
        exception.setId(90L);
        exception.setSourceSystem("DEVICE-GATEWAY");
        exception.setExternalKey("COUNT-001");
        exception.setHandleStatus(0);
        return exception;
    }

    private DispatchOrderEntity buildDispatch(Integer status) {
        DispatchOrderEntity dispatch = new DispatchOrderEntity();
        dispatch.setId(20L);
        dispatch.setDispatchNo("DO202607130001");
        dispatch.setDispatchStatus(status);
        return dispatch;
    }

    private CraftProcessEntity buildProcess() {
        CraftProcessEntity process = new CraftProcessEntity();
        process.setId(30L);
        process.setProcessCode("PR001");
        return process;
    }
}
