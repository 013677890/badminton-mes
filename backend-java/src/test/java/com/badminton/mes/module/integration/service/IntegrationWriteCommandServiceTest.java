package com.badminton.mes.module.integration.service;

import java.time.LocalDateTime;
import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.craft.dal.entity.CraftRouteEntity;
import com.badminton.mes.module.craft.dal.repository.CraftRouteProductRepository;
import com.badminton.mes.module.craft.dal.repository.CraftRouteRepository;
import com.badminton.mes.module.craft.enums.CraftRouteStatusEnum;
import com.badminton.mes.module.integration.constants.IntegrationErrorCodeConstants;
import com.badminton.mes.module.integration.controller.vo.ExternalWorkOrderWriteReqVO;
import com.badminton.mes.module.integration.controller.vo.UnitWriteReqVO;
import com.badminton.mes.module.integration.dal.entity.UnitEntity;
import com.badminton.mes.module.integration.dal.repository.UnitRepository;
import com.badminton.mes.module.integration.enums.IntegrationInterfaceTypeEnum;
import com.badminton.mes.module.integration.enums.IntegrationWriteStatusEnum;
import com.badminton.mes.module.integration.service.dto.IntegrationCommandResult;
import com.badminton.mes.module.production.dal.entity.BomEntity;
import com.badminton.mes.module.production.dal.entity.ProductEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;
import com.badminton.mes.module.production.dal.entity.WorkshopEntity;
import com.badminton.mes.module.production.dal.redis.WorkOrderNoSequence;
import com.badminton.mes.module.production.dal.repository.BomRepository;
import com.badminton.mes.module.production.dal.repository.ProductRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.production.dal.repository.WorkshopRepository;
import com.badminton.mes.module.production.enums.BomStatusEnum;
import com.badminton.mes.module.production.enums.WorkOrderSourceTypeEnum;
import com.badminton.mes.module.production.enums.WorkOrderStatusEnum;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link IntegrationWriteCommandService} 单元测试。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
@ExtendWith(MockitoExtension.class)
class IntegrationWriteCommandServiceTest {

    private static final Long OPERATOR_ID = 9L;

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WorkshopRepository workshopRepository;

    @Mock
    private BomRepository bomRepository;

    @Mock
    private CraftRouteRepository routeRepository;

    @Mock
    private CraftRouteProductRepository routeProductRepository;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private WorkOrderNoSequence workOrderNoSequence;

    @Mock
    private IntegrationAuditService auditService;

    private IntegrationWriteCommandService commandService;

