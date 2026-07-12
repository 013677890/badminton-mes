package com.badminton.mes.module.production.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.craft.dal.entity.CraftRouteEntity;
import com.badminton.mes.module.craft.dal.repository.CraftRouteProductRepository;
import com.badminton.mes.module.craft.dal.repository.CraftRouteRepository;
import com.badminton.mes.module.craft.enums.CraftRouteStatusEnum;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.controller.vo.WorkOrderMaterialRespVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderPageReqVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderRespVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderSaveReqVO;
import com.badminton.mes.module.production.dal.entity.BomDetailEntity;
import com.badminton.mes.module.production.dal.entity.BomEntity;
import com.badminton.mes.module.production.dal.entity.MaterialEntity;
import com.badminton.mes.module.production.dal.entity.ProductEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderMaterialEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderStatusLogEntity;
import com.badminton.mes.module.production.dal.entity.WorkshopEntity;
import com.badminton.mes.module.production.dal.redis.WorkOrderCache;
import com.badminton.mes.module.production.dal.redis.WorkOrderNoSequence;
import com.badminton.mes.module.production.dal.repository.BomDetailRepository;
import com.badminton.mes.module.production.dal.repository.BomRepository;
import com.badminton.mes.module.production.dal.repository.MaterialRepository;
import com.badminton.mes.module.production.dal.repository.ProductRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderMaterialRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderStatusLogRepository;
import com.badminton.mes.module.production.dal.repository.WorkshopRepository;
import com.badminton.mes.module.production.enums.BomStatusEnum;
import com.badminton.mes.module.production.enums.WorkOrderChangeTypeEnum;
import com.badminton.mes.module.production.enums.WorkOrderSourceTypeEnum;
import com.badminton.mes.module.production.enums.WorkOrderStatusEnum;
import com.badminton.mes.module.production.service.support.BomDetailManager;

