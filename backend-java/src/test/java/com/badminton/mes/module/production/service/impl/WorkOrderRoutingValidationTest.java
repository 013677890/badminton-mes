package com.badminton.mes.module.production.service.impl;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.craft.dal.entity.CraftRouteEntity;
import com.badminton.mes.module.craft.dal.repository.CraftRouteProductRepository;
import com.badminton.mes.module.craft.dal.repository.CraftRouteRepository;
import com.badminton.mes.module.craft.enums.CraftRouteStatusEnum;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.dal.entity.BomDetailEntity;
import com.badminton.mes.module.production.dal.entity.BomEntity;
import com.badminton.mes.module.production.dal.entity.MaterialEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;
import com.badminton.mes.module.production.dal.redis.WorkOrderCache;
import com.badminton.mes.module.production.dal.redis.WorkOrderNoSequence;
import com.badminton.mes.module.production.dal.repository.BomDetailRepository;
import com.badminton.mes.module.production.dal.repository.BomRepository;
import com.badminton.mes.module.production.dal.repository.CraftRoutingRelationRepository;
import com.badminton.mes.module.production.dal.repository.CraftRoutingRelationRepository.RoutingRelationSnapshot;
import com.badminton.mes.module.production.dal.repository.MaterialRepository;
import com.badminton.mes.module.production.dal.repository.ProductRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderMaterialRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderStatusLogRepository;
import com.badminton.mes.module.production.dal.repository.WorkshopRepository;
import com.badminton.mes.module.production.enums.BomStatusEnum;
import com.badminton.mes.module.production.enums.WorkOrderStatusEnum;
import com.badminton.mes.module.production.service.support.BomDetailManager;

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
 * 工单下达时工艺路线关系校验的聚焦单元测试。
 *
 * @author 刘涵
 */
@ExtendWith(MockitoExtension.class)
class WorkOrderRoutingValidationTest {

    private static final Long WORK_ORDER_ID = 100L;
    private static final Long PRODUCT_ID = 10L;
    private static final Long BOM_ID = 30L;
    private static final Long ROUTING_ID = 40L;
    private static final Long MATERIAL_ID = 50L;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CraftRouteRepository routeRepository;

    @Mock
    private CraftRouteProductRepository routeProductRepository;

    @Mock
    private WorkshopRepository workshopRepository;

    @Mock
    private BomRepository bomRepository;

    @Mock
    private BomDetailRepository bomDetailRepository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private CraftRoutingRelationRepository craftRoutingRelationRepository;

    @Mock
    private WorkOrderMaterialRepository workOrderMaterialRepository;

    @Mock
    private WorkOrderStatusLogRepository workOrderStatusLogRepository;

    @Mock
    private WorkOrderCache workOrderCache;

    @Mock
    private WorkOrderNoSequence workOrderNoSequence;

    @Mock
    private BomDetailManager bomDetailManager;

    private WorkOrderServiceImpl workOrderService;

    @BeforeEach
    void setUp() {
        workOrderService = new WorkOrderServiceImpl(workOrderRepository, productRepository,
                routeRepository, routeProductRepository, workshopRepository, bomRepository,
                bomDetailRepository, materialRepository, craftRoutingRelationRepository,
                workOrderMaterialRepository, workOrderStatusLogRepository, workOrderCache,
                workOrderNoSequence, bomDetailManager);

        when(workOrderRepository.updateToReleased(WORK_ORDER_ID, WorkOrderStatusEnum.CREATED.getStatus(),
                WorkOrderStatusEnum.RELEASED.getStatus())).thenReturn(1);
        when(workOrderRepository.findByIdForUpdate(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildReleasedWorkOrder()));
        CraftRouteEntity route = new CraftRouteEntity();
        route.setId(ROUTING_ID);
        route.setRoutingStatus(CraftRouteStatusEnum.EFFECTIVE.getStatus());
        when(routeRepository.findByIdAndDeletedFalseForUpdate(ROUTING_ID))
                .thenReturn(Optional.of(route));
        when(routeProductRepository.existsByRouteIdAndProductIdAndDeletedFalse(ROUTING_ID, PRODUCT_ID))
                .thenReturn(true);
        when(bomRepository.findByIdAndDeletedFalse(BOM_ID)).thenReturn(Optional.of(buildEffectiveBom()));
        when(bomDetailRepository.findByBomIdAndDeletedFalse(BOM_ID)).thenReturn(List.of(buildBomDetail()));
        when(materialRepository.findByIdInAndDeletedFalse(List.of(MATERIAL_ID)))
                .thenReturn(List.of(buildEnabledMaterial()));
    }

