package com.badminton.mes.module.production.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.controller.vo.WorkOrderPageReqVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderRespVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderSaveReqVO;
import com.badminton.mes.module.production.dal.entity.ProductEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;
import com.badminton.mes.module.production.dal.entity.WorkshopEntity;
import com.badminton.mes.module.production.dal.redis.WorkOrderCache;
import com.badminton.mes.module.production.dal.redis.WorkOrderNoSequence;
import com.badminton.mes.module.production.dal.repository.ProductRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.production.dal.repository.WorkshopRepository;
import com.badminton.mes.module.production.enums.WorkOrderSourceTypeEnum;
import com.badminton.mes.module.production.enums.WorkOrderStatusEnum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link WorkOrderServiceImpl} 单元测试。
 *
 * <p>数据库与 Redis 基础设施依赖全部 Mock，不依赖外部环境，可重复执行。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
@ExtendWith(MockitoExtension.class)
class WorkOrderServiceImplTest {

    /** 测试用产品 id */
    private static final Long PRODUCT_ID = 10L;

    /** 测试用车间 id */
    private static final Long WORKSHOP_ID = 20L;

    /** 测试用工单 id */
    private static final Long WORK_ORDER_ID = 100L;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WorkshopRepository workshopRepository;

    @Mock
    private WorkOrderCache workOrderCache;

    @Mock
    private WorkOrderNoSequence workOrderNoSequence;

    private WorkOrderServiceImpl workOrderService;

    @BeforeEach
    void setUp() {
        workOrderService = new WorkOrderServiceImpl(workOrderRepository, productRepository,
                workshopRepository, workOrderCache, workOrderNoSequence);
    }

    @Test
    @DisplayName("创建工单：自动生成单号，冗余字段按产品档案回填")
    void createWorkOrderGeneratesNoAndFillsRedundancy() {
        WorkOrderSaveReqVO reqVO = buildSaveReqVO();
        when(productRepository.findByIdAndDeletedFalse(PRODUCT_ID)).thenReturn(Optional.of(buildEnabledProduct()));
        when(workshopRepository.findByIdAndDeletedFalse(WORKSHOP_ID)).thenReturn(Optional.of(buildEnabledWorkshop()));
        when(workOrderNoSequence.nextNo()).thenReturn("WO202607080001");
        doAnswer(invocation -> {
            WorkOrderEntity entity = invocation.getArgument(0);
            entity.setId(WORK_ORDER_ID);
            return entity;
        }).when(workOrderRepository).saveAndFlush(any(WorkOrderEntity.class));

        Long id = workOrderService.createWorkOrder(reqVO);

        assertThat(id).isEqualTo(WORK_ORDER_ID);
        ArgumentCaptor<WorkOrderEntity> captor = ArgumentCaptor.forClass(WorkOrderEntity.class);
        verify(workOrderRepository).saveAndFlush(captor.capture());
        WorkOrderEntity inserted = captor.getValue();
        assertThat(inserted.getWorkOrderNo()).isEqualTo("WO202607080001");
        assertThat(inserted.getSourceType()).isEqualTo(WorkOrderSourceTypeEnum.MANUAL.getType());
        assertThat(inserted.getOrderStatus()).isEqualTo(WorkOrderStatusEnum.CREATED.getStatus());
        assertThat(inserted.getProductName()).isEqualTo("比赛级羽毛球");
        assertThat(inserted.getUnitId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("创建工单：计划完成时间早于开始时间，抛计划时间异常")
    void createWorkOrderRejectsInvalidPlanTime() {
        WorkOrderSaveReqVO reqVO = buildSaveReqVO();
        reqVO.setPlanEndTime(reqVO.getPlanStartTime().minusDays(1));

        assertThatThrownBy(() -> workOrderService.createWorkOrder(reqVO))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_PLAN_TIME_INVALID));
        verify(workOrderRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("创建工单：产品已停用，抛产品不可用异常")
    void createWorkOrderRejectsDisabledProduct() {
        WorkOrderSaveReqVO reqVO = buildSaveReqVO();
        ProductEntity disabled = buildEnabledProduct();
        disabled.setStatus(0);
        when(productRepository.findByIdAndDeletedFalse(PRODUCT_ID)).thenReturn(Optional.of(disabled));

        assertThatThrownBy(() -> workOrderService.createWorkOrder(reqVO))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.PRODUCT_NOT_EXISTS));
    }

