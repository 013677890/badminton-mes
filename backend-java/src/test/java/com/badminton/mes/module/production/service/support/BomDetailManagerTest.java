package com.badminton.mes.module.production.service.support;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.LongStream;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.controller.vo.BomDetailSaveReqVO;
import com.badminton.mes.module.production.dal.entity.MaterialEntity;
import com.badminton.mes.module.production.dal.repository.BomDetailRepository;
import com.badminton.mes.module.production.dal.repository.MaterialRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link BomDetailManager} 明细边界与物料锁测试。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
@ExtendWith(MockitoExtension.class)
class BomDetailManagerTest {

    @Mock
    private BomDetailRepository bomDetailRepository;
    @Mock
    private MaterialRepository materialRepository;

    private BomDetailManager manager;

    @BeforeEach
    void setUp() {
        manager = new BomDetailManager(bomDetailRepository, materialRepository);
    }

    @Test
    @DisplayName("明细校验：空集合与超过 200 条均拒绝")
    void validateAndLockRejectsEmptyAndOversizedDetails() {
        assertDetailInvalid(() -> manager.validateAndLock(List.of()));
        List<BomDetailSaveReqVO> oversized = LongStream.rangeClosed(1, 201)
                .mapToObj(this::validDetail)
                .toList();
        assertDetailInvalid(() -> manager.validateAndLock(oversized));
        verify(materialRepository, never()).findAllByIdInForUpdateOrderByIdAsc(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("明细校验：同一物料重复时拒绝")
    void validateAndLockRejectsDuplicateMaterial() {
        assertDetailInvalid(() -> manager.validateAndLock(
                List.of(validDetail(1L), validDetail(1L))));
    }

    @Test
    @DisplayName("明细校验：标准用量非正或损耗率越界时拒绝")
    void validateAndLockRejectsInvalidQuantityAndLossRate() {
        BomDetailSaveReqVO zeroQuantity = validDetail(1L);
        zeroQuantity.setQuantity(BigDecimal.ZERO);
        assertDetailInvalid(() -> manager.validateAndLock(List.of(zeroQuantity)));

        BomDetailSaveReqVO excessiveLoss = validDetail(2L);
        excessiveLoss.setLossRate(new BigDecimal("100.01"));
        assertDetailInvalid(() -> manager.validateAndLock(List.of(excessiveLoss)));
    }

    @Test
    @DisplayName("物料锁：按主键升序锁定全部启用物料")
    void validateAndLockUsesAscendingMaterialOrder() {
        List<Long> sortedIds = List.of(1L, 2L, 3L);
        when(materialRepository.findAllByIdInForUpdateOrderByIdAsc(sortedIds))
                .thenReturn(sortedIds.stream().map(id -> material(id, 1)).toList());

        manager.validateAndLock(List.of(validDetail(3L), validDetail(1L), validDetail(2L)));

        verify(materialRepository).findAllByIdInForUpdateOrderByIdAsc(sortedIds);
    }

    @Test
    @DisplayName("物料锁：物料缺失或停用时返回物料不可用")
    void validateAndLockRejectsMissingOrDisabledMaterial() {
        when(materialRepository.findAllByIdInForUpdateOrderByIdAsc(List.of(1L, 2L)))
                .thenReturn(List.of(material(1L, 1)));

        assertThatThrownBy(() -> manager.validateAndLock(
                List.of(validDetail(1L), validDetail(2L))))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                ProductionErrorCodeConstants.BOM_MATERIAL_NOT_AVAILABLE));

        when(materialRepository.findAllByIdInForUpdateOrderByIdAsc(List.of(3L)))
                .thenReturn(List.of(material(3L, 0)));
        assertThatThrownBy(() -> manager.validateAndLock(List.of(validDetail(3L))))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                ProductionErrorCodeConstants.BOM_MATERIAL_NOT_AVAILABLE));
    }

    /** 断言明细业务校验失败。 */
    private void assertDetailInvalid(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                ProductionErrorCodeConstants.BOM_DETAIL_INVALID));
    }

    /** 构造合法保存明细。 */
    private BomDetailSaveReqVO validDetail(long materialId) {
        BomDetailSaveReqVO detail = new BomDetailSaveReqVO();
        detail.setMaterialId(materialId);
        detail.setQuantity(BigDecimal.ONE);
        detail.setLossRate(BigDecimal.ZERO);
        return detail;
    }

    /** 构造物料实体。 */
    private MaterialEntity material(Long id, Integer status) {
        MaterialEntity material = new MaterialEntity();
        material.setId(id);
        material.setStatus(status);
        return material;
    }
}
