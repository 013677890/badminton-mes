package com.badminton.mes.module.integration.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.integration.constants.IntegrationErrorCodeConstants;
import com.badminton.mes.module.integration.controller.vo.ErpCraftSyncReqVO;
import com.badminton.mes.module.integration.controller.vo.ErpCraftSyncRespVO;
import com.badminton.mes.module.integration.controller.vo.ErpTaskSyncReqVO;
import com.badminton.mes.module.integration.controller.vo.ErpTaskSyncRespVO;
import com.badminton.mes.module.integration.dal.entity.ErpCraftPendingEntity;
import com.badminton.mes.module.integration.dal.repository.IntegrationWriteLogRepository;
import com.badminton.mes.module.integration.enums.IntegrationInterfaceTypeEnum;
import com.badminton.mes.module.integration.service.ErpCraftSyncCommandService;
import com.badminton.mes.module.integration.service.ErpMockDataSource;
import com.badminton.mes.module.integration.service.ErpTaskSyncCommandService;
import com.badminton.mes.module.integration.service.IntegrationAuditService;
import com.badminton.mes.module.integration.service.dto.ErpCraftDTO;
import com.badminton.mes.module.integration.service.dto.ErpCraftStepDTO;
import com.badminton.mes.module.integration.service.dto.ErpTaskDTO;
import com.badminton.mes.module.integration.service.dto.IntegrationCommandResult;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpSyncServiceImpl} 批处理与并发幂等回归测试。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@ExtendWith(MockitoExtension.class)
class ErpSyncServiceImplTest {

    private static final String SOURCE_SYSTEM = "ERP-MAIN";

    @Mock
    private ErpMockDataSource mockDataSource;

    @Mock
    private ErpTaskSyncCommandService taskSyncCommandService;

    @Mock
    private ErpCraftSyncCommandService craftSyncCommandService;

    @Mock
    private IntegrationAuditService auditService;

    @Mock
    private IntegrationWriteLogRepository writeLogRepository;

    private ErpSyncServiceImpl erpSyncService;

    @BeforeEach
    void setUp() {
        erpSyncService = new ErpSyncServiceImpl(
                mockDataSource,
                taskSyncCommandService,
                craftSyncCommandService,
                auditService,
                writeLogRepository);
    }

