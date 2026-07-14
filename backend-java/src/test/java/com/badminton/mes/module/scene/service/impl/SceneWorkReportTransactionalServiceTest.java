package com.badminton.mes.module.scene.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.barcode.service.BarcodeSceneService;
import com.badminton.mes.module.scene.constants.SceneErrorCodeConstants;
import com.badminton.mes.module.scene.controller.vo.SceneProductionParameterRespVO;
import com.badminton.mes.module.scene.controller.vo.SceneWorkReportReverseReqVO;
import com.badminton.mes.module.scene.controller.vo.SceneWorkReportSubmitReqVO;
import com.badminton.mes.module.scene.dal.entity.SceneDispatchDetailEntity;
import com.badminton.mes.module.scene.dal.entity.SceneProductionTaskEntity;
import com.badminton.mes.module.scene.dal.entity.SceneWorkReportEntity;
import com.badminton.mes.module.scene.dal.repository.SceneDispatchDetailRepository;
import com.badminton.mes.module.scene.dal.repository.SceneProductionTaskRepository;
import com.badminton.mes.module.scene.dal.repository.SceneWorkReportRepository;
import com.badminton.mes.module.scene.service.SceneDataScopeService;
import com.badminton.mes.module.scene.service.SceneProductionParameterService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 报工事务业务规则测试。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
class SceneWorkReportTransactionalServiceTest {

    private final SceneWorkReportRepository reportRepository = mock(SceneWorkReportRepository.class);
    private final SceneProductionTaskRepository taskRepository = mock(SceneProductionTaskRepository.class);
    private final SceneDispatchDetailRepository detailRepository = mock(SceneDispatchDetailRepository.class);
    private final SceneDataScopeService dataScopeService = mock(SceneDataScopeService.class);
    private final SceneProductionParameterService parameterService = mock(SceneProductionParameterService.class);
    private final BarcodeSceneService barcodeSceneService = mock(BarcodeSceneService.class);
    private final SceneWorkReportTransactionalService service = new SceneWorkReportTransactionalService(
            reportRepository, taskRepository, detailRepository, dataScopeService,
            parameterService, barcodeSceneService);

    @BeforeEach
    void setUp() {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(9L);
        loginUser.setRoleCodes(List.of(RoleCodeConstants.ADMIN));
        SecurityContextHolder.set("test-token", loginUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    void requiredBarcodeCreatesUseRecordAndUpdatesNetQuantities() {
        stubSubmitBase("1");
        when(barcodeSceneService.validateAndRecordUse("BC-001", 1L, 11L, "BATCH-1",
                3L, 9L, 6L, 3))
                .thenReturn(new BarcodeSceneService.BarcodeSceneSnapshot(100L, 11L, "BATCH-1", 1L));
        when(reportRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            SceneWorkReportEntity report = invocation.getArgument(0);
            report.setId(10L);
            return report;
        });

        SceneWorkReportSubmitReqVO reqVO = request();
        reqVO.setBarcodeValue("BC-001");

        assertThat(service.submit(reqVO, 1)).isEqualTo(10L);
        ArgumentCaptor<SceneWorkReportEntity> captor = ArgumentCaptor.forClass(SceneWorkReportEntity.class);
        verify(reportRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getBarcodeId()).isEqualTo(100L);
        assertThat(task().getId()).isEqualTo(1L);
        verify(taskRepository).save(any(SceneProductionTaskEntity.class));
        verify(detailRepository).save(any(SceneDispatchDetailEntity.class));
    }

    @Test
    void optionalBarcodeCanBeOmitted() {
        stubSubmitBase("0");
        when(reportRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            SceneWorkReportEntity report = invocation.getArgument(0);
            report.setId(10L);
            return report;
        });

        assertThat(service.submit(request(), 1)).isEqualTo(10L);
        verify(barcodeSceneService, never()).validateAndRecordUse(any(), any(), any(), any(),
                any(), any(), any(), any());
    }

