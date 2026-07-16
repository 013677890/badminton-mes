package com.badminton.mes.module.barcode.service.impl;

import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.barcode.constants.BarcodeErrorCodeConstants;
import com.badminton.mes.module.barcode.dal.entity.BarcodeEntity;
import com.badminton.mes.module.barcode.dal.entity.BarcodeUseRecordEntity;
import com.badminton.mes.module.barcode.dal.repository.BarcodeRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeUseRecordRepository;
import com.badminton.mes.module.barcode.enums.BarcodeStatusEnum;
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
 * scene 复用条码校验和使用记录测试。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
class BarcodeSceneServiceImplTest {

    private final BarcodeRepository barcodeRepository = mock(BarcodeRepository.class);
    private final BarcodeUseRecordRepository useRecordRepository = mock(BarcodeUseRecordRepository.class);
    private final BarcodeSceneServiceImpl service =
            new BarcodeSceneServiceImpl(barcodeRepository, useRecordRepository);

    @Test
    void validReportBarcodeIsMarkedUsedAndRecorded() {
        BarcodeEntity barcode = barcode(BarcodeStatusEnum.UNUSED.getStatus());
        when(barcodeRepository.findByBarcodeValueAndDeletedFalse("BC-001"))
                .thenReturn(Optional.of(barcode));

        var snapshot = service.validateAndRecordUse("BC-001", 10L, 20L, "BATCH-1",
                30L, 40L, 50L, 3);

        assertThat(snapshot.barcodeId()).isEqualTo(1L);
        verify(barcodeRepository).updateStatus(1L, BarcodeStatusEnum.UNUSED.getStatus(),
                BarcodeStatusEnum.USED.getStatus());
        ArgumentCaptor<BarcodeUseRecordEntity> captor = ArgumentCaptor.forClass(BarcodeUseRecordEntity.class);
        verify(useRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getUseType()).isEqualTo(3);
        assertThat(captor.getValue().getTaskId()).isEqualTo(10L);
        assertThat(captor.getValue().getProcessId()).isEqualTo(30L);
    }

    @Test
    void cancelledBarcodeIsRejectedWithoutUseRecord() {
        when(barcodeRepository.findByBarcodeValueAndDeletedFalse("BC-001"))
                .thenReturn(Optional.of(barcode(BarcodeStatusEnum.CANCELLED.getStatus())));

        assertThatThrownBy(() -> service.validateAndRecordUse("BC-001", 10L, 20L,
                "BATCH-1", 30L, 40L, 50L, 3))
                .isInstanceOfSatisfying(ServiceException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_NOT_EXISTS));
        verify(useRecordRepository, never()).save(any());
    }

    @Test
    void wrongTaskBarcodeIsRejectedWithoutUseRecord() {
        BarcodeEntity barcode = barcode(BarcodeStatusEnum.UNUSED.getStatus());
        barcode.setTaskId(99L);
        when(barcodeRepository.findByBarcodeValueAndDeletedFalse("BC-001"))
                .thenReturn(Optional.of(barcode));

        assertThatThrownBy(() -> service.validateAndRecordUse("BC-001", 10L, 20L,
                "BATCH-1", 30L, 40L, 50L, 3))
                .isInstanceOf(ServiceException.class);
        verify(useRecordRepository, never()).save(any());
    }

    private BarcodeEntity barcode(Integer status) {
        BarcodeEntity barcode = new BarcodeEntity();
        barcode.setId(1L);
        barcode.setProductId(20L);
        barcode.setBatchNo("BATCH-1");
        barcode.setTaskId(10L);
        barcode.setBarcodeStatus(status);
        return barcode;
    }
}