    @Test
    @DisplayName("任务同步：唯一键竞争后查询获胜工单并返回重复")
    void taskConstraintRaceReturnsDuplicate() {
        ErpTaskDTO task = buildTask("ERP-TASK-001");
        WorkOrderEntity existing = new WorkOrderEntity();
        existing.setId(10L);
        existing.setWorkOrderNo("WO202607130001");
        when(mockDataSource.fetchTasks()).thenReturn(List.of(task));
        when(auditService.serializeRequest(task)).thenReturn("{}");
        when(taskSyncCommandService.syncTask(task, "{}", SOURCE_SYSTEM))
                .thenThrow(new ServiceException(
                        IntegrationErrorCodeConstants.EXTERNAL_WORK_ORDER_DUPLICATE));
        when(taskSyncCommandService.findSyncedTask(SOURCE_SYSTEM, "ERP-TASK-001"))
                .thenReturn(Optional.of(existing));
        when(auditService.recordDuplicate(any(), any(), any(), any(), any(), any()))
                .thenReturn(100L);

        ErpTaskSyncRespVO response = erpSyncService.syncErpTasks(buildTaskRequest());

        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(response.getDuplicateCount()).isEqualTo(1);
        assertThat(response.getFailureCount()).isZero();
        assertThat(response.getDetails()).singleElement().satisfies(detail -> {
            assertThat(detail.getStatus()).isEqualTo("DUPLICATE");
            assertThat(detail.getWorkOrderId()).isEqualTo(10L);
        });
        verify(auditService).recordDuplicate(
                IntegrationInterfaceTypeEnum.ERP_TASK_SYNC,
                SOURCE_SYSTEM,
                "ERP-TASK-001",
                "{}",
                10L,
                "WO202607130001");
        verify(auditService, never()).recordFailure(
                any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("任务同步：单条来源数据异常不阻断后续任务")
    void invalidTaskDoesNotStopFollowingTask() {
        ErpTaskDTO invalidTask = buildTask(null);
        ErpTaskDTO validTask = buildTask("ERP-TASK-002");
        when(mockDataSource.fetchTasks()).thenReturn(List.of(invalidTask, validTask));
        when(auditService.serializeRequest(any())).thenReturn("{}");
        when(taskSyncCommandService.syncTask(invalidTask, "{}", SOURCE_SYSTEM))
                .thenThrow(new ServiceException(
                        IntegrationErrorCodeConstants.ERP_SOURCE_DATA_INVALID));
        when(taskSyncCommandService.syncTask(validTask, "{}", SOURCE_SYSTEM))
                .thenReturn(new IntegrationCommandResult(
                        20L, "WO202607130002", false, 102L));

        ErpTaskSyncRespVO response = erpSyncService.syncErpTasks(buildTaskRequest());

        assertThat(response.getTotalCount()).isEqualTo(2);
        assertThat(response.getFailureCount()).isEqualTo(1);
        assertThat(response.getSuccessCount()).isEqualTo(1);
        assertThat(response.getDetails()).extracting(ErpTaskSyncRespVO.Detail::getStatus)
                .containsExactly("FAILED", "SUCCESS");
        verify(auditService).recordFailure(
                eq(IntegrationInterfaceTypeEnum.ERP_TASK_SYNC),
                eq(SOURCE_SYSTEM),
                eq("INVALID_SOURCE_DATA"),
                eq("{}"),
                eq(null),
                eq(null),
                eq(IntegrationErrorCodeConstants.ERP_SOURCE_DATA_INVALID),
                any());
    }

    @Test
    @DisplayName("工艺同步：唯一键竞争后查询获胜暂存数据并返回重复")
    void craftConstraintRaceReturnsDuplicate() {
        ErpCraftDTO craft = buildCraft();
        ErpCraftPendingEntity existing = new ErpCraftPendingEntity();
        existing.setId(30L);
        existing.setSourceSystem(SOURCE_SYSTEM);
        existing.setErpRoutingCode("ROUTE-001");
        existing.setErpRoutingVersion("V1");
        when(mockDataSource.fetchCrafts()).thenReturn(List.of(craft));
        when(auditService.serializeRequest(craft)).thenReturn("{}");
        when(craftSyncCommandService.syncCraft(craft, "{}", SOURCE_SYSTEM))
                .thenThrow(new ServiceException(
                        IntegrationErrorCodeConstants.ERP_CRAFT_DUPLICATE));
        when(craftSyncCommandService.findSyncedCraft(
                SOURCE_SYSTEM, "ROUTE-001", "V1"))
                .thenReturn(Optional.of(existing));
        when(auditService.recordDuplicate(any(), any(), any(), any(), any(), any()))
                .thenReturn(103L);

        ErpCraftSyncRespVO response = erpSyncService.syncErpCrafts(buildCraftRequest());

        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(response.getDuplicateCount()).isEqualTo(1);
        assertThat(response.getFailureCount()).isZero();
        assertThat(response.getPendingItems()).isEmpty();
        verify(auditService).recordDuplicate(
                IntegrationInterfaceTypeEnum.ERP_CRAFT_SYNC,
                SOURCE_SYSTEM,
                "ROUTE-001:V1",
                "{}",
                30L,
                "ROUTE-001:V1");
        verify(auditService, never()).recordFailure(
                any(), any(), any(), any(), any(), any(), any(), any());
    }

    private ErpTaskSyncReqVO buildTaskRequest() {
        ErpTaskSyncReqVO request = new ErpTaskSyncReqVO();
        request.setSourceSystem(SOURCE_SYSTEM);
        return request;
    }

    private ErpCraftSyncReqVO buildCraftRequest() {
        ErpCraftSyncReqVO request = new ErpCraftSyncReqVO();
        request.setSourceSystem(SOURCE_SYSTEM);
        return request;
    }

    private ErpTaskDTO buildTask(String erpOrderNo) {
        return new ErpTaskDTO(
                erpOrderNo,
                "PRODUCT-001",
                100,
                LocalDateTime.of(2026, 7, 14, 8, 0),
                LocalDateTime.of(2026, 7, 14, 18, 0),
                "WORKSHOP-001",
                "BATCH-001");
    }

    private ErpCraftDTO buildCraft() {
        return new ErpCraftDTO(
                "ROUTE-001",
                "标准路线",
                "V1",
                "PRODUCT-001",
                List.of(new ErpCraftStepDTO(1, "PR001", "羽毛分拣")));
    }
}