    @Test
    @DisplayName("下达工单：工艺路线未生效时拒绝下达")
    void releaseWorkOrderRejectsUnavailableRouting() {
        assertRejected(new RoutingRelationSnapshot(false, true, 1, 1, 1, 0, 0),
                ProductionErrorCodeConstants.ROUTING_NOT_AVAILABLE);
    }

    @Test
    @DisplayName("下达工单：工艺路线未绑定产品时拒绝下达")
    void releaseWorkOrderRejectsRoutingWithoutProductBinding() {
        assertRejected(new RoutingRelationSnapshot(true, false, 1, 1, 1, 0, 0),
                ProductionErrorCodeConstants.ROUTING_PRODUCT_NOT_MATCH);
    }

    @Test
    @DisplayName("下达工单：工艺路线没有明细时拒绝下达")
    void releaseWorkOrderRejectsEmptyRoutingDetails() {
        assertRejected(new RoutingRelationSnapshot(true, true, 0, null, null, 0, 0),
                ProductionErrorCodeConstants.ROUTING_DETAIL_INVALID);
    }

    @Test
    @DisplayName("下达工单：工序顺序不连续时拒绝下达")
    void releaseWorkOrderRejectsDiscontinuousRoutingSequence() {
        assertRejected(new RoutingRelationSnapshot(true, true, 2, 1, 3, 0, 0),
                ProductionErrorCodeConstants.ROUTING_DETAIL_INVALID);
    }

    @Test
    @DisplayName("下达工单：路线明细引用不可用工序时拒绝下达")
    void releaseWorkOrderRejectsUnavailableProcess() {
        assertRejected(new RoutingRelationSnapshot(true, true, 1, 1, 1, 1, 0),
                ProductionErrorCodeConstants.ROUTING_PROCESS_NOT_AVAILABLE);
    }

    @Test
    @DisplayName("下达工单：路线明细引用不可用 SOP 时拒绝下达")
    void releaseWorkOrderRejectsUnavailableSop() {
        assertRejected(new RoutingRelationSnapshot(true, true, 1, 1, 1, 0, 1),
                ProductionErrorCodeConstants.ROUTING_SOP_NOT_AVAILABLE);
    }

    private void assertRejected(RoutingRelationSnapshot snapshot,
                                com.badminton.mes.common.core.ErrorCode expectedErrorCode) {
        when(craftRoutingRelationRepository.findRelationSnapshot(ROUTING_ID, PRODUCT_ID)).thenReturn(snapshot);

        assertThatThrownBy(() -> workOrderService.releaseWorkOrder(WORK_ORDER_ID))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(expectedErrorCode));
        verify(workOrderMaterialRepository, never()).saveAll(any());
        verify(workOrderStatusLogRepository, never()).save(any());
    }

    private WorkOrderEntity buildReleasedWorkOrder() {
        WorkOrderEntity workOrder = new WorkOrderEntity();
        workOrder.setId(WORK_ORDER_ID);
        workOrder.setProductId(PRODUCT_ID);
        workOrder.setBomId(BOM_ID);
        workOrder.setRoutingId(ROUTING_ID);
        workOrder.setOrderStatus(WorkOrderStatusEnum.CREATED.getStatus());
        return workOrder;
    }

    private BomEntity buildEffectiveBom() {
        BomEntity bom = new BomEntity();
        bom.setId(BOM_ID);
        bom.setProductId(PRODUCT_ID);
        bom.setBomStatus(BomStatusEnum.EFFECTIVE.getStatus());
        return bom;
    }

    private BomDetailEntity buildBomDetail() {
        BomDetailEntity detail = new BomDetailEntity();
        detail.setMaterialId(MATERIAL_ID);
        return detail;
    }

    private MaterialEntity buildEnabledMaterial() {
        MaterialEntity material = new MaterialEntity();
        material.setId(MATERIAL_ID);
        material.setStatus(CommonStatusEnum.ENABLED.getStatus());
        return material;
    }
}
