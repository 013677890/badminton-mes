package com.badminton.mes.module.production.service.impl;

import java.util.Optional;

import com.badminton.mes.module.production.dal.entity.ProductionLineEntity;
import com.badminton.mes.module.production.dal.entity.WorkshopEntity;
import com.badminton.mes.module.production.dal.repository.ProductionLineRepository;
import com.badminton.mes.module.production.dal.repository.WorkshopRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ProductionOrganizationReferenceQueryImpl} 单元测试。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
@ExtendWith(MockitoExtension.class)
class ProductionOrganizationReferenceQueryImplTest {

    @Mock
    private WorkshopRepository workshopRepository;

    @Mock
    private ProductionLineRepository productionLineRepository;

    @Test
    @DisplayName("组织归属：车间与产线为空时允许后台用户不绑定范围")
    void emptyAssignmentIsValid() {
        var service = new ProductionOrganizationReferenceQueryImpl(
                workshopRepository, productionLineRepository);

        assertThat(service.lockAndCheckAssignment(null, null)).isTrue();
        verify(workshopRepository, never()).findByIdAndDeletedFalseForUpdate(10L);
    }

    @Test
    @DisplayName("组织归属：产线不属于所选车间时拒绝")
    void mismatchedProductionLineIsInvalid() {
        WorkshopEntity workshop = new WorkshopEntity();
        workshop.setId(10L);
        workshop.setStatus(1);
        ProductionLineEntity line = new ProductionLineEntity();
        line.setId(20L);
        line.setWorkshopId(99L);
        line.setStatus(1);
        when(workshopRepository.findByIdAndDeletedFalseForUpdate(10L))
                .thenReturn(Optional.of(workshop));
        when(productionLineRepository.findByIdAndDeletedFalseForUpdate(20L))
                .thenReturn(Optional.of(line));
        var service = new ProductionOrganizationReferenceQueryImpl(
                workshopRepository, productionLineRepository);

        assertThat(service.lockAndCheckAssignment(10L, 20L)).isFalse();
    }

    @Test
    @DisplayName("组织归属：车间不存在时拒绝")
    void missingWorkshopIsInvalid() {
        when(workshopRepository.findByIdAndDeletedFalseForUpdate(10L))
                .thenReturn(Optional.empty());
        var service = new ProductionOrganizationReferenceQueryImpl(
                workshopRepository, productionLineRepository);

        assertThat(service.lockAndCheckAssignment(10L, null)).isFalse();
    }

    @Test
    @DisplayName("组织归属：车间已停用时拒绝")
    void disabledWorkshopIsInvalid() {
        WorkshopEntity workshop = new WorkshopEntity();
        workshop.setId(10L);
        workshop.setStatus(0);
        when(workshopRepository.findByIdAndDeletedFalseForUpdate(10L))
                .thenReturn(Optional.of(workshop));
        var service = new ProductionOrganizationReferenceQueryImpl(
                workshopRepository, productionLineRepository);

        assertThat(service.lockAndCheckAssignment(10L, null)).isFalse();
    }

    @Test
    @DisplayName("组织归属：启用车间且不指定产线时允许")
    void enabledWorkshopWithoutLineIsValid() {
        WorkshopEntity workshop = new WorkshopEntity();
        workshop.setId(10L);
        workshop.setStatus(1);
        when(workshopRepository.findByIdAndDeletedFalseForUpdate(10L))
                .thenReturn(Optional.of(workshop));
        var service = new ProductionOrganizationReferenceQueryImpl(
                workshopRepository, productionLineRepository);

        assertThat(service.lockAndCheckAssignment(10L, null)).isTrue();
    }

    @Test
    @DisplayName("组织归属：启用车间与启用产线归属一致时允许")
    void matchedEnabledLineIsValid() {
        WorkshopEntity workshop = new WorkshopEntity();
        workshop.setId(10L);
        workshop.setStatus(1);
        ProductionLineEntity line = new ProductionLineEntity();
        line.setId(20L);
        line.setWorkshopId(10L);
        line.setStatus(1);
        when(workshopRepository.findByIdAndDeletedFalseForUpdate(10L))
                .thenReturn(Optional.of(workshop));
        when(productionLineRepository.findByIdAndDeletedFalseForUpdate(20L))
                .thenReturn(Optional.of(line));
        var service = new ProductionOrganizationReferenceQueryImpl(
                workshopRepository, productionLineRepository);

        assertThat(service.lockAndCheckAssignment(10L, 20L)).isTrue();
    }

    @Test
    @DisplayName("组织归属：产线不存在时拒绝")
    void missingLineIsInvalid() {
        WorkshopEntity workshop = new WorkshopEntity();
        workshop.setId(10L);
        workshop.setStatus(1);
        when(workshopRepository.findByIdAndDeletedFalseForUpdate(10L))
                .thenReturn(Optional.of(workshop));
        when(productionLineRepository.findByIdAndDeletedFalseForUpdate(20L))
                .thenReturn(Optional.empty());
        var service = new ProductionOrganizationReferenceQueryImpl(
                workshopRepository, productionLineRepository);

        assertThat(service.lockAndCheckAssignment(10L, 20L)).isFalse();
    }

    @Test
    @DisplayName("组织归属：产线已停用时拒绝")
    void disabledLineIsInvalid() {
        WorkshopEntity workshop = new WorkshopEntity();
        workshop.setId(10L);
        workshop.setStatus(1);
        ProductionLineEntity line = new ProductionLineEntity();
        line.setId(20L);
        line.setWorkshopId(10L);
        line.setStatus(0);
        when(workshopRepository.findByIdAndDeletedFalseForUpdate(10L))
                .thenReturn(Optional.of(workshop));
        when(productionLineRepository.findByIdAndDeletedFalseForUpdate(20L))
                .thenReturn(Optional.of(line));
        var service = new ProductionOrganizationReferenceQueryImpl(
                workshopRepository, productionLineRepository);

        assertThat(service.lockAndCheckAssignment(10L, 20L)).isFalse();
    }
}