    @BeforeEach
    void setUp() {
        commandService = new IntegrationWriteCommandService(
                unitRepository, productRepository, workshopRepository, bomRepository,
                routeRepository, routeProductRepository, workOrderRepository,
                workOrderNoSequence, auditService);
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(OPERATOR_ID);
        SecurityContextHolder.set("integration-test-token", loginUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    @DisplayName("单位写入：新编码规范化后新增并与成功日志一起返回")
    void writeUnitCreatesNormalizedUnit() {
        UnitWriteReqVO reqVO = buildUnitReqVO();
        reqVO.setSourceSystem(" erp-main ");
        reqVO.setUnitCode(" kg ");
        when(unitRepository.findByUnitCodeForUpdate("KG")).thenReturn(Optional.empty());
        doAnswer(invocation -> {
            UnitEntity entity = invocation.getArgument(0);
            entity.setId(10L);
            return entity;
        }).when(unitRepository).saveAndFlush(any(UnitEntity.class));
        when(auditService.recordResult(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(100L);

        IntegrationCommandResult result = commandService.writeUnit(reqVO, "{}");

        assertThat(result.businessId()).isEqualTo(10L);
        assertThat(result.logId()).isEqualTo(100L);
        ArgumentCaptor<UnitEntity> captor = ArgumentCaptor.forClass(UnitEntity.class);
        verify(unitRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUnitCode()).isEqualTo("KG");
        assertThat(captor.getValue().getCreateBy()).isEqualTo(OPERATOR_ID);
        verify(auditService).recordResult(
                IntegrationInterfaceTypeEnum.UNIT_WRITE, "ERP-MAIN", "KG", "{}",
                IntegrationWriteStatusEnum.SUCCESS, 10L, "KG");
    }

    @Test
    @DisplayName("单位写入：已被产品引用时拒绝修改小数精度")
    void writeUnitRejectsPrecisionChangeWhenReferenced() {
        UnitEntity unit = new UnitEntity();
        unit.setId(10L);
        unit.setUnitCode("PCS");
        unit.setDecimalPrecision(0);
        when(unitRepository.findByUnitCodeForUpdate("PCS")).thenReturn(Optional.of(unit));
        when(productRepository.existsByUnitIdAndDeletedFalse(10L)).thenReturn(true);
        UnitWriteReqVO reqVO = buildUnitReqVO();
        reqVO.setDecimalPrecision(2);

        assertThatThrownBy(() -> commandService.writeUnit(reqVO, "{}"))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                IntegrationErrorCodeConstants.UNIT_PRECISION_IN_USE));
        verify(unitRepository, never()).saveAndFlush(any());
        verify(auditService, never()).recordResult(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("外部工单：来源幂等键已存在时返回原工单并记录重复")
    void writeWorkOrderReturnsExistingForDuplicateRequest() {
        WorkOrderEntity existing = new WorkOrderEntity();
        existing.setId(50L);
        existing.setWorkOrderNo("WO202607110001");
        when(workOrderRepository.findBySourceTypeAndSourceSystemAndSourceOrderNo(
                WorkOrderSourceTypeEnum.API_WRITE.getType(), "ERP-MAIN", "ERP-WO-001"))
                .thenReturn(Optional.of(existing));
        when(auditService.recordResult(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(101L);

        IntegrationCommandResult result = commandService.writeWorkOrder(buildWorkOrderReqVO(), "{}");

        assertThat(result.duplicate()).isTrue();
        assertThat(result.businessId()).isEqualTo(50L);
        verify(workOrderNoSequence, never()).nextNo();
        verify(productRepository, never()).findByProductCodeAndDeletedFalse(any());
        verify(auditService).recordResult(
                IntegrationInterfaceTypeEnum.WORK_ORDER_WRITE,
                "ERP-MAIN", "ERP-WO-001", "{}",
                IntegrationWriteStatusEnum.DUPLICATE, 50L, "WO202607110001");
    }

    @Test
    @DisplayName("外部工单：主数据完整时生成 API 来源工单和成功日志")
    void writeWorkOrderCreatesApiWorkOrder() {
        ExternalWorkOrderWriteReqVO reqVO = buildWorkOrderReqVO();
        when(workOrderRepository.findBySourceTypeAndSourceSystemAndSourceOrderNo(
                WorkOrderSourceTypeEnum.API_WRITE.getType(), "ERP-MAIN", "ERP-WO-001"))
                .thenReturn(Optional.empty());
        ProductEntity product = buildProduct();
        WorkshopEntity workshop = buildWorkshop();
        BomEntity bom = buildBom();
        CraftRouteEntity route = buildRoute();
        when(productRepository.findByProductCodeAndDeletedFalse("P001"))
                .thenReturn(Optional.of(product));
        UnitEntity unit = new UnitEntity();
        unit.setId(1L);
        unit.setStatus(1);
        when(unitRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(unit));
        when(workshopRepository.findByWorkshopCodeAndDeletedFalse("WS001"))
                .thenReturn(Optional.of(workshop));
        when(bomRepository.findByIdAndDeletedFalse(30L)).thenReturn(Optional.of(bom));
        when(routeRepository.findByIdAndDeletedFalse(40L)).thenReturn(Optional.of(route));
        when(routeProductRepository.existsByRouteIdAndProductIdAndDeletedFalse(40L, 20L))
                .thenReturn(true);
        when(workOrderNoSequence.nextNo()).thenReturn("WO202607110002");
        doAnswer(invocation -> {
            WorkOrderEntity entity = invocation.getArgument(0);
            entity.setId(60L);
            return entity;
        }).when(workOrderRepository).saveAndFlush(any(WorkOrderEntity.class));
        when(auditService.recordResult(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(102L);

        IntegrationCommandResult result = commandService.writeWorkOrder(reqVO, "{}");

        assertThat(result.duplicate()).isFalse();
        ArgumentCaptor<WorkOrderEntity> captor = ArgumentCaptor.forClass(WorkOrderEntity.class);
        verify(workOrderRepository).saveAndFlush(captor.capture());
        WorkOrderEntity saved = captor.getValue();
        assertThat(saved.getSourceType()).isEqualTo(WorkOrderSourceTypeEnum.API_WRITE.getType());
        assertThat(saved.getSourceSystem()).isEqualTo("ERP-MAIN");
        assertThat(saved.getSourceOrderNo()).isEqualTo("ERP-WO-001");
        assertThat(saved.getProductName()).isEqualTo("比赛级羽毛球");
        assertThat(saved.getOrderStatus()).isEqualTo(WorkOrderStatusEnum.CREATED.getStatus());
        assertThat(saved.getCreateBy()).isEqualTo(OPERATOR_ID);
        verify(auditService).recordResult(eq(IntegrationInterfaceTypeEnum.WORK_ORDER_WRITE),
                eq("ERP-MAIN"), eq("ERP-WO-001"), eq("{}"),
                eq(IntegrationWriteStatusEnum.SUCCESS), eq(60L), eq("WO202607110002"));
    }

    @Test
    @DisplayName("外部工单：并发穿透前置查询后由来源唯一键识别重复")
    void writeWorkOrderTranslatesConcurrentSourceConstraint() {
        ExternalWorkOrderWriteReqVO reqVO = buildWorkOrderReqVO();
        when(workOrderRepository.findBySourceTypeAndSourceSystemAndSourceOrderNo(
                WorkOrderSourceTypeEnum.API_WRITE.getType(), "ERP-MAIN", "ERP-WO-001"))
                .thenReturn(Optional.empty());
        ProductEntity product = buildProduct();
        when(productRepository.findByProductCodeAndDeletedFalse("P001"))
                .thenReturn(Optional.of(product));
        UnitEntity unit = new UnitEntity();
        unit.setId(1L);
        unit.setStatus(1);
        when(unitRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(unit));
        when(workshopRepository.findByWorkshopCodeAndDeletedFalse("WS001"))
                .thenReturn(Optional.of(buildWorkshop()));
        when(bomRepository.findByIdAndDeletedFalse(30L)).thenReturn(Optional.of(buildBom()));
        when(routeRepository.findByIdAndDeletedFalse(40L)).thenReturn(Optional.of(buildRoute()));
        when(routeProductRepository.existsByRouteIdAndProductIdAndDeletedFalse(40L, 20L))
                .thenReturn(true);
        when(workOrderNoSequence.nextNo()).thenReturn("WO202607110003");
        when(workOrderRepository.saveAndFlush(any(WorkOrderEntity.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Duplicate entry for key uk_external_source_order"));

        assertThatThrownBy(() -> commandService.writeWorkOrder(reqVO, "{}"))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                IntegrationErrorCodeConstants.EXTERNAL_WORK_ORDER_DUPLICATE));
        verify(auditService, never()).recordResult(any(), any(), any(), any(), any(), any(), any());
    }

    private UnitWriteReqVO buildUnitReqVO() {
        UnitWriteReqVO reqVO = new UnitWriteReqVO();
        reqVO.setSourceSystem("ERP-MAIN");
        reqVO.setUnitCode("PCS");
        reqVO.setUnitName("个");
        reqVO.setDecimalPrecision(0);
        reqVO.setStatus(1);
        return reqVO;
    }

    private ExternalWorkOrderWriteReqVO buildWorkOrderReqVO() {
        ExternalWorkOrderWriteReqVO reqVO = new ExternalWorkOrderWriteReqVO();
        reqVO.setSourceSystem("ERP-MAIN");
        reqVO.setExternalWorkOrderNo("ERP-WO-001");
        reqVO.setProductCode("P001");
        reqVO.setWorkshopCode("WS001");
        reqVO.setBomId(30L);
        reqVO.setRoutingId(40L);
        reqVO.setPlanQuantity(1000);
        reqVO.setPlanStartTime(LocalDateTime.of(2026, 7, 12, 8, 0));
        reqVO.setPlanEndTime(LocalDateTime.of(2026, 7, 15, 18, 0));
        return reqVO;
    }

    private ProductEntity buildProduct() {
        ProductEntity product = new ProductEntity();
        product.setId(20L);
        product.setProductCode("P001");
        product.setProductName("比赛级羽毛球");
        product.setSpec("77速 鹅毛");
        product.setUnitId(1L);
        product.setStatus(1);
        return product;
    }

    private WorkshopEntity buildWorkshop() {
        WorkshopEntity workshop = new WorkshopEntity();
        workshop.setId(25L);
        workshop.setWorkshopCode("WS001");
        workshop.setStatus(1);
        return workshop;
    }

    private BomEntity buildBom() {
        BomEntity bom = new BomEntity();
        bom.setId(30L);
        bom.setProductId(20L);
        bom.setBomStatus(BomStatusEnum.EFFECTIVE.getStatus());
        return bom;
    }

    private CraftRouteEntity buildRoute() {
        CraftRouteEntity route = new CraftRouteEntity();
        route.setId(40L);
        route.setRoutingStatus(CraftRouteStatusEnum.EFFECTIVE.getStatus());
        return route;
    }
}
