package com.badminton.mes.module.production.service.impl;

import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.integration.service.UnitService;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.controller.vo.MaterialUpdateReqVO;
import com.badminton.mes.module.production.dal.entity.MaterialEntity;
import com.badminton.mes.module.production.dal.repository.BomDetailRepository;
import com.badminton.mes.module.production.dal.repository.KitAnalysisRepository;
import com.badminton.mes.module.production.dal.repository.MaterialRepository;
import com.badminton.mes.module.production.dal.repository.MaterialStockRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderMaterialRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MaterialServiceImpl} 计量单位引用口径测试。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
@ExtendWith(MockitoExtension.class)
class MaterialServiceImplTest {

    private static final Long MATERIAL_ID = 10L;

    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private UnitService unitService;
    @Mock
    private BomDetailRepository bomDetailRepository;
    @Mock
    private WorkOrderMaterialRepository workOrderMaterialRepository;
    @Mock
    private MaterialStockRepository materialStockRepository;
    @Mock
    private KitAnalysisRepository kitAnalysisRepository;

    private MaterialServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MaterialServiceImpl(materialRepository, unitService,
                bomDetailRepository, workOrderMaterialRepository,
                materialStockRepository, kitAnalysisRepository);
    }

    @Test
    @DisplayName("修改物料单位：仅存在库存快照时也拒绝变更")
    void updateMaterialRejectsUnitChangeWhenStockExists() {
        stubEditableMaterial();
        when(materialStockRepository.existsByMaterialIdAndDeletedFalse(MATERIAL_ID))
                .thenReturn(true);

        assertUnitImmutable(buildUpdateRequest());

        verify(kitAnalysisRepository, never()).existsByMaterialIdAndDeletedFalse(any());
        verify(materialRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("修改物料单位：仅存在齐套分析记录时也拒绝变更")
    void updateMaterialRejectsUnitChangeWhenKitAnalysisExists() {
        stubEditableMaterial();
        when(kitAnalysisRepository.existsByMaterialIdAndDeletedFalse(MATERIAL_ID))
                .thenReturn(true);

        assertUnitImmutable(buildUpdateRequest());

        verify(materialRepository, never()).saveAndFlush(any());
    }

    /** 准备可编辑物料与启用的新计量单位。 */
    private void stubEditableMaterial() {
        MaterialEntity material = new MaterialEntity();
        material.setId(MATERIAL_ID);
        material.setMaterialCode("M001");
        material.setUnitId(1L);
        material.setStatus(1);
        material.setVersion(0);
        when(materialRepository.findByIdAndDeletedFalseForUpdate(MATERIAL_ID))
                .thenReturn(Optional.of(material));
        when(unitService.lockAndCheckEnabled(2L)).thenReturn(true);
    }

    /** 断言单位不可变错误。 */
    private void assertUnitImmutable(MaterialUpdateReqVO request) {
        assertThatThrownBy(() -> service.updateMaterial(MATERIAL_ID, request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                ProductionErrorCodeConstants.MATERIAL_UNIT_IMMUTABLE));
    }

    /** 构造修改单位请求。 */
    private MaterialUpdateReqVO buildUpdateRequest() {
        MaterialUpdateReqVO request = new MaterialUpdateReqVO();
        request.setMaterialCode("M001");
        request.setMaterialName("鹅毛羽片");
        request.setMaterialType(2);
        request.setUnitId(2L);
        request.setKeyMaterial(true);
        request.setStatus(1);
        request.setVersion(0);
        return request;
    }
}
