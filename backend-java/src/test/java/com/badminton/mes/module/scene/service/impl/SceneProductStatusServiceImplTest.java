package com.badminton.mes.module.scene.service.impl;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.scene.controller.vo.SceneProcessHistoryRespVO;
import com.badminton.mes.module.scene.controller.vo.SceneProductStatusPageReqVO;
import com.badminton.mes.module.scene.controller.vo.SceneProductStatusRespVO;
import com.badminton.mes.module.scene.controller.vo.SceneStatusHistoryRespVO;
import com.badminton.mes.module.scene.dal.entity.SceneBatchProcessHistoryEntity;
import com.badminton.mes.module.scene.dal.entity.SceneBatchStatusEntity;
import com.badminton.mes.module.scene.dal.entity.SceneBatchStatusHistoryEntity;
import com.badminton.mes.module.scene.dal.entity.SceneProductionTaskEntity;
import com.badminton.mes.module.scene.dal.repository.SceneBatchProcessHistoryRepository;
import com.badminton.mes.module.scene.dal.repository.SceneBatchStatusHistoryRepository;
import com.badminton.mes.module.scene.dal.repository.SceneBatchStatusRepository;
import com.badminton.mes.module.scene.dal.repository.SceneProductionTaskRepository;
import com.badminton.mes.module.scene.service.SceneDataScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.badminton.mes.module.scene.constants.SceneErrorCodeConstants.TASK_NOT_EXISTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M2 产品当前状态、状态履历和工序履历查询测试。
 *
 * @author 范家权
 */
@ExtendWith(MockitoExtension.class)
class SceneProductStatusServiceImplTest {

    @Mock
    private SceneBatchStatusRepository batchStatusRepository;

    @Mock
    private SceneBatchStatusHistoryRepository statusHistoryRepository;

    @Mock
    private SceneBatchProcessHistoryRepository processHistoryRepository;

    @Mock
    private SceneProductionTaskRepository taskRepository;

    @Mock
    private SceneDataScopeService dataScope;

