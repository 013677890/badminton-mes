package com.badminton.mes.module.production.service.impl;

import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.controller.vo.ProductionLinePageReqVO;
import com.badminton.mes.module.production.controller.vo.ProductionLineSaveReqVO;
import com.badminton.mes.module.production.controller.vo.ProductionLineUpdateReqVO;
import com.badminton.mes.module.production.controller.vo.ProductionStatusReqVO;
import com.badminton.mes.module.production.dal.entity.ProductionLineEntity;
import com.badminton.mes.module.production.dal.entity.WorkshopEntity;
import com.badminton.mes.module.production.dal.repository.DispatchOrderRepository;
import com.badminton.mes.module.production.dal.repository.ProductionLineRepository;
import com.badminton.mes.module.production.dal.repository.WorkshopRepository;
import com.badminton.mes.module.production.enums.DispatchStatusEnum;
import com.badminton.mes.module.system.service.OrganizationUserReferenceQuery;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ProductionLineServiceImpl} 单元测试。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
@ExtendWith(MockitoExtension.class)
class ProductionLineServiceImplTest {

    private static final Long LINE_ID = 20L;

    private static final Long WORKSHOP_ID = 10L;

    @Mock
    private ProductionLineRepository productionLineRepository;

    @Mock
    private WorkshopRepository workshopRepository;

    @Mock
    private DispatchOrderRepository dispatchOrderRepository;

    @Mock
    private OrganizationUserReferenceQuery userReferenceQuery;

