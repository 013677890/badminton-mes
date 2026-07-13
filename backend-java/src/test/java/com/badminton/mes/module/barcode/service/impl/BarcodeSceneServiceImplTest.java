package com.badminton.mes.module.barcode.service.impl;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.barcode.dal.entity.BarcodeEntity;
import com.badminton.mes.module.barcode.dal.entity.BarcodeUseRecordEntity;
import com.badminton.mes.module.barcode.dal.repository.BarcodeRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeUseRecordRepository;
import com.badminton.mes.module.barcode.enums.BarcodeStatusEnum;
import com.badminton.mes.module.barcode.service.BarcodeSceneService.BarcodeSceneSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.badminton.mes.module.barcode.constants.BarcodeErrorCodeConstants.BARCODE_NOT_EXISTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M1 条码事实与 M2 现场扫码之间稳定契约的单元测试。
 *
 * @author 范家权
 */
@ExtendWith(MockitoExtension.class)
class BarcodeSceneServiceImplTest {

    @Mock
    private BarcodeRepository barcodeRepository;

    @Mock
    private BarcodeUseRecordRepository useRecordRepository;

    private BarcodeSceneServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BarcodeSceneServiceImpl(barcodeRepository, useRecordRepository);
    }

    @Test
    void shouldMarkUnusedBarcodeAndPersistCompleteUseRecord() {
        BarcodeEntity barcode = barcode(11L, BarcodeStatusEnum.UNUSED, 21L, "BATCH-001", 31L);
        when(barcodeRepository.findByBarcodeValueAndDeletedFalse("BC-001"))
                .thenReturn(Optional.of(barcode));
        when(barcodeRepository.updateStatus(11L,
                BarcodeStatusEnum.UNUSED.getStatus(), BarcodeStatusEnum.USED.getStatus()))
                .thenReturn(1);

        BarcodeSceneSnapshot snapshot = service.validateAndRecordUse(
                "BC-001", 31L, 21L, "BATCH-001", 41L, 51L, 61L, 1);

        assertThat(snapshot.barcodeId()).isEqualTo(11L);
        assertThat(snapshot.productId()).isEqualTo(21L);
        assertThat(snapshot.batchNo()).isEqualTo("BATCH-001");
        assertThat(snapshot.taskId()).isEqualTo(31L);

        ArgumentCaptor<BarcodeUseRecordEntity> recordCaptor =
                ArgumentCaptor.forClass(BarcodeUseRecordEntity.class);
        verify(useRecordRepository).save(recordCaptor.capture());
        BarcodeUseRecordEntity record = recordCaptor.getValue();
        assertThat(record.getBarcodeId()).isEqualTo(11L);
        assertThat(record.getTaskId()).isEqualTo(31L);
        assertThat(record.getProcessId()).isEqualTo(41L);
        assertThat(record.getUserId()).isEqualTo(51L);
        assertThat(record.getEquipmentId()).isEqualTo(61L);
        assertThat(record.getUseType()).isEqualTo(1);
        assertThat(record.getBusinessTime()).isNotNull();
    }

    @Test
    void shouldKeepUsedStatusButAppendAnotherTraceRecord() {
        BarcodeEntity barcode = barcode(11L, BarcodeStatusEnum.USED, 21L, "BATCH-001", 31L);
        when(barcodeRepository.findByBarcodeValueAndDeletedFalse("BC-001"))
                .thenReturn(Optional.of(barcode));

        service.validateAndRecordUse("BC-001", 31L, 21L, "BATCH-001", 41L, 51L, null, 4);

        verify(barcodeRepository, never()).updateStatus(11L,
                BarcodeStatusEnum.UNUSED.getStatus(), BarcodeStatusEnum.USED.getStatus());
        verify(useRecordRepository).save(org.mockito.ArgumentMatchers.any(BarcodeUseRecordEntity.class));
    }

    @Test
    void shouldRejectCancelledBarcodeWithoutWritingTrace() {
        BarcodeEntity barcode = barcode(11L, BarcodeStatusEnum.CANCELLED, 21L, "BATCH-001", 31L);
        when(barcodeRepository.findByBarcodeValueAndDeletedFalse("BC-001"))
                .thenReturn(Optional.of(barcode));

        assertBarcodeRejected("BC-001", 31L, 21L, "BATCH-001");
    }

    @Test
    void shouldRejectBarcodeBoundToAnotherProduct() {
        BarcodeEntity barcode = barcode(11L, BarcodeStatusEnum.UNUSED, 999L, "BATCH-001", 31L);
        when(barcodeRepository.findByBarcodeValueAndDeletedFalse("BC-001"))
                .thenReturn(Optional.of(barcode));

        assertBarcodeRejected("BC-001", 31L, 21L, "BATCH-001");
    }

    @Test
    void shouldRejectBarcodeBoundToAnotherBatch() {
        BarcodeEntity barcode = barcode(11L, BarcodeStatusEnum.UNUSED, 21L, "BATCH-999", 31L);
        when(barcodeRepository.findByBarcodeValueAndDeletedFalse("BC-001"))
                .thenReturn(Optional.of(barcode));

        assertBarcodeRejected("BC-001", 31L, 21L, "BATCH-001");
    }

    @Test
    void shouldRejectBarcodeBoundToAnotherTask() {
        BarcodeEntity barcode = barcode(11L, BarcodeStatusEnum.UNUSED, 21L, "BATCH-001", 999L);
        when(barcodeRepository.findByBarcodeValueAndDeletedFalse("BC-001"))
                .thenReturn(Optional.of(barcode));

        assertBarcodeRejected("BC-001", 31L, 21L, "BATCH-001");
    }

    @Test
    void shouldRejectUnknownBarcodeWithoutWritingTrace() {
        when(barcodeRepository.findByBarcodeValueAndDeletedFalse("UNKNOWN"))
                .thenReturn(Optional.empty());

        assertBarcodeRejected("UNKNOWN", 31L, 21L, "BATCH-001");
    }

    private void assertBarcodeRejected(String value, Long taskId, Long productId, String batchNo) {
        assertThatThrownBy(() -> service.validateAndRecordUse(
                value, taskId, productId, batchNo, 41L, 51L, 61L, 1))
                .isInstanceOf(ServiceException.class)
                .extracting(exception -> ((ServiceException) exception).getErrorCode())
                .isEqualTo(BARCODE_NOT_EXISTS);
        verify(useRecordRepository, never()).save(
                org.mockito.ArgumentMatchers.any(BarcodeUseRecordEntity.class));
    }

    private static BarcodeEntity barcode(Long id, BarcodeStatusEnum status, Long productId,
                                         String batchNo, Long taskId) {
        BarcodeEntity barcode = new BarcodeEntity();
        barcode.setId(id);
        barcode.setBarcodeStatus(status.getStatus());
        barcode.setProductId(productId);
        barcode.setBatchNo(batchNo);
        barcode.setTaskId(taskId);
        return barcode;
    }
}
