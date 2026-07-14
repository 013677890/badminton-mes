package com.badminton.mes.module.equipment.service.impl;

import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.equipment.constants.EquipmentErrorCodeConstants;
import com.badminton.mes.module.equipment.controller.vo.EquipmentManufacturerSaveReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentManufacturerEntity;
import com.badminton.mes.module.equipment.dal.repository.EquipmentLedgerRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentManufacturerRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** EquipmentManufacturerServiceImpl 当前领域契约的单元测试。 */
@ExtendWith(MockitoExtension.class)
class EquipmentManufacturerServiceImplTest {

    @Mock
    private EquipmentManufacturerRepository manufacturerRepository;
    @Mock
    private EquipmentLedgerRepository ledgerRepository;

    private EquipmentManufacturerServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EquipmentManufacturerServiceImpl(manufacturerRepository, ledgerRepository);
    }

    @Test
    void createSetsDefaultsAndReturnsGeneratedId() {
        when(manufacturerRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            EquipmentManufacturerEntity entity = invocation.getArgument(0);
            entity.setId(31L);
            return entity;
        });

        assertThat(service.createEquipmentManufacturer(request())).isEqualTo(31L);

        ArgumentCaptor<EquipmentManufacturerEntity> captor =
                ArgumentCaptor.forClass(EquipmentManufacturerEntity.class);
        verify(manufacturerRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getManufacturerCode()).isEqualTo("MFR-001");
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
        assertThat(captor.getValue().getCreateBy()).isEqualTo(1L);
    }

    @Test
    void createRejectsDuplicateCodeBeforeWriting() {
        when(manufacturerRepository.existsByManufacturerCodeAndDeletedFalse("MFR-001"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createEquipmentManufacturer(request()))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                EquipmentErrorCodeConstants.EQUIPMENT_MANUFACTURER_CODE_DUPLICATE));
        verify(manufacturerRepository, never()).saveAndFlush(any());
    }

    @Test
    void deleteRejectsManufacturerReferencedByLedger() {
        EquipmentManufacturerEntity entity = new EquipmentManufacturerEntity();
        entity.setId(31L);
        entity.setManufacturerCode("MFR-001");
        when(manufacturerRepository.findByIdAndDeletedFalse(31L)).thenReturn(Optional.of(entity));
        when(ledgerRepository.countByManufacturerIdAndDeletedFalse(31L)).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteEquipmentManufacturer(31L))
                .isInstanceOf(ServiceException.class);
        verify(manufacturerRepository, never()).save(any());
    }

    private static EquipmentManufacturerSaveReqVO request() {
        EquipmentManufacturerSaveReqVO request = new EquipmentManufacturerSaveReqVO();
        request.setManufacturerCode("MFR-001");
        request.setManufacturerName("测试制造商");
        return request;
    }
}
