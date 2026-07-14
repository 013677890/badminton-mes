package com.badminton.mes.module.scene.service.impl;

import java.util.Optional;

import com.badminton.mes.module.scene.controller.vo.SceneWorkReportSubmitReqVO;
import com.badminton.mes.module.scene.dal.entity.SceneWorkReportEntity;
import com.badminton.mes.module.scene.dal.repository.SceneWorkReportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 报工入口幂等恢复测试。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
class SceneWorkReportServiceImplTest {

    private final SceneWorkReportRepository reportRepository = mock(SceneWorkReportRepository.class);
    private final SceneWorkReportTransactionalService transactionalService =
            mock(SceneWorkReportTransactionalService.class);
    private final SceneWorkReportServiceImpl service =
            new SceneWorkReportServiceImpl(reportRepository, transactionalService);

    @Test
    void duplicateRequestReturnsExistingIdWithoutNewTransaction() {
        SceneWorkReportEntity existed = new SceneWorkReportEntity();
        existed.setId(7L);
        when(reportRepository.findByRequestNoAndDeletedFalse("REQ-1"))
                .thenReturn(Optional.of(existed));

        assertThat(service.submit(request("REQ-1"), 1)).isEqualTo(7L);

        verify(transactionalService, never()).submit(request("REQ-1"), 1);
    }

    @Test
    void uniqueConflictRecoversCommittedRequestId() {
        SceneWorkReportEntity committed = new SceneWorkReportEntity();
        committed.setId(8L);
        when(reportRepository.findByRequestNoAndDeletedFalse("REQ-2"))
                .thenReturn(Optional.empty(), Optional.of(committed));
        when(transactionalService.submit(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(1)))
                .thenThrow(new DataIntegrityViolationException("uk_request_no"));

        assertThat(service.submit(request("REQ-2"), 1)).isEqualTo(8L);
    }

    @Test
    void nonIdempotencyConstraintFailureIsRethrown() {
        when(reportRepository.findByRequestNoAndDeletedFalse("REQ-3"))
                .thenReturn(Optional.empty());
        when(transactionalService.submit(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(1)))
                .thenThrow(new DataIntegrityViolationException("other_constraint"));

        assertThatThrownBy(() -> service.submit(request("REQ-3"), 1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private SceneWorkReportSubmitReqVO request(String requestNo) {
        SceneWorkReportSubmitReqVO reqVO = new SceneWorkReportSubmitReqVO();
        reqVO.setRequestNo(requestNo);
        return reqVO;
    }
}
