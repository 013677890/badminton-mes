package com.badminton.mes.module.integration.service;

import java.time.LocalDateTime;
import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.integration.constants.IntegrationErrorCodeConstants;
import com.badminton.mes.module.integration.service.dto.ErpTaskDTO;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;
import com.badminton.mes.module.production.dal.redis.WorkOrderNoSequence;
import com.badminton.mes.module.production.dal.repository.ProductRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.production.dal.repository.WorkshopRepository;
import com.badminton.mes.module.production.enums.WorkOrderSourceTypeEnum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpTaskSyncCommandService} 来源结构校验与幂等查询测试。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@ExtendWith(MockitoExtension.class)
class ErpTaskSyncCommandServiceTest {

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WorkshopRepository workshopRepository;

    @Mock
    private WorkOrderNoSequence workOrderNoSequence;

    @Mock
    private IntegrationAuditService auditService;

    private ErpTaskSyncCommandService commandService;

    @BeforeEach
    void setUp() {
        commandService = new ErpTaskSyncCommandService(
                workOrderRepository,
                productRepository,
                workshopRepository,
                workOrderNoSequence,
                auditService);
    }

    @Test
    @DisplayName("任务同步：关键来源字段缺失时在访问数据库前拒绝")
    void missingSourceFieldIsRejectedBeforeDatabaseAccess() {
        ErpTaskDTO invalidTask = new ErpTaskDTO(
                null,
                "PRODUCT-001",
                100,
                LocalDateTime.of(2026, 7, 14, 8, 0),
                LocalDateTime.of(2026, 7, 14, 18, 0),
                "WORKSHOP-001",
                null);

        assertThatThrownBy(() -> commandService.syncTask(
                invalidTask, "{}", "ERP-MAIN"))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                IntegrationErrorCodeConstants.ERP_SOURCE_DATA_INVALID));

        verify(workOrderRepository, never())
                .findBySourceTypeAndSourceSystemAndSourceOrderNo(
                        WorkOrderSourceTypeEnum.ERP_SYNC.getType(),
                        "ERP-MAIN",
                        null);
    }

    @Test
    @DisplayName("任务查询：使用 ERP 同步来源三元键查询并发获胜工单")
    void findSyncedTaskUsesErpSourceIdempotencyKey() {
        WorkOrderEntity existing = new WorkOrderEntity();
        existing.setId(10L);
        when(workOrderRepository.findBySourceTypeAndSourceSystemAndSourceOrderNo(
                WorkOrderSourceTypeEnum.ERP_SYNC.getType(),
                "ERP-MAIN",
                "ERP-TASK-001"))
                .thenReturn(Optional.of(existing));

        Optional<WorkOrderEntity> result = commandService.findSyncedTask(
                "ERP-MAIN", "ERP-TASK-001");

        assertThat(result).contains(existing);
    }
}