    private SceneProductStatusServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SceneProductStatusServiceImpl(batchStatusRepository,
                statusHistoryRepository, processHistoryRepository, taskRepository, dataScope);
    }

    @Test
    void shouldReturnCurrentBatchSnapshotAfterObjectScopeCheck() {
        LocalDateTime updateTime = LocalDateTime.of(2026, 7, 13, 15, 0);
        SceneBatchStatusEntity batch = batchStatus();
        batch.setUpdateTime(updateTime);
        when(batchStatusRepository.findByBatchNoAndDeletedFalse("BATCH-001"))
                .thenReturn(Optional.of(batch));
        when(taskRepository.findByIdAndDeletedFalse(30L)).thenReturn(Optional.of(task()));

        SceneProductStatusRespVO result = service.getByBatch("BATCH-001");

        assertThat(result.getId()).isEqualTo(70L);
        assertThat(result.getBatchNo()).isEqualTo("BATCH-001");
        assertThat(result.getTaskId()).isEqualTo(30L);
        assertThat(result.getProductId()).isEqualTo(201L);
        assertThat(result.getCurrentProcessId()).isEqualTo(40L);
        assertThat(result.getCurrentProcessName()).isEqualTo("穿线");
        assertThat(result.getBatchStatus()).isEqualTo(1);
        assertThat(result.getAbnormal()).isFalse();
        assertThat(result.getUpdateTime()).isEqualTo(updateTime);
        verify(dataScope).check(501L, 301L);
    }

    @Test
    void shouldHideUnknownBatchBehindStableBusinessError() {
        when(batchStatusRepository.findByBatchNoAndDeletedFalse("UNKNOWN"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByBatch("UNKNOWN"))
                .isInstanceOf(ServiceException.class)
                .satisfies(throwable -> assertThat(((ServiceException) throwable).getErrorCode())
                        .isEqualTo(TASK_NOT_EXISTS));
        verify(taskRepository, never()).findByIdAndDeletedFalse(any());
    }

    @Test
    void shouldMapStatusHistoryInRepositoryOrder() {
        SceneBatchStatusHistoryEntity history = new SceneBatchStatusHistoryEntity();
        LocalDateTime operatedAt = LocalDateTime.of(2026, 7, 13, 15, 10);
        history.setId(81L);
        history.setFromStatus(0);
        history.setToStatus(1);
        history.setProcessId(40L);
        history.setChangeReason("首道工序进入生产");
        history.setOperatorId(51L);
        history.setOperateTime(operatedAt);
        stubAccessibleBatch();
        when(statusHistoryRepository
                .findByBatchStatusIdAndDeletedFalseOrderByOperateTimeDescIdDesc(70L))
                .thenReturn(List.of(history));

        List<SceneStatusHistoryRespVO> result = service.histories(70L);

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.getId()).isEqualTo(81L);
            assertThat(item.getFromStatus()).isZero();
            assertThat(item.getToStatus()).isEqualTo(1);
            assertThat(item.getProcessId()).isEqualTo(40L);
            assertThat(item.getChangeReason()).isEqualTo("首道工序进入生产");
            assertThat(item.getOperatorId()).isEqualTo(51L);
            assertThat(item.getOperateTime()).isEqualTo(operatedAt);
        });
    }

    @Test
    void shouldMapProcessHistoryWithoutExposingPersistenceEntity() {
        SceneBatchProcessHistoryEntity history = new SceneBatchProcessHistoryEntity();
        LocalDateTime operatedAt = LocalDateTime.of(2026, 7, 13, 15, 20);
        history.setId(91L);
        history.setDispatchDetailId(10L);
        history.setProcessId(40L);
        history.setProcessCode("PX-001");
        history.setProcessName("穿线");
        history.setActionType(2);
        history.setOperatorId(51L);
        history.setActionReason("开始作业");
        history.setOperateTime(operatedAt);
        stubAccessibleBatch();
        when(processHistoryRepository
                .findByBatchStatusIdAndDeletedFalseOrderByOperateTimeDescIdDesc(70L))
                .thenReturn(List.of(history));

        List<SceneProcessHistoryRespVO> result = service.processHistories(70L);

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.getId()).isEqualTo(91L);
            assertThat(item.getDispatchDetailId()).isEqualTo(10L);
            assertThat(item.getProcessId()).isEqualTo(40L);
            assertThat(item.getProcessCode()).isEqualTo("PX-001");
            assertThat(item.getProcessName()).isEqualTo("穿线");
            assertThat(item.getActionType()).isEqualTo(2);
            assertThat(item.getOperatorId()).isEqualTo(51L);
            assertThat(item.getActionReason()).isEqualTo("开始作业");
            assertThat(item.getOperateTime()).isEqualTo(operatedAt);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyPageWithoutIssuingPageQueryWhenCountIsZero() {
        SceneProductStatusPageReqVO request = new SceneProductStatusPageReqVO();
        request.setPageNo(1);
        request.setPageSize(20);
        when(batchStatusRepository.count(any(Specification.class))).thenReturn(0L);

        PageResult<SceneProductStatusRespVO> result = service.page(request);

        assertThat(result.getList()).isEmpty();
        assertThat(result.getTotal()).isZero();
        assertThat(result.getPageNo()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(20);
        verify(batchStatusRepository, never())
                .findAll(any(Specification.class), any(Pageable.class));
    }

    private void stubAccessibleBatch() {
        when(batchStatusRepository.findByIdAndDeletedFalse(70L))
                .thenReturn(Optional.of(batchStatus()));
        when(taskRepository.findByIdAndDeletedFalse(30L)).thenReturn(Optional.of(task()));
    }

    private static SceneBatchStatusEntity batchStatus() {
        SceneBatchStatusEntity batch = new SceneBatchStatusEntity();
        batch.setId(70L);
        batch.setBatchNo("BATCH-001");
        batch.setTaskId(30L);
        batch.setProductId(201L);
        batch.setCurrentProcessId(40L);
        batch.setCurrentProcessName("穿线");
        batch.setBatchStatus(1);
        batch.setAbnormal(false);
        return batch;
    }

    private static SceneProductionTaskEntity task() {
        SceneProductionTaskEntity task = new SceneProductionTaskEntity();
        task.setId(30L);
        task.setWorkshopId(501L);
        task.setLineId(301L);
        return task;
    }
}
