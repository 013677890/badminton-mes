package com.badminton.mes.module.integration.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.integration.controller.vo.MaterialStockBatchReqVO;
import com.badminton.mes.module.integration.controller.vo.MaterialStockItemReqVO;
import com.badminton.mes.module.production.dal.entity.MaterialEntity;
import com.badminton.mes.module.production.dal.repository.MaterialRepository;
import com.badminton.mes.module.production.dal.repository.MaterialStockRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderMaterialRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.production.service.KitAnalysisService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MaterialStockSyncService} 单元测试。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@ExtendWith(MockitoExtension.class)
class MaterialStockSyncServiceTest {

    @Mock private MaterialRepository materialRepository;
    @Mock private MaterialStockRepository stockRepository;
    @Mock private WorkOrderMaterialRepository workOrderMaterialRepository;
    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private KitAnalysisService kitAnalysisService;
    @InjectMocks private MaterialStockSyncService service;

    @Test
    void syncUpsertsWmsAndTransitSnapshot() {
        MaterialEntity material = new MaterialEntity();
        material.setId(10L);
        when(materialRepository.findByMaterialCodeAndDeletedFalse("M001"))
                .thenReturn(Optional.of(material));
        MaterialStockItemReqVO item = new MaterialStockItemReqVO();
        item.setMaterialCode("m001");
        item.setAvailableQuantity(new BigDecimal("100"));
        item.setLockedQuantity(new BigDecimal("10"));
        item.setCheckingQuantity(new BigDecimal("5"));
        item.setTransitQuantity(new BigDecimal("50"));
        item.setSyncTime(LocalDateTime.of(2026, 7, 13, 10, 0));
        MaterialStockBatchReqVO request = new MaterialStockBatchReqVO();
        request.setSourceSystem("wms");
        request.setReanalyze(false);
        request.setItems(List.of(item));
        when(stockRepository.upsertIfNewer(
                10L, "WMS", item.getAvailableQuantity(), item.getLockedQuantity(),
                item.getCheckingQuantity(), item.getTransitQuantity(), item.getSyncTime()))
                .thenReturn(1);

        int count = service.sync(request);

        assertThat(count).isEqualTo(1);
        verify(stockRepository).upsertIfNewer(
                10L, "WMS", new BigDecimal("100"), new BigDecimal("10"),
                new BigDecimal("5"), new BigDecimal("50"), item.getSyncTime());
    }

    @Test
    void syncIgnoresOlderSnapshot() {
        MaterialEntity material = new MaterialEntity();
        material.setId(10L);
        when(materialRepository.findByMaterialCodeAndDeletedFalse("M001"))
                .thenReturn(Optional.of(material));
        MaterialStockItemReqVO item = new MaterialStockItemReqVO();
        item.setMaterialCode("M001");
        item.setAvailableQuantity(BigDecimal.ONE);
        item.setLockedQuantity(BigDecimal.ZERO);
        item.setCheckingQuantity(BigDecimal.ZERO);
        item.setTransitQuantity(BigDecimal.ZERO);
        item.setSyncTime(LocalDateTime.of(2026, 7, 13, 9, 0));
        MaterialStockBatchReqVO request = new MaterialStockBatchReqVO();
        request.setSourceSystem("WMS");
        request.setReanalyze(true);
        request.setItems(List.of(item));
        when(stockRepository.upsertIfNewer(
                10L, "WMS", BigDecimal.ONE, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, item.getSyncTime())).thenReturn(0);

        int count = service.sync(request);

        assertThat(count).isZero();
        verify(workOrderMaterialRepository, org.mockito.Mockito.never())
                .findDistinctWorkOrderIdsByMaterialIdIn(org.mockito.ArgumentMatchers.any());
    }
}
