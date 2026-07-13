package com.badminton.mes.module.production.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.controller.vo.ShortageHandleSaveReqVO;
import com.badminton.mes.module.production.dal.entity.KitAnalysisEntity;
import com.badminton.mes.module.production.dal.entity.KitShortageHandleEntity;
import com.badminton.mes.module.production.dal.entity.MaterialStockEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderMaterialEntity;
import com.badminton.mes.module.production.dal.redis.WorkOrderCache;
import com.badminton.mes.module.production.dal.repository.KitAnalysisRepository;
import com.badminton.mes.module.production.dal.repository.KitShortageHandleRepository;
import com.badminton.mes.module.production.dal.repository.MaterialRepository;
import com.badminton.mes.module.production.dal.repository.MaterialStockRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderMaterialRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.production.enums.KitStatusEnum;
import com.badminton.mes.module.production.enums.ShortageHandleStatusEnum;
import com.badminton.mes.module.production.enums.WorkOrderStatusEnum;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link KitAnalysisServiceImpl} 单元测试。
 *
 * <p>覆盖齐套/部分齐套/欠料三种计算分支、软删重算、冗余回写与
 * 欠料处理记录的关键规则，依赖全部 Mock。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@ExtendWith(MockitoExtension.class)
class KitAnalysisServiceImplTest {

    private static final Long WORK_ORDER_ID = 1001L;

    private static final Long OPERATOR_ID = 9L;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private WorkOrderMaterialRepository workOrderMaterialRepository;

    @Mock
    private MaterialStockRepository materialStockRepository;

    @Mock
    private KitAnalysisRepository kitAnalysisRepository;

    @Mock
    private KitShortageHandleRepository kitShortageHandleRepository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private WorkOrderCache workOrderCache;

    @InjectMocks
    private KitAnalysisServiceImpl kitAnalysisService;

