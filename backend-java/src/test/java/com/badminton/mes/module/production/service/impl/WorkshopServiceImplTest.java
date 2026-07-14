package com.badminton.mes.module.production.service.impl;

import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.controller.vo.ProductionStatusReqVO;
import com.badminton.mes.module.production.controller.vo.WorkshopPageReqVO;
import com.badminton.mes.module.production.controller.vo.WorkshopSaveReqVO;
import com.badminton.mes.module.production.controller.vo.WorkshopUpdateReqVO;
import com.badminton.mes.module.production.dal.entity.WorkshopEntity;
import com.badminton.mes.module.production.dal.repository.FactoryCalendarRepository;
import com.badminton.mes.module.production.dal.repository.ProductionLineRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.production.dal.repository.WorkshopRepository;
import com.badminton.mes.module.production.enums.WorkOrderStatusEnum;
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
 * {@link WorkshopServiceImpl} 单元测试。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
@ExtendWith(MockitoExtension.class)
class WorkshopServiceImplTest {

    private static final Long WORKSHOP_ID = 10L;

    @Mock
    private WorkshopRepository workshopRepository;

    @Mock
    private ProductionLineRepository productionLineRepository;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private FactoryCalendarRepository factoryCalendarRepository;

    @Mock
    private OrganizationUserReferenceQuery userReferenceQuery;

