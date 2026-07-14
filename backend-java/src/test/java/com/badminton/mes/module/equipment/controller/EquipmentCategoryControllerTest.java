package com.badminton.mes.module.equipment.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.module.equipment.controller.vo.EquipmentCategoryRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentCategorySaveReqVO;
import com.badminton.mes.module.equipment.service.EquipmentCategoryService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** EquipmentCategoryController 的轻量委托单元测试。 */
@ExtendWith(MockitoExtension.class)
class EquipmentCategoryControllerTest {

    @Mock
    private EquipmentCategoryService categoryService;

    @InjectMocks
    private EquipmentCategoryController controller;

    @Test
    void createDelegatesToCurrentServiceContract() {
        EquipmentCategorySaveReqVO request = new EquipmentCategorySaveReqVO();
        request.setCategoryCode("MACHINE");
        request.setCategoryName("生产设备");
        when(categoryService.createEquipmentCategory(request)).thenReturn(21L);

        CommonResult<Long> result = controller.createEquipmentCategory(request);

        assertThat(result.getCode()).isEqualTo("00000");
        assertThat(result.getData()).isEqualTo(21L);
        verify(categoryService).createEquipmentCategory(request);
    }

    @Test
    void getReturnsServiceResponse() {
        EquipmentCategoryRespVO response = new EquipmentCategoryRespVO();
        response.setId(21L);
        response.setCategoryCode("MACHINE");
        when(categoryService.getEquipmentCategory(21L)).thenReturn(response);

        assertThat(controller.getEquipmentCategory(21L).getData()).isSameAs(response);
    }
}