    @BeforeEach
    void setUpLoginContext() {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(OPERATOR_ID);
        SecurityContextHolder.set("unit-test-token", loginUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    private WorkOrderEntity buildReleasedWorkOrder() {
        WorkOrderEntity workOrder = new WorkOrderEntity();
        workOrder.setId(WORK_ORDER_ID);
        workOrder.setOrderStatus(WorkOrderStatusEnum.RELEASED.getStatus());
        return workOrder;
    }

    private WorkOrderMaterialEntity buildMaterialLine(Long materialId, String require, String issued) {
        WorkOrderMaterialEntity line = new WorkOrderMaterialEntity();
        line.setWorkOrderId(WORK_ORDER_ID);
        line.setMaterialId(materialId);
        line.setRequireQuantity(new BigDecimal(require));
        line.setIssuedQuantity(new BigDecimal(issued));
        return line;
    }

    private MaterialStockEntity buildStock(Long materialId, String available, String locked,
                                           String checking, String transit) {
        MaterialStockEntity stock = new MaterialStockEntity();
        stock.setMaterialId(materialId);
        stock.setAvailableQuantity(new BigDecimal(available));
        stock.setLockedQuantity(new BigDecimal(locked));
        stock.setCheckingQuantity(new BigDecimal(checking));
        stock.setTransitQuantity(new BigDecimal(transit));
        return stock;
    }

    @Test
    @DisplayName("齐套分析：三种物料行状态与工单级 MAX 回写，旧结果软删、缓存清理")
    void analyzeWorkOrderCalculatesAllBranches() {
        when(workOrderRepository.findByIdForUpdate(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildReleasedWorkOrder()));
        when(workOrderMaterialRepository.findByWorkOrderIdAndDeletedFalse(WORK_ORDER_ID)).thenReturn(List.of(
                buildMaterialLine(1L, "1000", "0"),
                buildMaterialLine(2L, "16000", "1000"),
                buildMaterialLine(3L, "20", "0")));
        when(materialStockRepository.findByMaterialIdInAndDeletedFalse(anyList())).thenReturn(List.of(
                // 物料1: 可用 47000 >= 需求1000 → 齐套
                buildStock(1L, "50000", "2000", "1000", "0"),
                // 物料2: 可用 30000-20000-5000=5000 < 剩余需求 15000 且 >0 → 部分齐套
                buildStock(2L, "30000", "20000", "5000", "80000")));
        // 物料3 无库存记录 → 可用 0 → 欠料

        Integer orderStatus = kitAnalysisService.analyzeWorkOrder(WORK_ORDER_ID);

        assertThat(orderStatus).isEqualTo(KitStatusEnum.SHORTAGE.getStatus());
        verify(kitAnalysisRepository).softDeleteByWorkOrderId(WORK_ORDER_ID);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<KitAnalysisEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(kitAnalysisRepository).saveAll(captor.capture());
        List<KitAnalysisEntity> saved = captor.getValue();
        assertThat(saved).hasSize(3);

        KitAnalysisEntity complete = saved.get(0);
        assertThat(complete.getKitStatus()).isEqualTo(KitStatusEnum.COMPLETE.getStatus());
        assertThat(complete.getAvailableQuantity()).isEqualByComparingTo("47000");
        assertThat(complete.getShortageQuantity()).isEqualByComparingTo("0");

        KitAnalysisEntity partial = saved.get(1);
        assertThat(partial.getKitStatus()).isEqualTo(KitStatusEnum.PARTIAL.getStatus());
        assertThat(partial.getRequireQuantity()).isEqualByComparingTo("15000");
        assertThat(partial.getAvailableQuantity()).isEqualByComparingTo("5000");
        assertThat(partial.getShortageQuantity()).isEqualByComparingTo("10000");
        assertThat(partial.getTransitQuantity()).isEqualByComparingTo("80000");

        KitAnalysisEntity shortage = saved.get(2);
        assertThat(shortage.getKitStatus()).isEqualTo(KitStatusEnum.SHORTAGE.getStatus());
        assertThat(shortage.getAvailableQuantity()).isEqualByComparingTo("0");
        assertThat(shortage.getShortageQuantity()).isEqualByComparingTo("20");

        verify(workOrderRepository).updateKitStatus(WORK_ORDER_ID, KitStatusEnum.SHORTAGE.getStatus());
        verify(workOrderCache).evictAfterCommit(WORK_ORDER_ID);
    }

    @Test
    @DisplayName("齐套分析：可用扣减锁定在检后为负按 0 处理，不出现负可用")
    void analyzeWorkOrderClampsNegativeAvailableToZero() {
        when(workOrderRepository.findByIdForUpdate(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildReleasedWorkOrder()));
        when(workOrderMaterialRepository.findByWorkOrderIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(List.of(buildMaterialLine(1L, "100", "0")));
        // 可用 100 - 锁定 150 = -50 → 按 0
        when(materialStockRepository.findByMaterialIdInAndDeletedFalse(anyList()))
                .thenReturn(List.of(buildStock(1L, "100", "150", "0", "0")));

        Integer orderStatus = kitAnalysisService.analyzeWorkOrder(WORK_ORDER_ID);

        assertThat(orderStatus).isEqualTo(KitStatusEnum.SHORTAGE.getStatus());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<KitAnalysisEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(kitAnalysisRepository).saveAll(captor.capture());
        assertThat(captor.getValue().get(0).getAvailableQuantity()).isEqualByComparingTo("0");
        assertThat(captor.getValue().get(0).getShortageQuantity()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("齐套分析：全部物料齐套时工单回写齐套状态")
    void analyzeWorkOrderAllComplete() {
        when(workOrderRepository.findByIdForUpdate(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildReleasedWorkOrder()));
        when(workOrderMaterialRepository.findByWorkOrderIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(List.of(buildMaterialLine(1L, "100", "0")));
        when(materialStockRepository.findByMaterialIdInAndDeletedFalse(anyList()))
                .thenReturn(List.of(buildStock(1L, "500", "0", "0", "0")));

        Integer orderStatus = kitAnalysisService.analyzeWorkOrder(WORK_ORDER_ID);

        assertThat(orderStatus).isEqualTo(KitStatusEnum.COMPLETE.getStatus());
        verify(workOrderRepository).updateKitStatus(WORK_ORDER_ID, KitStatusEnum.COMPLETE.getStatus());
    }

    @Test
    @DisplayName("齐套分析：工单不存在抛 A0402")
    void analyzeWorkOrderRejectsMissingOrder() {
        when(workOrderRepository.findByIdForUpdate(WORK_ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> kitAnalysisService.analyzeWorkOrder(WORK_ORDER_ID))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode())
                                .isEqualTo(ProductionErrorCodeConstants.WORK_ORDER_NOT_EXISTS));
    }

    @Test
    @DisplayName("齐套分析：已创建状态不允许分析抛 A0440")
    void analyzeWorkOrderRejectsCreatedStatus() {
        WorkOrderEntity workOrder = buildReleasedWorkOrder();
        workOrder.setOrderStatus(WorkOrderStatusEnum.CREATED.getStatus());
        when(workOrderRepository.findByIdForUpdate(WORK_ORDER_ID)).thenReturn(Optional.of(workOrder));

        assertThatThrownBy(() -> kitAnalysisService.analyzeWorkOrder(WORK_ORDER_ID))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode())
                                .isEqualTo(ProductionErrorCodeConstants.KIT_ANALYSIS_ORDER_STATUS_INVALID));
        verify(kitAnalysisRepository, never()).softDeleteByWorkOrderId(anyLong());
    }

    @Test
    @DisplayName("齐套分析：无物料需求抛 A0440 且不软删旧结果")
    void analyzeWorkOrderRejectsEmptyMaterials() {
        when(workOrderRepository.findByIdForUpdate(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildReleasedWorkOrder()));
        when(workOrderMaterialRepository.findByWorkOrderIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(List.of());

        assertThatThrownBy(() -> kitAnalysisService.analyzeWorkOrder(WORK_ORDER_ID))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode())
                                .isEqualTo(ProductionErrorCodeConstants.KIT_ANALYSIS_MATERIAL_EMPTY));
        verify(kitAnalysisRepository, never()).softDeleteByWorkOrderId(anyLong());
    }

    @Test
    @DisplayName("新增欠料处理：登录人作为创建人落库，初始状态处理中")
    void createShortageHandleSavesWithOperator() {
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildReleasedWorkOrder()));
        ShortageHandleSaveReqVO reqVO = new ShortageHandleSaveReqVO();
        reqVO.setWorkOrderId(WORK_ORDER_ID);
        reqVO.setMaterialId(2L);
        reqVO.setHandleType(1);
        reqVO.setHandlerId(5L);

        kitAnalysisService.createShortageHandle(reqVO);

        ArgumentCaptor<KitShortageHandleEntity> captor =
                ArgumentCaptor.forClass(KitShortageHandleEntity.class);
        verify(kitShortageHandleRepository).save(captor.capture());
        assertThat(captor.getValue().getCreateBy()).isEqualTo(OPERATOR_ID);
        assertThat(captor.getValue().getHandleStatus())
                .isEqualTo(ShortageHandleStatusEnum.PROCESSING.getStatus());
    }