    private ProductionLineServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductionLineServiceImpl(productionLineRepository, workshopRepository,
                dispatchOrderRepository, userReferenceQuery);
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(9L);
        SecurityContextHolder.set("production-line-test", loginUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    @DisplayName("创建产线：锁定启用车间、规范化编码并记录审计人")
    void createProductionLineLocksWorkshopAndNormalizes() {
        ProductionLineSaveReqVO request = buildSaveRequest(WORKSHOP_ID);
        when(workshopRepository.findByIdAndDeletedFalseForUpdate(WORKSHOP_ID))
                .thenReturn(Optional.of(buildWorkshop(1)));
        when(productionLineRepository.saveAndFlush(any(ProductionLineEntity.class)))
                .thenAnswer(invocation -> {
                    ProductionLineEntity entity = invocation.getArgument(0);
                    entity.setId(LINE_ID);
                    return entity;
                });

        Long result = service.createProductionLine(request);

        assertThat(result).isEqualTo(LINE_ID);
        ArgumentCaptor<ProductionLineEntity> captor =
                ArgumentCaptor.forClass(ProductionLineEntity.class);
        verify(productionLineRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getLineCode()).isEqualTo("LINE-A");
        assertThat(captor.getValue().getLineName()).isEqualTo("一号产线");
        assertThat(captor.getValue().getCreateBy()).isEqualTo(9L);
    }

    @Test
    @DisplayName("修改产线：创建后禁止变更所属车间")
    void updateProductionLineRejectsWorkshopChange() {
        ProductionLineEntity line = buildLine(1, 0);
        stubLockedLine(line, buildWorkshop(1));
        ProductionLineUpdateReqVO request = new ProductionLineUpdateReqVO();
        request.setLineCode("LINE-A");
        request.setLineName("一号产线");
        request.setWorkshopId(99L);
        request.setStandardCapacity(6000);
        request.setStatus(1);
        request.setVersion(0);

        assertThatThrownBy(() -> service.updateProductionLine(LINE_ID, request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                ProductionErrorCodeConstants.PRODUCTION_LINE_WORKSHOP_IMMUTABLE));
        verify(productionLineRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("停用产线：存在未结束派工时拒绝")
    void disableProductionLineRejectsActiveDispatch() {
        ProductionLineEntity line = buildLine(1, 0);
        stubLockedLine(line, buildWorkshop(1));
        when(dispatchOrderRepository.existsByLineIdAndDispatchStatusInAndDeletedFalse(
                LINE_ID, DispatchStatusEnum.activeStatuses())).thenReturn(true);
        ProductionStatusReqVO request = new ProductionStatusReqVO();
        request.setVersion(0);
        request.setStatus(0);

        assertThatThrownBy(() -> service.updateProductionLineStatus(LINE_ID, request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                ProductionErrorCodeConstants.PRODUCTION_LINE_ACTIVE_REFERENCE_EXISTS));
    }

    @Test
    @DisplayName("删除产线：无历史引用时逻辑删除并记录修改人")
    void deleteProductionLineLogicDeletesWhenUnreferenced() {
        ProductionLineEntity line = buildLine(0, 0);
        stubLockedLine(line, buildWorkshop(1));

        service.deleteProductionLine(LINE_ID, 0);

        assertThat(line.getDeleted()).isTrue();
        assertThat(line.getUpdateBy()).isEqualTo(9L);
        verify(productionLineRepository).saveAndFlush(line);
    }

    @Test
    @DisplayName("产线分页：count 为零时不执行列表查询")
    void getProductionLinePageShortCircuitsWhenEmpty() {
        ProductionLinePageReqVO request = new ProductionLinePageReqVO();
        request.setPageNo(1);
        request.setPageSize(10);
        when(productionLineRepository.count(any(Specification.class))).thenReturn(0L);

        var result = service.getProductionLinePage(request);

        assertThat(result.getList()).isEmpty();
        assertThat(result.getTotal()).isZero();
        verify(productionLineRepository, never()).findAll(
                any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("创建产线：唯一键冲突时兜底转换为编码重复异常")
    void createProductionLineTranslatesUniqueConstraintViolation() {
        ProductionLineSaveReqVO request = buildSaveRequest(WORKSHOP_ID);
        when(productionLineRepository.existsByLineCodeAndDeletedFalse("LINE-A")).thenReturn(false);
        when(workshopRepository.findByIdAndDeletedFalseForUpdate(WORKSHOP_ID))
                .thenReturn(Optional.of(buildWorkshop(1)));
        when(productionLineRepository.saveAndFlush(any(ProductionLineEntity.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Duplicate entry for key 'uk_active_line_code'"));

        assertThatThrownBy(() -> service.createProductionLine(request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                ProductionErrorCodeConstants.PRODUCTION_LINE_CODE_DUPLICATE));
    }

    @Test
    @DisplayName("创建产线：乐观锁冲突时兜底转换为并发修改异常")
    void createProductionLineTranslatesOptimisticLockFailure() {
        ProductionLineSaveReqVO request = buildSaveRequest(WORKSHOP_ID);
        when(productionLineRepository.existsByLineCodeAndDeletedFalse("LINE-A")).thenReturn(false);
        when(workshopRepository.findByIdAndDeletedFalseForUpdate(WORKSHOP_ID))
                .thenReturn(Optional.of(buildWorkshop(1)));
        when(productionLineRepository.saveAndFlush(any(ProductionLineEntity.class)))
                .thenThrow(new OptimisticLockingFailureException("Row was updated or deleted"));

        assertThatThrownBy(() -> service.createProductionLine(request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                ProductionErrorCodeConstants.PRODUCTION_LINE_CONCURRENT_MODIFICATION));
    }

    @Test
    @DisplayName("创建产线：车间不存在时拒绝")
    void createProductionLineRejectsWorkshopNotFound() {
        ProductionLineSaveReqVO request = buildSaveRequest(WORKSHOP_ID);
        when(productionLineRepository.existsByLineCodeAndDeletedFalse("LINE-A")).thenReturn(false);
        when(workshopRepository.findByIdAndDeletedFalseForUpdate(WORKSHOP_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createProductionLine(request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                ProductionErrorCodeConstants.PRODUCTION_LINE_WORKSHOP_NOT_AVAILABLE));
        verify(productionLineRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("创建产线：车间已停用时拒绝")
    void createProductionLineRejectsDisabledWorkshop() {
        ProductionLineSaveReqVO request = buildSaveRequest(WORKSHOP_ID);
        when(productionLineRepository.existsByLineCodeAndDeletedFalse("LINE-A")).thenReturn(false);
        when(workshopRepository.findByIdAndDeletedFalseForUpdate(WORKSHOP_ID))
                .thenReturn(Optional.of(buildWorkshop(0)));

        assertThatThrownBy(() -> service.createProductionLine(request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                ProductionErrorCodeConstants.PRODUCTION_LINE_WORKSHOP_NOT_AVAILABLE));
        verify(productionLineRepository, never()).saveAndFlush(any());
    }

    /** 构造产线保存请求。 */
    private ProductionLineSaveReqVO buildSaveRequest(Long workshopId) {
        ProductionLineSaveReqVO request = new ProductionLineSaveReqVO();
        request.setLineCode(" line-a ");
        request.setLineName(" 一号产线 ");
        request.setWorkshopId(workshopId);
        request.setStandardCapacity(5000);
        request.setStatus(1);
        return request;
    }

    /** 构造产线实体。 */
    private ProductionLineEntity buildLine(Integer status, Integer version) {
        ProductionLineEntity line = new ProductionLineEntity();
        line.setId(LINE_ID);
        line.setLineCode("LINE-A");
        line.setLineName("一号产线");
        line.setWorkshopId(WORKSHOP_ID);
        line.setStandardCapacity(5000);
        line.setStatus(status);
        line.setVersion(version);
        line.setDeleted(false);
        return line;
    }

    /** 构造车间实体。 */
    private WorkshopEntity buildWorkshop(Integer status) {
        WorkshopEntity workshop = new WorkshopEntity();
        workshop.setId(WORKSHOP_ID);
        workshop.setWorkshopCode("WS-A");
        workshop.setWorkshopName("成型车间");
        workshop.setStatus(status);
        workshop.setDeleted(false);
        return workshop;
    }

    /** 模拟按车间、产线顺序取得写锁。 */
    private void stubLockedLine(ProductionLineEntity line, WorkshopEntity workshop) {
        when(productionLineRepository.findWorkshopIdById(LINE_ID))
                .thenReturn(Optional.of(WORKSHOP_ID));
        when(workshopRepository.findByIdAndDeletedFalseForUpdate(WORKSHOP_ID))
                .thenReturn(Optional.of(workshop));
        when(productionLineRepository.findByIdAndDeletedFalseForUpdate(LINE_ID))
                .thenReturn(Optional.of(line));
    }
}