    @Test
    void requiredBarcodeMissingIsRejected() {
        stubSubmitBase("1");

        assertThatThrownBy(() -> service.submit(request(), 1))
                .isInstanceOfSatisfying(ServiceException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(SceneErrorCodeConstants.REPORT_BARCODE_REQUIRED));
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    void pausedOperationIsNotReportable() {
        SceneProductionTaskEntity task = task();
        SceneDispatchDetailEntity detail = detail();
        detail.setPaused(true);
        when(reportRepository.findByRequestNoAndDeletedFalse("REQ-1")).thenReturn(Optional.empty());
        when(detailRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(detail));
        when(taskRepository.findByIdAndDeletedFalseForUpdate(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.submit(request(), 1))
                .isInstanceOfSatisfying(ServiceException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(SceneErrorCodeConstants.REPORT_STATUS_INVALID));
    }

    @Test
    void invalidQuantityIsRejectedBeforeBarcodeUse() {
        stubSubmitBase("1");
        SceneWorkReportSubmitReqVO reqVO = request();
        reqVO.setInputQuantity(10);
        reqVO.setGoodQuantity(9);
        reqVO.setDefectQuantity(2);

        assertThatThrownBy(() -> service.submit(reqVO, 1))
                .isInstanceOfSatisfying(ServiceException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(SceneErrorCodeConstants.REPORT_QUANTITY_INVALID));
        verify(barcodeSceneService, never()).validateAndRecordUse(any(), any(), any(), any(),
                any(), any(), any(), any());
    }

    @Test
    void completedOperationRemainsReportable() {
        SceneDispatchDetailEntity detail = detail();
        detail.setDetailStatus(2);
        stubSubmitBase("0", task(), detail);
        when(reportRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            SceneWorkReportEntity report = invocation.getArgument(0);
            report.setId(12L);
            return report;
        });

        assertThat(service.submit(request(), 1)).isEqualTo(12L);
    }

    @Test
    void fullReversalUpdatesTaskAndOperationNetQuantities() {
        SceneWorkReportEntity source = sourceReport();
        SceneProductionTaskEntity task = task();
        task.setInputQuantity(10);
        task.setGoodQuantity(8);
        task.setDefectQuantity(2);
        task.setReworkQuantity(1);
        SceneDispatchDetailEntity detail = detail();
        detail.setGoodQuantity(8);
        detail.setDefectQuantity(2);
        when(reportRepository.findByRequestNoAndDeletedFalse("REV-1")).thenReturn(Optional.empty());
        when(reportRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(source));
        when(reportRepository.existsBySourceReportIdAndDeletedFalse(20L)).thenReturn(false);
        when(taskRepository.findByIdAndDeletedFalseForUpdate(1L)).thenReturn(Optional.of(task));
        when(detailRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(detail));
        when(reportRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            SceneWorkReportEntity report = invocation.getArgument(0);
            report.setId(21L);
            return report;
        });

        SceneWorkReportReverseReqVO reqVO = new SceneWorkReportReverseReqVO();
        reqVO.setRequestNo("REV-1");
        reqVO.setReason("录入错误");

        assertThat(service.reverse(20L, reqVO)).isEqualTo(21L);
        assertThat(task.getInputQuantity()).isZero();
        assertThat(task.getGoodQuantity()).isZero();
        assertThat(detail.getDefectQuantity()).isZero();
    }

    @Test
    void alreadyReversedReportIsRejected() {
        SceneWorkReportEntity source = sourceReport();
        when(reportRepository.findByRequestNoAndDeletedFalse("REV-1")).thenReturn(Optional.empty());
        when(reportRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(source));
        when(reportRepository.existsBySourceReportIdAndDeletedFalse(20L)).thenReturn(true);
        SceneWorkReportReverseReqVO reqVO = new SceneWorkReportReverseReqVO();
        reqVO.setRequestNo("REV-1");

        assertThatThrownBy(() -> service.reverse(20L, reqVO))
                .isInstanceOfSatisfying(ServiceException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(SceneErrorCodeConstants.REPORT_ALREADY_REVERSED));
    }

    private void stubSubmitBase(String mustScanValue) {
        stubSubmitBase(mustScanValue, task(), detail());
    }

    private void stubSubmitBase(String mustScanValue, SceneProductionTaskEntity task,
                                SceneDispatchDetailEntity detail) {
        SceneProductionParameterRespVO parameter = new SceneProductionParameterRespVO();
        parameter.setParamValue(mustScanValue);
        when(reportRepository.findByRequestNoAndDeletedFalse("REQ-1")).thenReturn(Optional.empty());
        when(detailRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(detail));
        when(taskRepository.findByIdAndDeletedFalseForUpdate(1L)).thenReturn(Optional.of(task));
        when(reportRepository.findByTaskIdAndDeletedFalse(1L)).thenReturn(List.of());
        when(parameterService.getEffectiveParameter(any())).thenReturn(parameter);
    }

    private SceneWorkReportSubmitReqVO request() {
        SceneWorkReportSubmitReqVO reqVO = new SceneWorkReportSubmitReqVO();
        reqVO.setRequestNo("REQ-1");
        reqVO.setDispatchDetailId(2L);
        reqVO.setInputQuantity(10);
        reqVO.setGoodQuantity(8);
        reqVO.setDefectQuantity(2);
        reqVO.setReworkQuantity(1);
        reqVO.setReportTime(LocalDateTime.now());
        return reqVO;
    }

    private SceneProductionTaskEntity task() {
        SceneProductionTaskEntity task = new SceneProductionTaskEntity();
        task.setId(1L);
        task.setProductId(11L);
        task.setBatchNo("BATCH-1");
        task.setWorkshopId(4L);
        task.setLineId(5L);
        task.setTaskStatus(3);
        task.setPlanQuantity(100);
        task.setInputQuantity(0);
        task.setGoodQuantity(0);
        task.setDefectQuantity(0);
        task.setReworkQuantity(0);
        return task;
    }

    private SceneDispatchDetailEntity detail() {
        SceneDispatchDetailEntity detail = new SceneDispatchDetailEntity();
        detail.setId(2L);
        detail.setTaskId(1L);
        detail.setProcessId(3L);
        detail.setEquipmentId(6L);
        detail.setDetailStatus(1);
        detail.setPaused(false);
        detail.setGoodQuantity(0);
        detail.setDefectQuantity(0);
        return detail;
    }

    private SceneWorkReportEntity sourceReport() {
        SceneWorkReportEntity report = new SceneWorkReportEntity();
        report.setId(20L);
        report.setTaskId(1L);
        report.setDispatchDetailId(2L);
        report.setProcessId(3L);
        report.setRecordType(1);
        report.setReportType(1);
        report.setSourceType(1);
        report.setInputQuantity(10);
        report.setGoodQuantity(8);
        report.setDefectQuantity(2);
        report.setReworkQuantity(1);
        return report;
    }
}
