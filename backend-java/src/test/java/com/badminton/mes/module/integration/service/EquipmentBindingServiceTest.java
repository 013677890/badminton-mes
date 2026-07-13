package com.badminton.mes.module.integration.service;

import java.util.Optional;

import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.integration.controller.vo.EquipmentBindingSaveReqVO;
import com.badminton.mes.module.integration.dal.repository.EquipmentBindingRepository;
import com.badminton.mes.module.production.dal.entity.ProductionLineEntity;
import com.badminton.mes.module.production.dal.repository.ProductionLineRepository;
import com.badminton.mes.module.craft.dal.repository.CraftProcessRepository;
import com.badminton.mes.module.system.dal.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * {@link EquipmentBindingService} 单元测试。
 *
 * @author Codex
 * @date 2026/07/13
 */
@ExtendWith(MockitoExtension.class)
class EquipmentBindingServiceTest {

    @Mock private EquipmentBindingRepository bindingRepository;
    @Mock private ProductionLineRepository lineRepository;
    @Mock private CraftProcessRepository processRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private EquipmentBindingService service;

    @Test
    void autoReportRequiresEnabledDefaultEmployee() {
        ProductionLineEntity line = new ProductionLineEntity();
        line.setId(1L);
        when(lineRepository.findByIdAndStatusAndDeletedFalse(
                1L, CommonStatusEnum.ENABLED.getStatus())).thenReturn(Optional.of(line));
        EquipmentBindingSaveReqVO request = new EquipmentBindingSaveReqVO();
        request.setEquipmentCode("EQ001");
        request.setLineId(1L);
        request.setAutoReport(true);
        request.setMaxIncrement(100L);
        request.setStatus(1);

        assertThatThrownBy(() -> service.saveBinding(request))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("默认员工");
    }
}