import org.junit.jupiter.api.AfterEach;
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

    /** 测试用 BOM id */
    private static final Long BOM_ID = 30L;

    /** 测试用工艺路线 id */
    private static final Long ROUTE_ID = 40L;

    /** 测试用物料 id */
    private static final Long MATERIAL_ID = 50L;

    /** 测试用登录用户 id，工单 create_by / operate_by 应取该值 */
    private static final Long OPERATOR_ID = 9L;

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
                bomDetailRepository, materialRepository, workOrderMaterialRepository,
                workOrderStatusLogRepository, workOrderCache, workOrderNoSequence,
                bomDetailManager);
        // Service 从登录上下文取操作人，单测手工构造上下文并在用后清理
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(OPERATOR_ID);
        loginUser.setUserNo("tester");
        SecurityContextHolder.set("unit-test-token", loginUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    @DisplayName("创建工单：自动生成单号，冗余字段按产品档案回填")
    void createWorkOrderGeneratesNoAndFillsRedundancy() {
        WorkOrderSaveReqVO reqVO = buildSaveReqVO();
        when(productRepository.findByIdAndDeletedFalseForUpdate(PRODUCT_ID))
                .thenReturn(Optional.of(buildEnabledProduct()));
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
        // 操作人取自登录上下文，不再是占位常量
        assertThat(inserted.getCreateBy()).isEqualTo(OPERATOR_ID);
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
        when(productRepository.findByIdAndDeletedFalseForUpdate(PRODUCT_ID))
                .thenReturn(Optional.of(disabled));

        assertThatThrownBy(() -> workOrderService.createWorkOrder(reqVO))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.PRODUCT_NOT_EXISTS));
    }

    @Test
    @DisplayName("创建工单：指定单号已存在，抛单号重复异常且不落库")
    void createWorkOrderRejectsDuplicateNo() {
        WorkOrderSaveReqVO reqVO = buildSaveReqVO();
        reqVO.setWorkOrderNo("WO202607080001");
        when(productRepository.findByIdAndDeletedFalseForUpdate(PRODUCT_ID))
                .thenReturn(Optional.of(buildEnabledProduct()));
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
        when(productRepository.findByIdAndDeletedFalseForUpdate(PRODUCT_ID))
                .thenReturn(Optional.of(buildEnabledProduct()));
        when(workshopRepository.findByIdAndDeletedFalse(WORKSHOP_ID)).thenReturn(Optional.of(buildEnabledWorkshop()));
        when(workOrderRepository.existsByWorkOrderNoAndDeletedFalse("WO202607080002")).thenReturn(false);
        when(workOrderRepository.saveAndFlush(any(WorkOrderEntity.class)))
                .thenThrow(new DataIntegrityViolationException("uk_work_order_no"));

        assertThatThrownBy(() -> workOrderService.createWorkOrder(reqVO))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_NO_DUPLICATE));
    }

    @Test
    @DisplayName("修改工单：已下达状态未填变更原因，抛变更原因必填异常")
    void updateWorkOrderRejectsReleasedWithoutReason() {
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildWorkOrder(WorkOrderStatusEnum.RELEASED.getStatus())));

        assertThatThrownBy(() -> workOrderService.updateWorkOrder(WORK_ORDER_ID, buildSaveReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_CHANGE_REASON_REQUIRED));
        verify(workOrderRepository, never()).updateReleasedPlan(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("修改工单：已下达状态带变更原因，仅改计划并记录计划变更日志")
    void updateWorkOrderReleasedWithReasonWritesLog() {
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildWorkOrder(WorkOrderStatusEnum.RELEASED.getStatus())));
        when(workOrderRepository.updateReleasedPlan(any(), any(), any(), any(), any())).thenReturn(1);
        WorkOrderSaveReqVO reqVO = buildSaveReqVO();
        reqVO.setChangeReason("客户加急，交期提前");

        workOrderService.updateWorkOrder(WORK_ORDER_ID, reqVO);

        ArgumentCaptor<WorkOrderStatusLogEntity> captor = ArgumentCaptor.forClass(WorkOrderStatusLogEntity.class);
        verify(workOrderStatusLogRepository).save(captor.capture());
        WorkOrderStatusLogEntity statusLog = captor.getValue();
        assertThat(statusLog.getChangeType()).isEqualTo(WorkOrderChangeTypeEnum.PLAN_CHANGE.getType());
        assertThat(statusLog.getChangeReason()).isEqualTo("客户加急，交期提前");
        verify(workOrderCache).evictAfterCommit(WORK_ORDER_ID);
    }

    @Test
    @DisplayName("修改工单：已下达计划数量低于已派工数量，抛数量校验异常")
    void updateWorkOrderReleasedRejectsPlanLessThanDispatched() {
        WorkOrderEntity released = buildWorkOrder(WorkOrderStatusEnum.RELEASED.getStatus());
        released.setDispatchedQuantity(2000);
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID)).thenReturn(Optional.of(released));
        when(workOrderRepository.updateReleasedPlan(any(), any(), any(), any(), any())).thenReturn(0);
        WorkOrderSaveReqVO reqVO = buildSaveReqVO();
        reqVO.setChangeReason("下调计划");

        assertThatThrownBy(() -> workOrderService.updateWorkOrder(WORK_ORDER_ID, reqVO))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_PLAN_LESS_THAN_DISPATCHED));
    }

    @Test
    @DisplayName("修改工单：已下达计划数量变化后按 BOM 重算物料需求，保留已领并清理孤儿行")
    void updateWorkOrderReleasedRecalculatesMaterials() {
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildWorkOrder(WorkOrderStatusEnum.RELEASED.getStatus())));
        when(workOrderRepository.updateReleasedPlan(any(), any(), any(), any(), any())).thenReturn(1);
        when(bomDetailRepository.findByBomIdAndDeletedFalse(BOM_ID)).thenReturn(List.of(buildBomDetail()));
        // 已有行：BOM 内物料(已领 100)；孤儿行：BOM 已移除且未领料的物料
        WorkOrderMaterialEntity bomRow = new WorkOrderMaterialEntity();
        bomRow.setId(1L);
        bomRow.setWorkOrderId(WORK_ORDER_ID);
        bomRow.setMaterialId(MATERIAL_ID);
        bomRow.setRequireQuantity(new BigDecimal("16800.0000"));
        bomRow.setIssuedQuantity(new BigDecimal("100.0000"));
        WorkOrderMaterialEntity orphanRow = new WorkOrderMaterialEntity();
        orphanRow.setId(2L);
        orphanRow.setWorkOrderId(WORK_ORDER_ID);
        orphanRow.setMaterialId(60L);
        orphanRow.setRequireQuantity(new BigDecimal("50.0000"));
        orphanRow.setIssuedQuantity(BigDecimal.ZERO);
        orphanRow.setDeleted(false);
        when(workOrderMaterialRepository.findByWorkOrderIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(List.of(bomRow, orphanRow));
        WorkOrderSaveReqVO reqVO = buildSaveReqVO();
        reqVO.setPlanQuantity(2000);
        reqVO.setChangeReason("客户追加订单");

        workOrderService.updateWorkOrder(WORK_ORDER_ID, reqVO);

        // 2000 × 16 ×(1 + 5%) = 33600；已领数量保留；孤儿行逻辑删除
        ArgumentCaptor<List<WorkOrderMaterialEntity>> captor = ArgumentCaptor.captor();
        verify(workOrderMaterialRepository).saveAll(captor.capture());
        List<WorkOrderMaterialEntity> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(bomRow.getRequireQuantity()).isEqualByComparingTo(new BigDecimal("33600"));
        assertThat(bomRow.getIssuedQuantity()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(orphanRow.getDeleted()).isTrue();
        verify(workOrderStatusLogRepository).save(any(WorkOrderStatusLogEntity.class));
    }

    @Test
    @DisplayName("修改工单：重算后需求数量低于已领数量，抛已领超出异常且不落库")
    void updateWorkOrderReleasedRejectsRequireBelowIssued() {
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildWorkOrder(WorkOrderStatusEnum.RELEASED.getStatus())));
        when(workOrderRepository.updateReleasedPlan(any(), any(), any(), any(), any())).thenReturn(1);
        when(bomDetailRepository.findByBomIdAndDeletedFalse(BOM_ID)).thenReturn(List.of(buildBomDetail()));
        WorkOrderMaterialEntity issuedRow = new WorkOrderMaterialEntity();
        issuedRow.setWorkOrderId(WORK_ORDER_ID);
        issuedRow.setMaterialId(MATERIAL_ID);
        issuedRow.setRequireQuantity(new BigDecimal("16800.0000"));
        issuedRow.setIssuedQuantity(new BigDecimal("100.0000"));
        when(workOrderMaterialRepository.findByWorkOrderIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(List.of(issuedRow));
        WorkOrderSaveReqVO reqVO = buildSaveReqVO();
        // 新需求 = 1 × 16 × 1.05 = 16.8，低于已领 100
        reqVO.setPlanQuantity(1);
        reqVO.setChangeReason("大幅下调计划");

        assertThatThrownBy(() -> workOrderService.updateWorkOrder(WORK_ORDER_ID, reqVO))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_MATERIAL_ISSUED_EXCEED));
        verify(workOrderMaterialRepository, never()).saveAll(any());
        verify(workOrderStatusLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("修改工单：生产中状态不允许修改")
    void updateWorkOrderRejectsInProductionStatus() {
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildWorkOrder(WorkOrderStatusEnum.IN_PRODUCTION.getStatus())));

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
        when(productRepository.findByIdAndDeletedFalseForUpdate(PRODUCT_ID))
                .thenReturn(Optional.of(buildEnabledProduct()));
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
        when(productRepository.findByIdAndDeletedFalseForUpdate(PRODUCT_ID))
                .thenReturn(Optional.of(buildEnabledProduct()));
        when(workshopRepository.findByIdAndDeletedFalse(WORKSHOP_ID)).thenReturn(Optional.of(buildEnabledWorkshop()));
        when(workOrderRepository.updatePlan(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any())).thenReturn(1);

        workOrderService.updateWorkOrder(WORK_ORDER_ID, buildSaveReqVO());

        verify(workOrderCache).evictAfterCommit(WORK_ORDER_ID);
    }

    @Test
    @DisplayName("删除工单：成功逻辑删除并清理缓存")
    void deleteWorkOrderEvictsCacheOnSuccess() {
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildWorkOrder(WorkOrderStatusEnum.CREATED.getStatus())));
        when(workOrderRepository.logicDeleteById(WORK_ORDER_ID, WorkOrderStatusEnum.CREATED.getStatus()))
                .thenReturn(1);

        workOrderService.deleteWorkOrder(WORK_ORDER_ID);

        verify(workOrderCache).evictAfterCommit(WORK_ORDER_ID);
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
    @DisplayName("下达工单：CAS 命中后按 BOM 生成物料需求(含损耗率)并记录状态日志")
    void releaseWorkOrderGeneratesMaterialsAndLog() {
        stubReleasableWorkOrder();
        when(workOrderRepository.updateToReleased(WORK_ORDER_ID, WorkOrderStatusEnum.CREATED.getStatus(),
                WorkOrderStatusEnum.RELEASED.getStatus())).thenReturn(1);
        when(bomRepository.findByIdAndDeletedFalse(BOM_ID))
                .thenReturn(Optional.of(buildBom(BomStatusEnum.EFFECTIVE.getStatus())));
        when(bomDetailRepository.findByBomIdAndDeletedFalse(BOM_ID)).thenReturn(List.of(buildBomDetail()));
        when(workOrderMaterialRepository.existsByWorkOrderIdAndDeletedFalse(WORK_ORDER_ID)).thenReturn(false);

        workOrderService.releaseWorkOrder(WORK_ORDER_ID);

        // 计划 1000 × 用量 16 ×(1 + 5%) = 16800
        verify(bomDetailManager).validateAndLockExisting(any());
        ArgumentCaptor<List<WorkOrderMaterialEntity>> materialCaptor = ArgumentCaptor.captor();
        verify(workOrderMaterialRepository).saveAll(materialCaptor.capture());
        List<WorkOrderMaterialEntity> materials = materialCaptor.getValue();
        assertThat(materials).hasSize(1);
        assertThat(materials.get(0).getMaterialId()).isEqualTo(MATERIAL_ID);
        assertThat(materials.get(0).getRequireQuantity()).isEqualByComparingTo(new BigDecimal("16800"));
        ArgumentCaptor<WorkOrderStatusLogEntity> logCaptor = ArgumentCaptor.forClass(WorkOrderStatusLogEntity.class);
        verify(workOrderStatusLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getToStatus()).isEqualTo(WorkOrderStatusEnum.RELEASED.getStatus());
        verify(workOrderCache).evictAfterCommit(WORK_ORDER_ID);
    }

    @Test
    @DisplayName("下达工单：BOM 未生效，抛 BOM 不可用异常")
    void releaseWorkOrderRejectsIneffectiveBom() {
        stubReleasableWorkOrder();
        when(workOrderRepository.updateToReleased(WORK_ORDER_ID, WorkOrderStatusEnum.CREATED.getStatus(),
                WorkOrderStatusEnum.RELEASED.getStatus())).thenReturn(1);
        when(bomRepository.findByIdAndDeletedFalse(BOM_ID))
                .thenReturn(Optional.of(buildBom(BomStatusEnum.DRAFT.getStatus())));

        assertThatThrownBy(() -> workOrderService.releaseWorkOrder(WORK_ORDER_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.BOM_NOT_AVAILABLE));
        verify(workOrderMaterialRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("下达工单：BOM 无明细，抛明细为空异常")
    void releaseWorkOrderRejectsEmptyBomDetail() {
        stubReleasableWorkOrder();
        when(workOrderRepository.updateToReleased(WORK_ORDER_ID, WorkOrderStatusEnum.CREATED.getStatus(),
                WorkOrderStatusEnum.RELEASED.getStatus())).thenReturn(1);
        when(bomRepository.findByIdAndDeletedFalse(BOM_ID))
                .thenReturn(Optional.of(buildBom(BomStatusEnum.EFFECTIVE.getStatus())));
        when(bomDetailRepository.findByBomIdAndDeletedFalse(BOM_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> workOrderService.releaseWorkOrder(WORK_ORDER_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.BOM_DETAIL_EMPTY));
    }

    @Test
    @DisplayName("下达工单：未维护 BOM，查因后抛缺 BOM/工艺路线异常")
    void releaseWorkOrderRejectsMissingBom() {
        WorkOrderEntity created = buildWorkOrder(WorkOrderStatusEnum.CREATED.getStatus());
        created.setBomId(null);
        when(workOrderRepository.findByIdForUpdate(WORK_ORDER_ID)).thenReturn(Optional.of(created));

        assertThatThrownBy(() -> workOrderService.releaseWorkOrder(WORK_ORDER_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_RELEASE_MISSING_BOM_ROUTING));
    }

    @Test
    @DisplayName("下达工单：状态已是已下达，查因后抛状态不允许下达异常")
    void releaseWorkOrderRejectsWrongStatus() {
        when(workOrderRepository.findByIdForUpdate(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildWorkOrder(WorkOrderStatusEnum.RELEASED.getStatus())));

        assertThatThrownBy(() -> workOrderService.releaseWorkOrder(WORK_ORDER_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_RELEASE));
    }

    @Test
    @DisplayName("下达工单：路线未绑定当前产品时拒绝下达")
    void releaseWorkOrderRejectsUnboundRoute() {
        when(workOrderRepository.findByIdForUpdate(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildWorkOrder(WorkOrderStatusEnum.CREATED.getStatus())));
        when(routeRepository.findByIdAndDeletedFalseForUpdate(ROUTE_ID))
                .thenReturn(Optional.of(buildRoute(CraftRouteStatusEnum.EFFECTIVE)));

        assertThatThrownBy(() -> workOrderService.releaseWorkOrder(WORK_ORDER_ID))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_ROUTING_NOT_AVAILABLE));
        verify(workOrderRepository, never()).updateToReleased(any(), any(), any());
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

    @Test
    @DisplayName("暂停工单：已下达状态可暂停，命中的 CAS 分支即日志 fromStatus")
    void pauseWorkOrderWritesLogWithReason() {
        when(workOrderRepository.updateStatus(WORK_ORDER_ID,
                List.of(WorkOrderStatusEnum.RELEASED.getStatus()),
                WorkOrderStatusEnum.PAUSED.getStatus())).thenReturn(1);

        workOrderService.pauseWorkOrder(WORK_ORDER_ID, "羽片缺料");

        ArgumentCaptor<WorkOrderStatusLogEntity> captor = ArgumentCaptor.forClass(WorkOrderStatusLogEntity.class);
        verify(workOrderStatusLogRepository).save(captor.capture());
        WorkOrderStatusLogEntity statusLog = captor.getValue();
        assertThat(statusLog.getFromStatus()).isEqualTo(WorkOrderStatusEnum.RELEASED.getStatus());
        assertThat(statusLog.getToStatus()).isEqualTo(WorkOrderStatusEnum.PAUSED.getStatus());
        assertThat(statusLog.getChangeReason()).isEqualTo("羽片缺料");
        verify(workOrderCache).evictAfterCommit(WORK_ORDER_ID);
    }

    @Test
    @DisplayName("暂停工单：生产中状态在第二次 CAS 命中，日志记生产中而非读取快照")
    void pauseWorkOrderRecordsActualFromStatus() {
        // 第一次尝试(已下达)未命中，第二次(生产中)命中：fromStatus 必须是生产中
        when(workOrderRepository.updateStatus(WORK_ORDER_ID,
                List.of(WorkOrderStatusEnum.RELEASED.getStatus()),
                WorkOrderStatusEnum.PAUSED.getStatus())).thenReturn(0);
        when(workOrderRepository.updateStatus(WORK_ORDER_ID,
                List.of(WorkOrderStatusEnum.IN_PRODUCTION.getStatus()),
                WorkOrderStatusEnum.PAUSED.getStatus())).thenReturn(1);

        workOrderService.pauseWorkOrder(WORK_ORDER_ID, "设备故障");

        ArgumentCaptor<WorkOrderStatusLogEntity> captor = ArgumentCaptor.forClass(WorkOrderStatusLogEntity.class);
        verify(workOrderStatusLogRepository).save(captor.capture());
        assertThat(captor.getValue().getFromStatus())
                .isEqualTo(WorkOrderStatusEnum.IN_PRODUCTION.getStatus());
    }

    @Test
    @DisplayName("暂停工单：已创建状态 CAS 未命中，抛状态不允许暂停异常")
    void pauseWorkOrderRejectsCreatedStatus() {
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildWorkOrder(WorkOrderStatusEnum.CREATED.getStatus())));
        when(workOrderRepository.updateStatus(any(), any(), any())).thenReturn(0);

        assertThatThrownBy(() -> workOrderService.pauseWorkOrder(WORK_ORDER_ID, "误操作"))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_PAUSE));
        verify(workOrderStatusLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("恢复工单：从最近一条暂停日志还原暂停前状态")
    void resumeWorkOrderRestoresStatusFromLog() {
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildWorkOrder(WorkOrderStatusEnum.PAUSED.getStatus())));
        WorkOrderStatusLogEntity pauseLog = new WorkOrderStatusLogEntity();
        pauseLog.setFromStatus(WorkOrderStatusEnum.IN_PRODUCTION.getStatus());
        pauseLog.setToStatus(WorkOrderStatusEnum.PAUSED.getStatus());
        when(workOrderStatusLogRepository.findFirstByWorkOrderIdAndToStatusAndDeletedFalseOrderByIdDesc(
                WORK_ORDER_ID, WorkOrderStatusEnum.PAUSED.getStatus())).thenReturn(Optional.of(pauseLog));
        when(workOrderRepository.updateStatus(WORK_ORDER_ID, List.of(WorkOrderStatusEnum.PAUSED.getStatus()),
                WorkOrderStatusEnum.IN_PRODUCTION.getStatus())).thenReturn(1);

        workOrderService.resumeWorkOrder(WORK_ORDER_ID);

        ArgumentCaptor<WorkOrderStatusLogEntity> captor = ArgumentCaptor.forClass(WorkOrderStatusLogEntity.class);
        verify(workOrderStatusLogRepository).save(captor.capture());
        assertThat(captor.getValue().getToStatus()).isEqualTo(WorkOrderStatusEnum.IN_PRODUCTION.getStatus());
        verify(workOrderCache).evictAfterCommit(WORK_ORDER_ID);
    }

    @Test
    @DisplayName("恢复工单：非暂停状态 CAS 未命中，抛状态不允许恢复异常")
    void resumeWorkOrderRejectsNotPausedStatus() {
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildWorkOrder(WorkOrderStatusEnum.RELEASED.getStatus())));
        when(workOrderStatusLogRepository.findFirstByWorkOrderIdAndToStatusAndDeletedFalseOrderByIdDesc(
                WORK_ORDER_ID, WorkOrderStatusEnum.PAUSED.getStatus())).thenReturn(Optional.empty());
        when(workOrderRepository.updateStatus(any(), any(), any())).thenReturn(0);

        assertThatThrownBy(() -> workOrderService.resumeWorkOrder(WORK_ORDER_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_RESUME));
    }

    @Test
    @DisplayName("完工工单：生产中分支 CAS 命中，日志记真实前置状态")
    void finishWorkOrderWritesLog() {
        // 先尝试已下达(未命中)再尝试生产中(命中)
        when(workOrderRepository.updateToFinished(WORK_ORDER_ID, WorkOrderStatusEnum.RELEASED.getStatus(),
                WorkOrderStatusEnum.FINISHED.getStatus())).thenReturn(0);
        when(workOrderRepository.updateToFinished(WORK_ORDER_ID, WorkOrderStatusEnum.IN_PRODUCTION.getStatus(),
                WorkOrderStatusEnum.FINISHED.getStatus())).thenReturn(1);

        workOrderService.finishWorkOrder(WORK_ORDER_ID);

        ArgumentCaptor<WorkOrderStatusLogEntity> captor = ArgumentCaptor.forClass(WorkOrderStatusLogEntity.class);
        verify(workOrderStatusLogRepository).save(captor.capture());
        assertThat(captor.getValue().getFromStatus()).isEqualTo(WorkOrderStatusEnum.IN_PRODUCTION.getStatus());
        assertThat(captor.getValue().getToStatus()).isEqualTo(WorkOrderStatusEnum.FINISHED.getStatus());
        verify(workOrderCache).evictAfterCommit(WORK_ORDER_ID);
    }

    @Test
    @DisplayName("完工工单：数量超限时 CAS 未命中，查因后抛超限异常")
    void finishWorkOrderRejectsQuantityExceedingLimit() {
        // CAS 的 SQL 条件含数量上限，超限时两个分支都未命中(默认返回 0)
        WorkOrderEntity inProduction = buildWorkOrder(WorkOrderStatusEnum.IN_PRODUCTION.getStatus());
        inProduction.setOverRatio(new BigDecimal("5.00"));
        // 上限 = 1000 × 1.05 = 1050
        inProduction.setFinishQuantity(1051);
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID)).thenReturn(Optional.of(inProduction));

        assertThatThrownBy(() -> workOrderService.finishWorkOrder(WORK_ORDER_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_FINISH_EXCEED_LIMIT));
        verify(workOrderStatusLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("关闭工单：已完工状态可关闭并记录状态日志")
    void closeWorkOrderWritesLog() {
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildWorkOrder(WorkOrderStatusEnum.FINISHED.getStatus())));
        when(workOrderRepository.updateStatus(WORK_ORDER_ID, List.of(WorkOrderStatusEnum.FINISHED.getStatus()),
                WorkOrderStatusEnum.CLOSED.getStatus())).thenReturn(1);

        workOrderService.closeWorkOrder(WORK_ORDER_ID);

        verify(workOrderStatusLogRepository).save(any(WorkOrderStatusLogEntity.class));
        verify(workOrderCache).evictAfterCommit(WORK_ORDER_ID);
    }

    @Test
    @DisplayName("关闭工单：未完工状态 CAS 未命中，抛状态不允许关闭异常")
    void closeWorkOrderRejectsNotFinishedStatus() {
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildWorkOrder(WorkOrderStatusEnum.IN_PRODUCTION.getStatus())));
        when(workOrderRepository.updateStatus(any(), any(), any())).thenReturn(0);

        assertThatThrownBy(() -> workOrderService.closeWorkOrder(WORK_ORDER_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_CLOSE));
    }

    @Test
    @DisplayName("缓存删除：写路径委托缓存组件在提交后删除")
    void evictionDelegatesAfterCommit() {
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildWorkOrder(WorkOrderStatusEnum.FINISHED.getStatus())));
        when(workOrderRepository.updateStatus(WORK_ORDER_ID,
                List.of(WorkOrderStatusEnum.FINISHED.getStatus()),
                WorkOrderStatusEnum.CLOSED.getStatus())).thenReturn(1);

        workOrderService.closeWorkOrder(WORK_ORDER_ID);

        verify(workOrderCache).evictAfterCommit(WORK_ORDER_ID);
        verify(workOrderCache, never()).evict(any());
    }

    @Test
    @DisplayName("作废工单：已创建状态可作废，物料需求随单逻辑删除")
    void cancelWorkOrderWritesLogWithReason() {
        when(workOrderRepository.updateStatus(WORK_ORDER_ID,
                List.of(WorkOrderStatusEnum.CREATED.getStatus()),
                WorkOrderStatusEnum.CANCELLED.getStatus())).thenReturn(1);

        workOrderService.cancelWorkOrder(WORK_ORDER_ID, "重复创建");

        ArgumentCaptor<WorkOrderStatusLogEntity> captor = ArgumentCaptor.forClass(WorkOrderStatusLogEntity.class);
        verify(workOrderStatusLogRepository).save(captor.capture());
        assertThat(captor.getValue().getFromStatus()).isEqualTo(WorkOrderStatusEnum.CREATED.getStatus());
        assertThat(captor.getValue().getToStatus()).isEqualTo(WorkOrderStatusEnum.CANCELLED.getStatus());
        assertThat(captor.getValue().getChangeReason()).isEqualTo("重复创建");
        verify(workOrderMaterialRepository).logicDeleteByWorkOrderId(WORK_ORDER_ID);
        verify(workOrderCache).evictAfterCommit(WORK_ORDER_ID);
    }

    @Test
    @DisplayName("作废工单：已完工状态 CAS 未命中，抛状态不允许作废异常")
    void cancelWorkOrderRejectsFinishedStatus() {
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildWorkOrder(WorkOrderStatusEnum.FINISHED.getStatus())));
        when(workOrderRepository.updateStatus(any(), any(), any())).thenReturn(0);

        assertThatThrownBy(() -> workOrderService.cancelWorkOrder(WORK_ORDER_ID, "误操作"))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_CANCEL));
        verify(workOrderMaterialRepository, never()).logicDeleteByWorkOrderId(any());
    }

    @Test
    @DisplayName("查询物料需求：按物料档案回填编码与名称")
    void getWorkOrderMaterialsFillsMaterialInfo() {
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildWorkOrder(WorkOrderStatusEnum.RELEASED.getStatus())));
        WorkOrderMaterialEntity requirement = new WorkOrderMaterialEntity();
        requirement.setId(1L);
        requirement.setWorkOrderId(WORK_ORDER_ID);
        requirement.setMaterialId(MATERIAL_ID);
        requirement.setRequireQuantity(new BigDecimal("16800.0000"));
        when(workOrderMaterialRepository.findByWorkOrderIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(List.of(requirement));
        MaterialEntity material = new MaterialEntity();
        material.setId(MATERIAL_ID);
        material.setMaterialCode("M002");
        material.setMaterialName("鹅毛羽片");
        when(materialRepository.findByIdInAndDeletedFalse(List.of(MATERIAL_ID))).thenReturn(List.of(material));

        List<WorkOrderMaterialRespVO> result = workOrderService.getWorkOrderMaterials(WORK_ORDER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMaterialCode()).isEqualTo("M002");
        assertThat(result.get(0).getMaterialName()).isEqualTo("鹅毛羽片");
    }

    @Test
    @DisplayName("查询物料需求：未生成时返回空集合且不查物料档案")
    void getWorkOrderMaterialsReturnsEmptyWhenNoneGenerated() {
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildWorkOrder(WorkOrderStatusEnum.CREATED.getStatus())));
        when(workOrderMaterialRepository.findByWorkOrderIdAndDeletedFalse(WORK_ORDER_ID)).thenReturn(List.of());

        assertThat(workOrderService.getWorkOrderMaterials(WORK_ORDER_ID)).isEmpty();
        verify(materialRepository, never()).findByIdInAndDeletedFalse(any());
    }

    @Test
    @DisplayName("查询状态日志：按最新在前返回")
    void getWorkOrderStatusLogsReturnsList() {
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildWorkOrder(WorkOrderStatusEnum.RELEASED.getStatus())));
        WorkOrderStatusLogEntity statusLog = new WorkOrderStatusLogEntity();
        statusLog.setId(1L);
        statusLog.setWorkOrderId(WORK_ORDER_ID);
        statusLog.setFromStatus(WorkOrderStatusEnum.CREATED.getStatus());
        statusLog.setToStatus(WorkOrderStatusEnum.RELEASED.getStatus());
        statusLog.setChangeType(WorkOrderChangeTypeEnum.STATUS_TRANSITION.getType());
        when(workOrderStatusLogRepository.findByWorkOrderIdAndDeletedFalseOrderByIdDesc(WORK_ORDER_ID))
                .thenReturn(List.of(statusLog));

        assertThat(workOrderService.getWorkOrderStatusLogs(WORK_ORDER_ID)).hasSize(1)
                .first().satisfies(respVO -> {
                    assertThat(respVO.getToStatus()).isEqualTo(WorkOrderStatusEnum.RELEASED.getStatus());
                    assertThat(respVO.getChangeType())
                            .isEqualTo(WorkOrderChangeTypeEnum.STATUS_TRANSITION.getType());
                });
    }

    /**
     * 准备可下达工单及其生效路线引用。
     */
    private void stubReleasableWorkOrder() {
        when(workOrderRepository.findByIdForUpdate(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildWorkOrder(WorkOrderStatusEnum.CREATED.getStatus())));
        when(routeRepository.findByIdAndDeletedFalseForUpdate(ROUTE_ID))
                .thenReturn(Optional.of(buildRoute(CraftRouteStatusEnum.EFFECTIVE)));
        when(routeProductRepository.existsByRouteIdAndProductIdAndDeletedFalse(ROUTE_ID, PRODUCT_ID))
                .thenReturn(true);
    }

    /**
     * 构造指定状态的工艺路线。
     *
     * @param status 路线状态
     * @return 路线实体
     */
    private CraftRouteEntity buildRoute(CraftRouteStatusEnum status) {
        CraftRouteEntity route = new CraftRouteEntity();
        route.setId(ROUTE_ID);
        route.setRoutingStatus(status.getStatus());
        route.setDeleted(false);
        return route;
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
        workOrder.setRoutingId(ROUTE_ID);
        workOrder.setWorkshopId(WORKSHOP_ID);
        workOrder.setPlanQuantity(1000);
        workOrder.setPlanStartTime(LocalDateTime.of(2026, 7, 10, 8, 0, 0));
        workOrder.setPlanEndTime(LocalDateTime.of(2026, 7, 15, 18, 0, 0));
        workOrder.setOrderStatus(orderStatus);
        workOrder.setDeleted(false);
        return workOrder;
    }

    /**
     * 构造指定状态的 BOM。
     *
     * @param bomStatus BOM 状态值
     */
    private BomEntity buildBom(Integer bomStatus) {
        BomEntity bom = new BomEntity();
        bom.setId(BOM_ID);
        bom.setBomCode("BOM-P001-V1");
        bom.setProductId(PRODUCT_ID);
        bom.setVersion("V1.0");
        bom.setBomStatus(bomStatus);
        bom.setDeleted(false);
        return bom;
    }

    /**
     * 构造 BOM 明细：单位用量 16、损耗率 5%。
     */
    private BomDetailEntity buildBomDetail() {
        BomDetailEntity detail = new BomDetailEntity();
        detail.setId(1L);
        detail.setBomId(BOM_ID);
        detail.setMaterialId(MATERIAL_ID);
        detail.setQuantity(new BigDecimal("16.0000"));
        detail.setLossRate(new BigDecimal("5.00"));
        detail.setDeleted(false);
        return detail;
    }

    @SuppressWarnings("unchecked")
    private static Specification<WorkOrderEntity> anyWorkOrderSpecification() {
        return (Specification<WorkOrderEntity>) any(Specification.class);
    }
}
