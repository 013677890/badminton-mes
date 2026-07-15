package com.badminton.mes.module.production.service.impl;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.dal.redis.WorkOrderCache;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

/**
 * 工单现场执行汇总写入测试。
 *
 * @author 张竹灏
 * @date 2026/07/14
 */
class WorkOrderExecutionSummaryServiceImplTest {

    private final WorkOrderRepository repository = mock(WorkOrderRepository.class);
    private final WorkOrderCache cache = mock(WorkOrderCache.class);
    private final WorkOrderExecutionSummaryServiceImpl service =
            new WorkOrderExecutionSummaryServiceImpl(repository, cache);

    @Test
    void reportAndReversalUseSignedAtomicDeltas() {
        when(repository.adjustExecutionSummary(1L, 10, 2, 1)).thenReturn(1);
        when(repository.adjustExecutionSummary(1L, -10, -2, -1)).thenReturn(1);

        service.adjustReportedQuantities(1L, 10, 2, 1);
        service.adjustReportedQuantities(1L, -10, -2, -1);

        verify(cache, times(2)).evictAfterCommit(1L);
        verify(repository).adjustExecutionSummary(1L, -10, -2, -1);
    }

    @Test
    void invalidExecutionSummaryIsRejected() {
        when(repository.adjustExecutionSummary(1L, 10, 2, 1)).thenReturn(0);

        assertThatThrownBy(() -> service.adjustReportedQuantities(1L, 10, 2, 1))
                .isInstanceOfSatisfying(ServiceException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ProductionErrorCodeConstants.WORK_ORDER_EXECUTION_QUANTITY_INVALID));
    }

    @Test
    void approvedCompletionUpdatesFinishQuantity() {
        when(repository.increaseApprovedFinishQuantity(1L, 8)).thenReturn(1);

        service.addApprovedCompletion(1L, 8);

        verify(repository).increaseApprovedFinishQuantity(1L, 8);
        verify(cache).evictAfterCommit(1L);
    }
}
