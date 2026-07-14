package com.badminton.mes.module.production.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.controller.vo.BomActionReqVO;
import com.badminton.mes.module.production.controller.vo.BomDetailSaveReqVO;
import com.badminton.mes.module.production.controller.vo.BomNewVersionReqVO;
import com.badminton.mes.module.production.controller.vo.BomSaveReqVO;
import com.badminton.mes.module.production.dal.entity.BomDetailEntity;
import com.badminton.mes.module.production.dal.entity.BomEntity;
import com.badminton.mes.module.production.dal.entity.ProductEntity;
import com.badminton.mes.module.production.dal.repository.BomRepository;
import com.badminton.mes.module.production.dal.repository.ProductRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.production.enums.BomStatusEnum;
import com.badminton.mes.module.production.service.support.BomDetailManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link BomServiceImpl} 状态机、锁顺序与异常契约测试。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
@ExtendWith(MockitoExtension.class)
class BomServiceImplTest {

    private static final Long BOM_ID = 100L;
    private static final Long OLD_BOM_ID = 90L;
    private static final Long PRODUCT_ID = 10L;

    @Mock
    private BomRepository bomRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private WorkOrderRepository workOrderRepository;
    @Mock
    private BomDetailManager detailManager;

    private BomServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BomServiceImpl(bomRepository, productRepository,
                workOrderRepository, detailManager);
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(9L);
        SecurityContextHolder.set("bom-service-test", loginUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    @DisplayName("生效 BOM：按产品、BOM、物料顺序加锁并停用旧生效版本")
    void activateBomSwitchesEffectiveVersionWithStableLockOrder() {
        BomEntity target = buildBom(BOM_ID, BomStatusEnum.DRAFT, 0);
        BomEntity oldEffective = buildBom(OLD_BOM_ID, BomStatusEnum.EFFECTIVE, 3);
        ProductEntity product = buildEnabledProduct();
        List<BomEntity> productBoms = List.of(oldEffective, target);
        List<BomDetailEntity> details = List.of(buildDetail());
        when(bomRepository.findByIdAndDeletedFalse(BOM_ID)).thenReturn(Optional.of(target));
        when(productRepository.findByIdAndDeletedFalseForUpdate(PRODUCT_ID))
                .thenReturn(Optional.of(product));
        when(bomRepository.findByProductIdForUpdateOrderByIdAsc(PRODUCT_ID))
                .thenReturn(productBoms);
        when(detailManager.getActiveDetails(BOM_ID)).thenReturn(details);

        service.activateBom(BOM_ID, actionRequest(0));

        assertThat(target.getBomStatus()).isEqualTo(BomStatusEnum.EFFECTIVE.getStatus());
        assertThat(oldEffective.getBomStatus()).isEqualTo(BomStatusEnum.DISABLED.getStatus());
        InOrder lockOrder = inOrder(productRepository, bomRepository, detailManager);
        lockOrder.verify(productRepository).findByIdAndDeletedFalseForUpdate(PRODUCT_ID);
        lockOrder.verify(bomRepository).findByProductIdForUpdateOrderByIdAsc(PRODUCT_ID);
        lockOrder.verify(detailManager).validateAndLockExisting(details);
        verify(bomRepository).saveAllAndFlush(productBoms);
    }

    @Test
    @DisplayName("生效 BOM：客户端锁版本陈旧时拒绝状态切换")
    void activateBomRejectsStaleLockVersion() {
        BomEntity target = buildBom(BOM_ID, BomStatusEnum.DRAFT, 2);
        when(bomRepository.findByIdAndDeletedFalse(BOM_ID)).thenReturn(Optional.of(target));
        when(productRepository.findByIdAndDeletedFalseForUpdate(PRODUCT_ID))
                .thenReturn(Optional.of(buildEnabledProduct()));
        when(bomRepository.findByProductIdForUpdateOrderByIdAsc(PRODUCT_ID))
                .thenReturn(List.of(target));

        assertThatThrownBy(() -> service.activateBom(BOM_ID, actionRequest(1)))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                ProductionErrorCodeConstants.BOM_CONCURRENT_MODIFICATION));
        verify(detailManager, never()).getActiveDetails(any());
        verify(bomRepository, never()).saveAllAndFlush(any());
    }

    @Test
    @DisplayName("停用 BOM：草稿状态不允许停用")
    void disableBomRejectsDraft() {
        BomEntity draft = buildBom(BOM_ID, BomStatusEnum.DRAFT, 0);
        when(bomRepository.findByIdAndDeletedFalseForUpdate(BOM_ID))
                .thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.disableBom(BOM_ID, actionRequest(0)))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                ProductionErrorCodeConstants.BOM_STATUS_INVALID));
        verify(bomRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("创建新版本：草稿来源不允许再次克隆")
    void createNewVersionRejectsDraftSource() {
        BomEntity draft = buildBom(BOM_ID, BomStatusEnum.DRAFT, 0);
        when(bomRepository.findByIdAndDeletedFalse(BOM_ID)).thenReturn(Optional.of(draft));
        when(productRepository.findByIdAndDeletedFalseForUpdate(PRODUCT_ID))
                .thenReturn(Optional.of(buildEnabledProduct()));
        when(bomRepository.findByProductIdForUpdateOrderByIdAsc(PRODUCT_ID))
                .thenReturn(List.of(draft));
        BomNewVersionReqVO request = new BomNewVersionReqVO();
        request.setLockVersion(0);
        request.setBomCode("BOM-P001-V2");
        request.setVersion("V2.0");

        assertThatThrownBy(() -> service.createNewVersion(BOM_ID, request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                ProductionErrorCodeConstants.BOM_STATUS_INVALID));
        verify(detailManager, never()).copyTo(any(), any());
    }

    @Test
    @DisplayName("创建 BOM：数据库编码唯一约束竞争转换为稳定业务错误")
    void createBomTranslatesCodeConstraintViolation() {
        when(productRepository.findByIdAndDeletedFalseForUpdate(PRODUCT_ID))
                .thenReturn(Optional.of(buildEnabledProduct()));
        DataIntegrityViolationException conflict = new DataIntegrityViolationException(
                "insert conflict", new RuntimeException("UK_ACTIVE_BOM_CODE"));
        when(bomRepository.saveAndFlush(any(BomEntity.class))).thenThrow(conflict);

        assertThatThrownBy(() -> service.createBom(buildSaveRequest()))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                ProductionErrorCodeConstants.BOM_CODE_DUPLICATE));
        verify(detailManager, never()).insert(any(), any());
    }

    /** 构造生效/停用请求。 */
    private BomActionReqVO actionRequest(Integer lockVersion) {
        BomActionReqVO request = new BomActionReqVO();
        request.setLockVersion(lockVersion);
        return request;
    }

    /** 构造 BOM。 */
    private BomEntity buildBom(Long id, BomStatusEnum status, Integer lockVersion) {
        BomEntity bom = new BomEntity();
        bom.setId(id);
        bom.setProductId(PRODUCT_ID);
        bom.setBomStatus(status.getStatus());
        bom.setLockVersion(lockVersion);
        return bom;
    }

    /** 构造启用产品。 */
    private ProductEntity buildEnabledProduct() {
        ProductEntity product = new ProductEntity();
        product.setId(PRODUCT_ID);
        product.setStatus(1);
        return product;
    }

    /** 构造 BOM 明细。 */
    private BomDetailEntity buildDetail() {
        BomDetailEntity detail = new BomDetailEntity();
        detail.setId(1L);
        detail.setBomId(BOM_ID);
        detail.setMaterialId(20L);
        detail.setQuantity(BigDecimal.ONE);
        detail.setLossRate(BigDecimal.ZERO);
        return detail;
    }

    /** 构造合法 BOM 创建请求。 */
    private BomSaveReqVO buildSaveRequest() {
        BomDetailSaveReqVO detail = new BomDetailSaveReqVO();
        detail.setMaterialId(20L);
        detail.setQuantity(BigDecimal.ONE);
        detail.setLossRate(BigDecimal.ZERO);
        BomSaveReqVO request = new BomSaveReqVO();
        request.setBomCode("bom-p001-v1");
        request.setProductId(PRODUCT_ID);
        request.setVersion("v1.0");
        request.setDetails(List.of(detail));
        return request;
    }
}
