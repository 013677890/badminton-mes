package com.badminton.mes.module.equipment.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.module.equipment.controller.vo.EquipmentManufacturerRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentManufacturerSaveReqVO;
import com.badminton.mes.module.equipment.service.EquipmentManufacturerService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** EquipmentManufacturerController 的轻量委托单元测试。 */
@ExtendWith(MockitoExtension.class)
class EquipmentManufacturerControllerTest {

    @Mock
    private EquipmentManufacturerService manufacturerService;

    @InjectMocks
    private EquipmentManufacturerController controller;

    @Test
    void createDelegatesToCurrentServiceContract() {
        EquipmentManufacturerSaveReqVO request = request();
        when(manufacturerService.createEquipmentManufacturer(request)).thenReturn(31L);

        CommonResult<Long> result = controller.createEquipmentManufacturer(request);

        assertThat(result.getCode()).isEqualTo("00000");
        assertThat(result.getData()).isEqualTo(31L);
        verify(manufacturerService).createEquipmentManufacturer(request);
    }

    @Test
    void getReturnsServiceResponse() {
        EquipmentManufacturerRespVO response = new EquipmentManufacturerRespVO();
        response.setId(31L);
        response.setManufacturerCode("MFR-001");
        when(manufacturerService.getEquipmentManufacturer(31L)).thenReturn(response);

        assertThat(controller.getEquipmentManufacturer(31L).getData()).isSameAs(response);
    }

    private static EquipmentManufacturerSaveReqVO request() {
        EquipmentManufacturerSaveReqVO request = new EquipmentManufacturerSaveReqVO();
        request.setManufacturerCode("MFR-001");
        request.setManufacturerName("测试制造商");
        return request;
    }
}