    private WorkshopServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new WorkshopServiceImpl(workshopRepository, productionLineRepository,
                workOrderRepository, factoryCalendarRepository, userReferenceQuery);
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(9L);
        SecurityContextHolder.set("workshop-test", loginUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    @DisplayName("创建车间：规范化编码名称并记录审计人")
    void createWorkshopNormalizesAndAudits() {
        WorkshopSaveReqVO request = buildSaveRequest();
        when(userReferenceQuery.isEnabledUser(8L)).thenReturn(true);
        when(workshopRepository.saveAndFlush(any(WorkshopEntity.class))).thenAnswer(invocation -> {
            WorkshopEntity entity = invocation.getArgument(0);
            entity.setId(WORKSHOP_ID);
            return entity;
        });

        Long result = service.createWorkshop(request);

        assertThat(result).isEqualTo(WORKSHOP_ID);
        ArgumentCaptor<WorkshopEntity> captor = ArgumentCaptor.forClass(WorkshopEntity.class);
        verify(workshopRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getWorkshopCode()).isEqualTo("WS-A");
        assertThat(captor.getValue().getWorkshopName()).isEqualTo("成型车间");
        assertThat(captor.getValue().getCreateBy()).isEqualTo(9L);
        assertThat(captor.getValue().getUpdateBy()).isEqualTo(9L);
    }

    @Test
    @DisplayName("停用车间：存在启用产线时拒绝")
    void disableWorkshopRejectsEnabledLine() {
        WorkshopEntity workshop = buildWorkshop(1, 0);
        when(workshopRepository.findByIdAndDeletedFalseForUpdate(WORKSHOP_ID))
                .thenReturn(Optional.of(workshop));
        when(productionLineRepository.existsByWorkshopIdAndStatusAndDeletedFalse(
                WORKSHOP_ID, 1)).thenReturn(true);
        ProductionStatusReqVO request = new ProductionStatusReqVO();
        request.setVersion(0);
        request.setStatus(0);

        assertThatThrownBy(() -> service.updateWorkshopStatus(WORKSHOP_ID, request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                ProductionErrorCodeConstants.WORKSHOP_ACTIVE_REFERENCE_EXISTS));
        verify(workshopRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("删除车间：存在工厂日历历史时拒绝")
    void deleteWorkshopRejectsFactoryCalendarReference() {
        WorkshopEntity workshop = buildWorkshop(0, 0);
        when(workshopRepository.findByIdAndDeletedFalseForUpdate(WORKSHOP_ID))
                .thenReturn(Optional.of(workshop));
        when(factoryCalendarRepository.existsByWorkshopIdAndDeletedFalse(WORKSHOP_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.deleteWorkshop(WORKSHOP_ID, 0))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                ProductionErrorCodeConstants.WORKSHOP_REFERENCE_EXISTS));
    }

    @Test
    @DisplayName("停用车间：客户端版本过期时优先返回并发错误")
    void disableWorkshopRejectsStaleVersion() {
        WorkshopEntity workshop = buildWorkshop(1, 2);
        when(workshopRepository.findByIdAndDeletedFalseForUpdate(WORKSHOP_ID))
                .thenReturn(Optional.of(workshop));
        ProductionStatusReqVO request = new ProductionStatusReqVO();
        request.setVersion(1);
        request.setStatus(0);

        assertThatThrownBy(() -> service.updateWorkshopStatus(WORKSHOP_ID, request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                ProductionErrorCodeConstants.WORKSHOP_CONCURRENT_MODIFICATION));
    }

    @Test
    @DisplayName("车间分页：count 为零时不执行列表查询")
    void getWorkshopPageShortCircuitsWhenEmpty() {
        WorkshopPageReqVO request = new WorkshopPageReqVO();
        request.setPageNo(1);
        request.setPageSize(10);
        when(workshopRepository.count(any(Specification.class))).thenReturn(0L);

        var result = service.getWorkshopPage(request);

        assertThat(result.getList()).isEmpty();
        assertThat(result.getTotal()).isZero();
        verify(workshopRepository, never()).findAll(
                any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("创建车间：唯一键冲突时兜底转换为编码重复异常")
    void createWorkshopTranslatesUniqueConstraintViolation() {
        WorkshopSaveReqVO request = buildSaveRequest();
        when(workshopRepository.existsByWorkshopCodeAndDeletedFalse("WS-A")).thenReturn(false);
        when(userReferenceQuery.isEnabledUser(8L)).thenReturn(true);
        when(workshopRepository.saveAndFlush(any(WorkshopEntity.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Duplicate entry for key 'uk_active_workshop_code'"));

        assertThatThrownBy(() -> service.createWorkshop(request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                ProductionErrorCodeConstants.WORKSHOP_CODE_DUPLICATE));
    }

    @Test
    @DisplayName("创建车间：乐观锁冲突时兜底转换为并发修改异常")
    void createWorkshopTranslatesOptimisticLockFailure() {
        WorkshopSaveReqVO request = buildSaveRequest();
        when(workshopRepository.existsByWorkshopCodeAndDeletedFalse("WS-A")).thenReturn(false);
        when(userReferenceQuery.isEnabledUser(8L)).thenReturn(true);
        when(workshopRepository.saveAndFlush(any(WorkshopEntity.class)))
                .thenThrow(new OptimisticLockingFailureException("Row was updated or deleted"));

        assertThatThrownBy(() -> service.createWorkshop(request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                ProductionErrorCodeConstants.WORKSHOP_CONCURRENT_MODIFICATION));
    }

    @Test
    @DisplayName("修改车间停用：存在未结束工单时拒绝")
    void updateWorkshopRejectsWorkOrderReference() {
        WorkshopEntity workshop = buildWorkshop(1, 0);
        when(workshopRepository.findByIdAndDeletedFalseForUpdate(WORKSHOP_ID))
                .thenReturn(Optional.of(workshop));
        when(workshopRepository.existsByWorkshopCodeAndIdNotAndDeletedFalse("WS-A", WORKSHOP_ID))
                .thenReturn(false);
        when(userReferenceQuery.isEnabledUser(8L)).thenReturn(true);
        when(productionLineRepository.existsByWorkshopIdAndStatusAndDeletedFalse(
                WORKSHOP_ID, 1)).thenReturn(false);
        when(workOrderRepository.existsByWorkshopIdAndOrderStatusInAndDeletedFalse(
                WORKSHOP_ID, WorkOrderStatusEnum.activeStatuses())).thenReturn(true);

        assertThatThrownBy(() -> service.updateWorkshop(WORKSHOP_ID, buildDisableUpdateRequest()))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                ProductionErrorCodeConstants.WORKSHOP_ACTIVE_REFERENCE_EXISTS));
        verify(workshopRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("停用车间：存在启用用户引用时拒绝")
    void disableWorkshopRejectsUserReference() {
        WorkshopEntity workshop = buildWorkshop(1, 0);
        when(workshopRepository.findByIdAndDeletedFalseForUpdate(WORKSHOP_ID))
                .thenReturn(Optional.of(workshop));
        when(productionLineRepository.existsByWorkshopIdAndStatusAndDeletedFalse(
                WORKSHOP_ID, 1)).thenReturn(false);
        when(workOrderRepository.existsByWorkshopIdAndOrderStatusInAndDeletedFalse(
                WORKSHOP_ID, WorkOrderStatusEnum.activeStatuses())).thenReturn(false);
        when(userReferenceQuery.hasEnabledWorkshopUser(WORKSHOP_ID)).thenReturn(true);
        ProductionStatusReqVO request = new ProductionStatusReqVO();
        request.setVersion(0);
        request.setStatus(0);

        assertThatThrownBy(() -> service.updateWorkshopStatus(WORKSHOP_ID, request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                ProductionErrorCodeConstants.WORKSHOP_ACTIVE_REFERENCE_EXISTS));
        verify(workshopRepository, never()).saveAndFlush(any());
    }

    /** 构造有效车间保存请求。 */
    private WorkshopSaveReqVO buildSaveRequest() {
        WorkshopSaveReqVO request = new WorkshopSaveReqVO();
        request.setWorkshopCode(" ws-a ");
        request.setWorkshopName(" 成型车间 ");
        request.setManagerId(8L);
        request.setStatus(1);
        return request;
    }

    /** 构造停用车间的修改请求。 */
    private WorkshopUpdateReqVO buildDisableUpdateRequest() {
        WorkshopUpdateReqVO request = new WorkshopUpdateReqVO();
        request.setWorkshopCode("WS-A");
        request.setWorkshopName("成型车间");
        request.setManagerId(8L);
        request.setStatus(0);
        request.setVersion(0);
        return request;
    }

    /** 构造车间实体。 */
    private WorkshopEntity buildWorkshop(Integer status, Integer version) {
        WorkshopEntity workshop = new WorkshopEntity();
        workshop.setId(WORKSHOP_ID);
        workshop.setWorkshopCode("WS-A");
        workshop.setWorkshopName("成型车间");
        workshop.setStatus(status);
        workshop.setVersion(version);
        workshop.setDeleted(false);
        return workshop;
    }
}
