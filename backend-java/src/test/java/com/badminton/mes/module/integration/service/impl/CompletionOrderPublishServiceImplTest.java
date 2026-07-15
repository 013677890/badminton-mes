package com.badminton.mes.module.integration.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import com.badminton.mes.module.integration.dal.entity.CompletionOrderEntity;
import com.badminton.mes.module.integration.dal.repository.CompletionOrderRepository;
import com.badminton.mes.module.integration.service.dto.ApprovedCompletionDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 审核通过完工单发布测试。
 *
 * @author 张竹灏
 * @date 2026/07/14
 */
class CompletionOrderPublishServiceImplTest {

    private final CompletionOrderRepository repository = mock(CompletionOrderRepository.class);
    private final CompletionOrderPublishServiceImpl service = new CompletionOrderPublishServiceImpl(repository);

    @Test
    void duplicateCompletionReturnsExistingId() {
        CompletionOrderEntity existed = new CompletionOrderEntity();
        existed.setId(9L);
        when(repository.findByCompletionNoAndDeletedFalse("WG001")).thenReturn(Optional.of(existed));

        assertThat(service.publishApproved(command())).isEqualTo(9L);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void newCompletionIsPublishedForErpRead() {
        when(repository.findByCompletionNoAndDeletedFalse("WG001")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
            CompletionOrderEntity entity = invocation.getArgument(0);
            entity.setId(10L);
            return entity;
        });

        assertThat(service.publishApproved(command())).isEqualTo(10L);
        ArgumentCaptor<CompletionOrderEntity> captor = ArgumentCaptor.forClass(CompletionOrderEntity.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getAuditStatus()).isEqualTo(1);
        assertThat(captor.getValue().getProductionTaskId()).isEqualTo(2L);
        assertThat(captor.getValue().getCompletionQuantity()).isEqualTo(8);
    }

    private ApprovedCompletionDTO command() {
        return new ApprovedCompletionDTO("WG001", 2L, 1L, "WO001", 3L,
                "P001", "羽毛球", "B001", 8, 8, 0, 7L,
                LocalDateTime.of(2026, 7, 14, 10, 0), "审核通过");
    }
}
