package com.badminton.mes.module.integration.service.impl;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.integration.constants.IntegrationErrorCodeConstants;
import com.badminton.mes.module.integration.controller.vo.ExternalWorkOrderWriteReqVO;
import com.badminton.mes.module.integration.controller.vo.IntegrationWriteLogPageReqVO;
import com.badminton.mes.module.integration.controller.vo.IntegrationWriteLogRespVO;
import com.badminton.mes.module.integration.controller.vo.IntegrationWriteResultRespVO;
import com.badminton.mes.module.integration.controller.vo.UnitWriteReqVO;
import com.badminton.mes.module.integration.dal.entity.IntegrationWriteLogEntity;
import com.badminton.mes.module.integration.dal.repository.IntegrationWriteLogRepository;
import com.badminton.mes.module.integration.enums.IntegrationInterfaceTypeEnum;
import com.badminton.mes.module.integration.service.IntegrationAuditService;
import com.badminton.mes.module.integration.service.IntegrationWriteCommandService;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link IntegrationServiceImpl} 单元测试。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
@ExtendWith(MockitoExtension.class)
class IntegrationServiceImplTest {

    @Mock
    private IntegrationWriteCommandService commandService;

    @Mock
    private IntegrationAuditService auditService;

    @Mock
    private IntegrationWriteLogRepository writeLogRepository;

    private IntegrationServiceImpl integrationService;

    @BeforeEach
    void setUp() {
        integrationService = new IntegrationServiceImpl(
                commandService, auditService, writeLogRepository);
    }

    @Test
    @DisplayName("单位写入：业务命令回滚后以独立日志返回失败结果")
    void writeUnitRecordsBusinessFailure() {
        UnitWriteReqVO reqVO = buildUnitReqVO();
        when(auditService.serializeRequest(reqVO)).thenReturn("{}");
        when(commandService.writeUnit(reqVO, "{}"))
                .thenThrow(new ServiceException(IntegrationErrorCodeConstants.UNIT_PRECISION_IN_USE));
        when(auditService.recordFailure(any(), any(), any(), any(),
                any(), any(), any(), any())).thenReturn(500L);

        IntegrationWriteResultRespVO result = integrationService.writeUnit(reqVO);

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getLogId()).isEqualTo(500L);
        assertThat(result.getErrorCode()).isEqualTo("A0440");
        verify(auditService).recordFailure(
                eq(IntegrationInterfaceTypeEnum.UNIT_WRITE), eq("ERP-MAIN"), eq("PCS"), eq("{}"),
                eq(null), eq(null), eq(IntegrationErrorCodeConstants.UNIT_PRECISION_IN_USE), any());
    }

    @Test
    @DisplayName("工单写入：并发唯一键竞争与普通重复返回相同契约")
    void writeWorkOrderReturnsStableDuplicateContractAfterConstraintRace() {
        ExternalWorkOrderWriteReqVO reqVO = new ExternalWorkOrderWriteReqVO();
        reqVO.setSourceSystem("ERP-MAIN");
        reqVO.setExternalWorkOrderNo("ERP-WO-001");
        when(auditService.serializeRequest(reqVO)).thenReturn("{}");
        when(commandService.writeWorkOrder(reqVO, "{}"))
                .thenThrow(new ServiceException(
                        IntegrationErrorCodeConstants.EXTERNAL_WORK_ORDER_DUPLICATE));
        WorkOrderEntity existing = new WorkOrderEntity();
        existing.setId(60L);
        existing.setWorkOrderNo("WO202607110002");
        when(commandService.findExternalWorkOrder("ERP-MAIN", "ERP-WO-001"))
                .thenReturn(Optional.of(existing));
        when(auditService.recordDuplicate(any(), any(), any(), any(), any(), any()))
                .thenReturn(501L);

        IntegrationWriteResultRespVO result = integrationService.writeWorkOrder(reqVO);

        assertThat(result.getStatus()).isEqualTo("DUPLICATE");
        assertThat(result.getErrorCode()).isNull();
        assertThat(result.getBusinessId()).isEqualTo(60L);
        verify(auditService).recordDuplicate(
                IntegrationInterfaceTypeEnum.WORK_ORDER_WRITE,
                "ERP-MAIN", "ERP-WO-001", "{}", 60L, "WO202607110002");
        verify(auditService, never()).recordFailure(
                any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("日志查询：请求页码超界时返回最后一页")
    @SuppressWarnings("unchecked")
    void getWriteLogPageNormalizesOverflowPage() {
        IntegrationWriteLogPageReqVO reqVO = new IntegrationWriteLogPageReqVO();
        reqVO.setPageNo(9);
        reqVO.setPageSize(10);
        IntegrationWriteLogEntity log = new IntegrationWriteLogEntity();
        log.setId(100L);
        log.setInterfaceType("UNIT_WRITE");
        when(writeLogRepository.count(any(Specification.class))).thenReturn(11L);
        when(writeLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        PageResult<IntegrationWriteLogRespVO> result =
                integrationService.getWriteLogPage(reqVO);

        assertThat(result.getPageNo()).isEqualTo(2);
        assertThat(result.getTotal()).isEqualTo(11L);
        assertThat(result.getList()).singleElement()
                .extracting(IntegrationWriteLogRespVO::getId).isEqualTo(100L);
    }

    private UnitWriteReqVO buildUnitReqVO() {
        UnitWriteReqVO reqVO = new UnitWriteReqVO();
        reqVO.setSourceSystem("ERP-MAIN");
        reqVO.setUnitCode("PCS");
        reqVO.setUnitName("个");
        reqVO.setDecimalPrecision(0);
        reqVO.setStatus(1);
        return reqVO;
    }
}