    @Test
    @DisplayName("解决欠料处理：已解决记录重复解决抛 A0506")
    void resolveShortageHandleRejectsResolved() {
        KitShortageHandleEntity entity = new KitShortageHandleEntity();
        entity.setId(7L);
        entity.setHandleStatus(ShortageHandleStatusEnum.RESOLVED.getStatus());
        when(kitShortageHandleRepository.updateHandleStatus(7L,
                ShortageHandleStatusEnum.PROCESSING.getStatus(),
                ShortageHandleStatusEnum.RESOLVED.getStatus())).thenReturn(0);
        when(kitShortageHandleRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> kitAnalysisService.resolveShortageHandle(7L))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode())
                                .isEqualTo(ProductionErrorCodeConstants.SHORTAGE_HANDLE_ALREADY_RESOLVED));
        verify(kitShortageHandleRepository, never()).save(any());
    }

    @Test
    @DisplayName("解决欠料处理：CAS 命中直接返回且不调用 save")
    void resolveShortageHandleMarksResolved() {
        when(kitShortageHandleRepository.updateHandleStatus(7L,
                ShortageHandleStatusEnum.PROCESSING.getStatus(),
                ShortageHandleStatusEnum.RESOLVED.getStatus())).thenReturn(1);

        kitAnalysisService.resolveShortageHandle(7L);

        verify(kitShortageHandleRepository, never()).findByIdAndDeletedFalse(7L);
        verify(kitShortageHandleRepository, never()).save(any());
    }

    @Test
    @DisplayName("解决欠料处理：CAS 未命中且记录不存在抛 A0402")
    void resolveShortageHandleRejectsMissingAfterCasMiss() {
        when(kitShortageHandleRepository.updateHandleStatus(7L,
                ShortageHandleStatusEnum.PROCESSING.getStatus(),
                ShortageHandleStatusEnum.RESOLVED.getStatus())).thenReturn(0);
        when(kitShortageHandleRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> kitAnalysisService.resolveShortageHandle(7L))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode())
                                .isEqualTo(ProductionErrorCodeConstants.SHORTAGE_HANDLE_NOT_EXISTS));
        verify(kitShortageHandleRepository, never()).save(any());
    }
}