    @Test
    @DisplayName("创建工单：指定单号已存在，抛单号重复异常且不落库")
    void createWorkOrderRejectsDuplicateNo() {
        WorkOrderSaveReqVO reqVO = buildSaveReqVO();
        reqVO.setWorkOrderNo("WO202607080001");
        when(productRepository.findByIdAndDeletedFalse(PRODUCT_ID)).thenReturn(Optional.of(buildEnabledProduct()));
        when(workshopRepository.findByIdAndDeletedFalse(WORKSHOP_ID)).thenReturn(Optional.of(buildEnabledWorkshop()));
        when(workOrderRepository.existsByWorkOrderNoAndDeletedFalse("WO202607080001")).thenReturn(true);

        assertThatThrownBy(() -> workOrderService.createWorkOrder(reqVO))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_NO_DUPLICATE));
        verify(workOrderRepository, never()).saveAndFlush(any());
        verify(workOrderNoSequence, never()).nextNo();
    }

    @Test
    @DisplayName("创建工单：并发穿透查重触发唯一索引，转为单号重复业务异常")
    void createWorkOrderTranslatesDataIntegrityViolationException() {
        WorkOrderSaveReqVO reqVO = buildSaveReqVO();
        reqVO.setWorkOrderNo("WO202607080002");
        when(productRepository.findByIdAndDeletedFalse(PRODUCT_ID)).thenReturn(Optional.of(buildEnabledProduct()));
        when(workshopRepository.findByIdAndDeletedFalse(WORKSHOP_ID)).thenReturn(Optional.of(buildEnabledWorkshop()));
        when(workOrderRepository.existsByWorkOrderNoAndDeletedFalse("WO202607080002")).thenReturn(false);
        when(workOrderRepository.saveAndFlush(any(WorkOrderEntity.class)))
                .thenThrow(new DataIntegrityViolationException("uk_work_order_no"));

        assertThatThrownBy(() -> workOrderService.createWorkOrder(reqVO))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_NO_DUPLICATE));
    }

    @Test
    @DisplayName("修改工单：已下达状态不允许修改")
    void updateWorkOrderRejectsReleasedStatus() {
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildWorkOrder(WorkOrderStatusEnum.RELEASED.getStatus())));

        assertThatThrownBy(() -> workOrderService.updateWorkOrder(WORK_ORDER_ID, buildSaveReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_UPDATE));
        verify(workOrderRepository, never()).updatePlan(any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("修改工单：CAS 未命中，抛状态不允许修改异常")
    void updateWorkOrderFailsWhenCasMiss() {
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildWorkOrder(WorkOrderStatusEnum.CREATED.getStatus())));
        when(productRepository.findByIdAndDeletedFalse(PRODUCT_ID)).thenReturn(Optional.of(buildEnabledProduct()));
        when(workshopRepository.findByIdAndDeletedFalse(WORKSHOP_ID)).thenReturn(Optional.of(buildEnabledWorkshop()));
        when(workOrderRepository.updatePlan(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any())).thenReturn(0);

        assertThatThrownBy(() -> workOrderService.updateWorkOrder(WORK_ORDER_ID, buildSaveReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_UPDATE));
    }

    @Test
    @DisplayName("修改工单：成功后删除详情缓存")
    void updateWorkOrderEvictsCacheOnSuccess() {
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildWorkOrder(WorkOrderStatusEnum.CREATED.getStatus())));
        when(productRepository.findByIdAndDeletedFalse(PRODUCT_ID)).thenReturn(Optional.of(buildEnabledProduct()));
        when(workshopRepository.findByIdAndDeletedFalse(WORKSHOP_ID)).thenReturn(Optional.of(buildEnabledWorkshop()));
        when(workOrderRepository.updatePlan(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any())).thenReturn(1);

        workOrderService.updateWorkOrder(WORK_ORDER_ID, buildSaveReqVO());

        verify(workOrderCache).evict(WORK_ORDER_ID);
    }

    @Test
    @DisplayName("删除工单：成功逻辑删除并清理缓存")
    void deleteWorkOrderEvictsCacheOnSuccess() {
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildWorkOrder(WorkOrderStatusEnum.CREATED.getStatus())));
        when(workOrderRepository.logicDeleteById(WORK_ORDER_ID, WorkOrderStatusEnum.CREATED.getStatus()))
                .thenReturn(1);

        workOrderService.deleteWorkOrder(WORK_ORDER_ID);

        verify(workOrderCache).evict(WORK_ORDER_ID);
    }

    @Test
    @DisplayName("删除工单：工单不存在抛业务异常")
    void deleteWorkOrderRejectsMissingOrder() {
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workOrderService.deleteWorkOrder(WORK_ORDER_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_NOT_EXISTS));
    }

    @Test
    @DisplayName("下达工单：CAS 命中直接成功并清理缓存，不再回查")
    void releaseWorkOrderSucceedsByCas() {
        when(workOrderRepository.updateToReleased(WORK_ORDER_ID, WorkOrderStatusEnum.CREATED.getStatus(),
                WorkOrderStatusEnum.RELEASED.getStatus())).thenReturn(1);

        workOrderService.releaseWorkOrder(WORK_ORDER_ID);

        verify(workOrderCache).evict(WORK_ORDER_ID);
        verify(workOrderRepository, never()).findByIdAndDeletedFalse(any());
    }

    @Test
    @DisplayName("下达工单：未维护 BOM，查因后抛缺 BOM/工艺路线异常")
    void releaseWorkOrderRejectsMissingBom() {
        when(workOrderRepository.updateToReleased(WORK_ORDER_ID, WorkOrderStatusEnum.CREATED.getStatus(),
                WorkOrderStatusEnum.RELEASED.getStatus())).thenReturn(0);
        WorkOrderEntity created = buildWorkOrder(WorkOrderStatusEnum.CREATED.getStatus());
        created.setBomId(null);
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID)).thenReturn(Optional.of(created));

        assertThatThrownBy(() -> workOrderService.releaseWorkOrder(WORK_ORDER_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_RELEASE_MISSING_BOM_ROUTING));
    }

    @Test
    @DisplayName("下达工单：状态已是已下达，查因后抛状态不允许下达异常")
    void releaseWorkOrderRejectsWrongStatus() {
        when(workOrderRepository.updateToReleased(WORK_ORDER_ID, WorkOrderStatusEnum.CREATED.getStatus(),
                WorkOrderStatusEnum.RELEASED.getStatus())).thenReturn(0);
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildWorkOrder(WorkOrderStatusEnum.RELEASED.getStatus())));

        assertThatThrownBy(() -> workOrderService.releaseWorkOrder(WORK_ORDER_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_RELEASE));
    }

    @Test
    @DisplayName("查询详情：缓存命中直接返回，不查数据库")
    void getWorkOrderHitsCache() {
        WorkOrderEntity cached = buildWorkOrder(WorkOrderStatusEnum.CREATED.getStatus());
        when(workOrderCache.get(WORK_ORDER_ID)).thenReturn(Optional.of(cached));

        WorkOrderRespVO respVO = workOrderService.getWorkOrder(WORK_ORDER_ID);

        assertThat(respVO.getId()).isEqualTo(WORK_ORDER_ID);
        assertThat(respVO.getWorkOrderNo()).isEqualTo("WO202607080001");
        verify(workOrderRepository, never()).findByIdAndDeletedFalse(any());
    }

    @Test
    @DisplayName("查询详情：缓存未命中回源数据库并回写缓存")
    void getWorkOrderFallsBackToDatabaseAndWritesCache() {
        WorkOrderEntity entity = buildWorkOrder(WorkOrderStatusEnum.CREATED.getStatus());
        when(workOrderCache.get(WORK_ORDER_ID)).thenReturn(Optional.empty());
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID)).thenReturn(Optional.of(entity));

        WorkOrderRespVO respVO = workOrderService.getWorkOrder(WORK_ORDER_ID);

        assertThat(respVO.getId()).isEqualTo(WORK_ORDER_ID);
        verify(workOrderCache).put(entity);
    }

    @Test
    @DisplayName("查询详情：工单不存在抛业务异常且不回写缓存")
    void getWorkOrderRejectsMissingOrder() {
        when(workOrderCache.get(WORK_ORDER_ID)).thenReturn(Optional.empty());
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workOrderService.getWorkOrder(WORK_ORDER_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_NOT_EXISTS));
        verify(workOrderCache, never()).put(any());
    }

    @Test
    @DisplayName("分页查询：总数为 0 直接返回空页，不再查列表")
    void getWorkOrderPageReturnsEmptyWhenNoData() {
        WorkOrderPageReqVO reqVO = new WorkOrderPageReqVO();
        when(workOrderRepository.count(anyWorkOrderSpecification())).thenReturn(0L);

        PageResult<WorkOrderRespVO> pageResult = workOrderService.getWorkOrderPage(reqVO);

        assertThat(pageResult.getTotal()).isZero();
        assertThat(pageResult.getList()).isEmpty();
        verify(workOrderRepository, never()).findAll(anyWorkOrderSpecification(), any(Pageable.class));
    }

    @Test
    @DisplayName("分页查询：页码超过总页数时按最后一页查询")
    void getWorkOrderPageCorrectsOverflowPageNo() {
        WorkOrderPageReqVO reqVO = new WorkOrderPageReqVO();
        reqVO.setPageNo(99);
        reqVO.setPageSize(10);
        when(workOrderRepository.count(anyWorkOrderSpecification())).thenReturn(25L);
        when(workOrderRepository.findAll(anyWorkOrderSpecification(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildWorkOrder(WorkOrderStatusEnum.CREATED.getStatus()))));

        PageResult<WorkOrderRespVO> pageResult = workOrderService.getWorkOrderPage(reqVO);

        assertThat(pageResult.getPageNo()).isEqualTo(3);
        assertThat(pageResult.getTotal()).isEqualTo(25L);
        assertThat(pageResult.getList()).hasSize(1);
    }

    /**
     * 构造合法的保存请求。
     */
    private WorkOrderSaveReqVO buildSaveReqVO() {
        WorkOrderSaveReqVO reqVO = new WorkOrderSaveReqVO();
        reqVO.setProductId(PRODUCT_ID);
        reqVO.setWorkshopId(WORKSHOP_ID);
        reqVO.setPlanQuantity(1000);
        reqVO.setOverRatio(new BigDecimal("5.00"));
        reqVO.setPriority(5);
        reqVO.setPlanStartTime(LocalDateTime.of(2026, 7, 10, 8, 0, 0));
        reqVO.setPlanEndTime(LocalDateTime.of(2026, 7, 15, 18, 0, 0));
        return reqVO;
    }

    /**
     * 构造启用状态的产品档案。
     */
    private ProductEntity buildEnabledProduct() {
        ProductEntity product = new ProductEntity();
        product.setId(PRODUCT_ID);
        product.setProductCode("P001");
        product.setProductName("比赛级羽毛球");
        product.setSpec("77速 鹅毛");
        product.setUnitId(1L);
        product.setStatus(1);
        product.setDeleted(false);
        return product;
    }

    /**
     * 构造启用状态的车间档案。
     */
    private WorkshopEntity buildEnabledWorkshop() {
        WorkshopEntity workshop = new WorkshopEntity();
        workshop.setId(WORKSHOP_ID);
        workshop.setWorkshopCode("WS001");
        workshop.setWorkshopName("一号成型车间");
        workshop.setStatus(1);
        workshop.setDeleted(false);
        return workshop;
    }

    /**
     * 构造指定状态的工单。
     *
     * @param orderStatus 工单状态值
     */
    private WorkOrderEntity buildWorkOrder(Integer orderStatus) {
        WorkOrderEntity workOrder = new WorkOrderEntity();
        workOrder.setId(WORK_ORDER_ID);
        workOrder.setWorkOrderNo("WO202607080001");
        workOrder.setSourceType(WorkOrderSourceTypeEnum.MANUAL.getType());
        workOrder.setProductId(PRODUCT_ID);
        workOrder.setProductName("比赛级羽毛球");
        workOrder.setUnitId(1L);
        workOrder.setBomId(30L);
        workOrder.setRoutingId(40L);
        workOrder.setWorkshopId(WORKSHOP_ID);
        workOrder.setPlanQuantity(1000);
        workOrder.setPlanStartTime(LocalDateTime.of(2026, 7, 10, 8, 0, 0));
        workOrder.setPlanEndTime(LocalDateTime.of(2026, 7, 15, 18, 0, 0));
        workOrder.setOrderStatus(orderStatus);
        workOrder.setDeleted(false);
        return workOrder;
    }

    @SuppressWarnings("unchecked")
    private static Specification<WorkOrderEntity> anyWorkOrderSpecification() {
        return (Specification<WorkOrderEntity>) any(Specification.class);
    }
}
