package com.badminton.mes.module.production.service.impl;

import java.util.Optional;

import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.craft.service.ProductRouteReferenceQuery;
import com.badminton.mes.module.integration.service.UnitService;
import com.badminton.mes.module.production.controller.vo.ProductionStatusReqVO;
import com.badminton.mes.module.production.dal.entity.ProductEntity;
import com.badminton.mes.module.production.dal.repository.BomRepository;
import com.badminton.mes.module.production.dal.repository.ProductRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.production.enums.BomStatusEnum;
import com.badminton.mes.module.production.enums.WorkOrderStatusEnum;
import com.badminton.mes.module.wage.service.ProductPieceRateRuleReferenceQuery;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ProductServiceImpl} 跨模块查询契约与活动状态测试。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    private static final Long PRODUCT_ID = 10L;

    @Mock
    private ProductRepository productRepository;
    @Mock
    private UnitService unitService;
    @Mock
    private WorkOrderRepository workOrderRepository;
    @Mock
    private BomRepository bomRepository;
    @Mock
    private ProductRouteReferenceQuery routeReferenceQuery;
    @Mock
    private ProductPieceRateRuleReferenceQuery pieceRateRuleReferenceQuery;

    private ProductServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductServiceImpl(productRepository, unitService,
                workOrderRepository, bomRepository, routeReferenceQuery,
                pieceRateRuleReferenceQuery);
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(9L);
        SecurityContextHolder.set("product-service-test", loginUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    @DisplayName("停用产品：通过模块 Service 契约检查引用并使用枚举活动状态集合")
    void disableProductUsesServiceContractsAndActiveStatuses() {
        ProductEntity product = new ProductEntity();
        product.setId(PRODUCT_ID);
        product.setStatus(1);
        product.setVersion(0);
        when(productRepository.findByIdAndDeletedFalseForUpdate(PRODUCT_ID))
                .thenReturn(Optional.of(product));
        ProductionStatusReqVO request = new ProductionStatusReqVO();
        request.setVersion(0);
        request.setStatus(0);

        service.updateProductStatus(PRODUCT_ID, request);

        assertThat(product.getStatus()).isZero();
        verify(workOrderRepository).existsByProductIdAndOrderStatusInAndDeletedFalse(
                PRODUCT_ID, WorkOrderStatusEnum.activeStatuses());
        verify(bomRepository).existsByProductIdAndBomStatusAndDeletedFalse(
                PRODUCT_ID, BomStatusEnum.EFFECTIVE.getStatus());
        verify(routeReferenceQuery).hasEffectiveRoute(PRODUCT_ID);
        verify(pieceRateRuleReferenceQuery).hasEnabledRule(PRODUCT_ID);
        verify(productRepository).saveAndFlush(product);
    }
}
