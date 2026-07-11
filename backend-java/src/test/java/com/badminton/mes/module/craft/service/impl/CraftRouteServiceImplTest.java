package com.badminton.mes.module.craft.service.impl;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.craft.constants.CraftErrorCodeConstants;
import com.badminton.mes.module.craft.controller.vo.CraftRouteChangeLogPageReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteChangeLogRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteNewVersionReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteSaveReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteStatusReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteStepSaveReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteUpdateReqVO;
import com.badminton.mes.module.craft.dal.entity.CraftRouteChangeLogEntity;
import com.badminton.mes.module.craft.dal.entity.CraftRouteDetailEntity;
import com.badminton.mes.module.craft.dal.entity.CraftRouteEntity;
import com.badminton.mes.module.craft.dal.entity.CraftRouteProductEntity;
import com.badminton.mes.module.craft.dal.repository.CraftProcessRepository;
import com.badminton.mes.module.craft.dal.repository.CraftRouteChangeLogRepository;
import com.badminton.mes.module.craft.dal.repository.CraftRouteProductRepository;
import com.badminton.mes.module.craft.dal.repository.CraftRouteRepository;
import com.badminton.mes.module.craft.enums.CraftRouteChangeTypeEnum;
import com.badminton.mes.module.craft.enums.CraftRouteStatusEnum;
import com.badminton.mes.module.craft.service.CraftRouteAuditService;
import com.badminton.mes.module.craft.service.CraftRouteChildService;
import com.badminton.mes.module.craft.service.CraftRouteReferenceValidator;
import com.badminton.mes.module.craft.service.dto.CraftRouteChildren;
import com.badminton.mes.module.production.dal.repository.ProductRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CraftRouteServiceImpl} 单元测试。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@ExtendWith(MockitoExtension.class)
class CraftRouteServiceImplTest {

    private static final Long ROUTE_ID = 100L;

    private static final Long NEW_ROUTE_ID = 200L;

    private static final Long PRODUCT_ID = 30L;

    private static final Long PROCESS_ID = 40L;

    private static final Long OPERATOR_ID = 9L;

    @Mock
    private CraftRouteRepository routeRepository;

    @Mock
    private CraftRouteProductRepository routeProductRepository;

    @Mock
    private CraftRouteChangeLogRepository changeLogRepository;

    @Mock
    private CraftProcessRepository processRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private CraftRouteChildService childService;

    @Mock
    private CraftRouteReferenceValidator referenceValidator;

    @Mock
    private CraftRouteAuditService auditService;

    private CraftRouteServiceImpl routeService;

    @BeforeEach
    void setUp() {
        routeService = new CraftRouteServiceImpl(routeRepository, routeProductRepository,
                changeLogRepository, processRepository, productRepository, workOrderRepository,
                childService, referenceValidator, auditService);
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(OPERATOR_ID);
        SecurityContextHolder.set("unit-test-token", loginUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    @DisplayName("创建路线：规范化编码、落草稿状态并写入创建审计")
    void createRouteNormalizesAndWritesAudit() {
        CraftRouteSaveReqVO reqVO = buildSaveReqVO();
        reqVO.setRoutingCode("rt-shuttle");
        reqVO.setRoutingVersion("v1.0");
        stubSaveAssignsId(ROUTE_ID);
        when(childService.create(eq(ROUTE_ID), anyList(), anyList(), eq(OPERATOR_ID)))
                .thenReturn(buildChildren(false));

        Long id = routeService.createRoute(reqVO);

        assertThat(id).isEqualTo(ROUTE_ID);
        ArgumentCaptor<CraftRouteEntity> captor = ArgumentCaptor.forClass(CraftRouteEntity.class);
        verify(routeRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getRoutingCode()).isEqualTo("RT-SHUTTLE");
        assertThat(captor.getValue().getRoutingVersion()).isEqualTo("V1.0");
        assertThat(captor.getValue().getRoutingStatus())
                .isEqualTo(CraftRouteStatusEnum.DRAFT.getStatus());
        verify(referenceValidator).validateForSave(anyList(), anyList());
        verify(auditService).record(eq(ROUTE_ID), eq(CraftRouteChangeTypeEnum.CREATE),
                eq(null), any(), eq("创建工艺路线"), eq(OPERATOR_ID));
    }

    @Test
    @DisplayName("创建路线：编码与业务版本重复时拒绝保存")
    void createRouteRejectsDuplicateCodeVersion() {
        when(routeRepository.existsByRoutingCodeAndRoutingVersionAndDeletedFalse(
                "RT-SHUTTLE", "V1.0")).thenReturn(true);

        assertThatThrownBy(() -> routeService.createRoute(buildSaveReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isSameAs(CraftErrorCodeConstants.ROUTE_CODE_VERSION_DUPLICATE));
        verify(routeRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("修改路线：客户端版本落后时拒绝覆盖新数据")
    void updateRouteRejectsStaleClientVersion() {
        when(routeRepository.findByIdAndDeletedFalse(ROUTE_ID))
                .thenReturn(Optional.of(buildRoute(CraftRouteStatusEnum.DRAFT, 1)));
        CraftRouteUpdateReqVO reqVO = buildUpdateReqVO();
        reqVO.setVersion(0);

        assertThatThrownBy(() -> routeService.updateRoute(ROUTE_ID, reqVO))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isSameAs(CraftErrorCodeConstants.ROUTE_CONCURRENT_MODIFICATION));
        verify(routeRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("修改路线：非草稿状态拒绝修改")
    void updateRouteRejectsNonDraft() {
        when(routeRepository.findByIdAndDeletedFalse(ROUTE_ID))
                .thenReturn(Optional.of(buildRoute(CraftRouteStatusEnum.EFFECTIVE, 0)));

        assertThatThrownBy(() -> routeService.updateRoute(ROUTE_ID, buildUpdateReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isSameAs(CraftErrorCodeConstants.ROUTE_NOT_DRAFT));
        verify(routeRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("修改派生版本：禁止更换路线编码破坏版本链身份")
    void updateDerivedRouteRejectsRoutingCodeChange() {
        CraftRouteEntity route = buildRoute(CraftRouteStatusEnum.DRAFT, 0);
        route.setPreviousRouteId(90L);
        when(routeRepository.findByIdAndDeletedFalse(ROUTE_ID)).thenReturn(Optional.of(route));
        CraftRouteUpdateReqVO reqVO = buildUpdateReqVO();
        reqVO.setRoutingCode("RT-OTHER");

        assertThatThrownBy(() -> routeService.updateRoute(ROUTE_ID, reqVO))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isSameAs(CraftErrorCodeConstants.ROUTE_VERSION_IDENTITY_IMMUTABLE));
        verify(referenceValidator, never()).validateForSave(anyList(), anyList());
        verify(routeRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("删除路线：已被生产工单引用时拒绝删除")
    void deleteRouteRejectsWorkOrderReference() {
        when(routeRepository.findByIdAndDeletedFalseForUpdate(ROUTE_ID))
                .thenReturn(Optional.of(buildRoute(CraftRouteStatusEnum.DRAFT, 0)));
        when(workOrderRepository.existsByRoutingIdAndDeletedFalse(ROUTE_ID)).thenReturn(true);

        assertThatThrownBy(() -> routeService.deleteRoute(ROUTE_ID, 0))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isSameAs(CraftErrorCodeConstants.ROUTE_REFERENCED_BY_WORK_ORDER));
        verify(routeRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("删除路线：草稿逻辑删除并级联删除子记录")
    void deleteRouteMarksDeletedAndRemovesChildren() {
        CraftRouteEntity route = buildRoute(CraftRouteStatusEnum.DRAFT, 0);
        when(routeRepository.findByIdAndDeletedFalseForUpdate(ROUTE_ID)).thenReturn(Optional.of(route));
        when(childService.load(ROUTE_ID)).thenReturn(buildChildren(false));

        routeService.deleteRoute(ROUTE_ID, 0);

        assertThat(route.getDeleted()).isTrue();
        verify(childService).deleteAll(ROUTE_ID, OPERATOR_ID);
        verify(auditService).record(eq(ROUTE_ID), eq(CraftRouteChangeTypeEnum.DELETE),
                any(), eq(null), eq("删除工艺路线草稿"), eq(OPERATOR_ID));
    }

    @Test
    @DisplayName("审核路线：草稿生效、锁定产品并切换默认路线")
    void approveRouteActivatesDefaults() {
        CraftRouteEntity route = buildRoute(CraftRouteStatusEnum.DRAFT, 0);
        when(routeRepository.findByIdAndDeletedFalseForUpdate(ROUTE_ID))
                .thenReturn(Optional.of(route));
        when(childService.load(ROUTE_ID)).thenReturn(buildChildren(false));
        CraftRouteStatusReqVO reqVO = buildStatusReqVO("首版审核生效");

        routeService.approveRoute(ROUTE_ID, reqVO);

        assertThat(route.getRoutingStatus()).isEqualTo(CraftRouteStatusEnum.EFFECTIVE.getStatus());
        assertThat(route.getAuditBy()).isEqualTo(OPERATOR_ID);
        assertThat(route.getAuditTime()).isNotNull();
        InOrder lockThenValidate = inOrder(productRepository, referenceValidator);
        lockThenValidate.verify(productRepository).findAllByIdInForUpdateOrderByIdAsc(List.of(PRODUCT_ID));
        lockThenValidate.verify(referenceValidator).validateForApproval(anyList(), anyList());
        verify(childService).activateDefaults(ROUTE_ID, List.of(PRODUCT_ID), OPERATOR_ID);
        verify(auditService).record(eq(ROUTE_ID), eq(CraftRouteChangeTypeEnum.APPROVE),
                any(), any(), eq("首版审核生效"), eq(OPERATOR_ID));
    }

    @Test
    @DisplayName("审核路线：非草稿状态拒绝审核")
    void approveRouteRejectsNonDraft() {
        when(routeRepository.findByIdAndDeletedFalseForUpdate(ROUTE_ID))
                .thenReturn(Optional.of(buildRoute(CraftRouteStatusEnum.EFFECTIVE, 0)));

        assertThatThrownBy(() -> routeService.approveRoute(ROUTE_ID, buildStatusReqVO("重复审核")))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isSameAs(CraftErrorCodeConstants.ROUTE_NOT_DRAFT));
        verify(childService, never()).activateDefaults(any(), anyList(), any());
    }

    @Test
    @DisplayName("审核路线：数据库子项为空时拒绝生效")
    void approveRouteRejectsEmptyStoredChildren() {
        when(routeRepository.findByIdAndDeletedFalseForUpdate(ROUTE_ID))
                .thenReturn(Optional.of(buildRoute(CraftRouteStatusEnum.DRAFT, 0)));
        when(childService.load(ROUTE_ID)).thenReturn(new CraftRouteChildren(List.of(), List.of()));

        assertThatThrownBy(() -> routeService.approveRoute(ROUTE_ID, buildStatusReqVO("审核")))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isSameAs(CraftErrorCodeConstants.ROUTE_CONFIGURATION_INCOMPLETE));
        verify(productRepository, never()).findAllByIdInForUpdateOrderByIdAsc(anyList());
        verify(routeRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("停用路线：生效路线停用并清除默认标记")
    void disableRouteClearsDefaults() {
        CraftRouteEntity route = buildRoute(CraftRouteStatusEnum.EFFECTIVE, 0);
        when(routeRepository.findByIdAndDeletedFalseForUpdate(ROUTE_ID))
                .thenReturn(Optional.of(route));
        when(childService.load(ROUTE_ID)).thenReturn(buildChildren(true));

        routeService.disableRoute(ROUTE_ID, buildStatusReqVO("工艺淘汰"));

        assertThat(route.getRoutingStatus()).isEqualTo(CraftRouteStatusEnum.DISABLED.getStatus());
        verify(childService).clearDefaults(ROUTE_ID, OPERATOR_ID);
        verify(auditService).record(eq(ROUTE_ID), eq(CraftRouteChangeTypeEnum.DISABLE),
                any(), any(), eq("工艺淘汰"), eq(OPERATOR_ID));
    }

    @Test
    @DisplayName("停用路线：草稿状态拒绝停用")
    void disableRouteRejectsDraft() {
        when(routeRepository.findByIdAndDeletedFalseForUpdate(ROUTE_ID))
                .thenReturn(Optional.of(buildRoute(CraftRouteStatusEnum.DRAFT, 0)));

        assertThatThrownBy(() -> routeService.disableRoute(ROUTE_ID, buildStatusReqVO("误操作")))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isSameAs(CraftErrorCodeConstants.ROUTE_NOT_EFFECTIVE));
        verify(childService, never()).clearDefaults(any(), any());
    }

    @Test
    @DisplayName("创建新版本：克隆子记录并回链上一版本")
    void createRouteVersionClonesChildren() {
        CraftRouteEntity source = buildRoute(CraftRouteStatusEnum.EFFECTIVE, 0);
        when(routeRepository.findByIdAndDeletedFalseForUpdate(ROUTE_ID)).thenReturn(Optional.of(source));
        stubSaveAssignsId(NEW_ROUTE_ID);
        CraftRouteChildren children = buildChildren(true);
        when(childService.load(ROUTE_ID)).thenReturn(children);
        when(childService.cloneTo(NEW_ROUTE_ID, children, OPERATOR_ID))
                .thenReturn(buildChildren(false));
        CraftRouteNewVersionReqVO reqVO = buildNewVersionReqVO("v2.0");

        Long newId = routeService.createRouteVersion(ROUTE_ID, reqVO);

        assertThat(newId).isEqualTo(NEW_ROUTE_ID);
        ArgumentCaptor<CraftRouteEntity> captor = ArgumentCaptor.forClass(CraftRouteEntity.class);
        verify(routeRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getRoutingVersion()).isEqualTo("V2.0");
        assertThat(captor.getValue().getPreviousRouteId()).isEqualTo(ROUTE_ID);
        assertThat(captor.getValue().getRoutingStatus())
                .isEqualTo(CraftRouteStatusEnum.DRAFT.getStatus());
        verify(auditService).record(eq(NEW_ROUTE_ID), eq(CraftRouteChangeTypeEnum.CREATE_VERSION),
                any(), any(), eq("年度工艺升级"), eq(OPERATOR_ID));
    }

    @Test
    @DisplayName("创建新版本：源路线未生效时拒绝")
    void createRouteVersionRejectsNonEffectiveSource() {
        when(routeRepository.findByIdAndDeletedFalseForUpdate(ROUTE_ID))
                .thenReturn(Optional.of(buildRoute(CraftRouteStatusEnum.DRAFT, 0)));

        assertThatThrownBy(() ->
                routeService.createRouteVersion(ROUTE_ID, buildNewVersionReqVO("V2.0")))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isSameAs(CraftErrorCodeConstants.ROUTE_NOT_EFFECTIVE));
        verify(routeRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("默认路线：产品未配置默认路线时抛业务异常")
    void getDefaultRouteRejectsMissingDefault() {
        when(routeProductRepository.findByProductIdAndDefaultRouteTrueAndDeletedFalse(PRODUCT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.getDefaultRoute(PRODUCT_ID))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isSameAs(CraftErrorCodeConstants.ROUTE_DEFAULT_NOT_FOUND));
    }

    @Test
    @DisplayName("保存路线：无法识别的完整性异常不伪装成编码重复")
    void saveRouteKeepsUnknownIntegrityViolation() {
        when(routeRepository.saveAndFlush(any(CraftRouteEntity.class)))
                .thenThrow(new DataIntegrityViolationException("unknown constraint"));

        assertThatThrownBy(() -> routeService.createRoute(buildSaveReqVO()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("变更日志：分页返回并按 id 倒序查询")
    void getChangeLogPageReturnsPagedResult() {
        CraftRouteChangeLogPageReqVO reqVO = new CraftRouteChangeLogPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        CraftRouteChangeLogEntity log = new CraftRouteChangeLogEntity();
        log.setId(5L);
        when(routeRepository.existsById(ROUTE_ID)).thenReturn(true);
        when(changeLogRepository.countByRouteIdAndDeletedFalse(ROUTE_ID)).thenReturn(1L);
        when(changeLogRepository.findByRouteIdAndDeletedFalse(eq(ROUTE_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        PageResult<CraftRouteChangeLogRespVO> result =
                routeService.getRouteChangeLogPage(ROUTE_ID, reqVO);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList()).singleElement()
                .extracting(CraftRouteChangeLogRespVO::getId).isEqualTo(5L);
    }

    private void stubSaveAssignsId(Long assignedId) {
        doAnswer(invocation -> {
            CraftRouteEntity entity = invocation.getArgument(0);
            entity.setId(assignedId);
            entity.setVersion(0);
            return entity;
        }).when(routeRepository).saveAndFlush(any(CraftRouteEntity.class));
    }

    private CraftRouteSaveReqVO buildSaveReqVO() {
        CraftRouteSaveReqVO reqVO = new CraftRouteSaveReqVO();
        fillSaveFields(reqVO);
        return reqVO;
    }

    private CraftRouteUpdateReqVO buildUpdateReqVO() {
        CraftRouteUpdateReqVO reqVO = new CraftRouteUpdateReqVO();
        fillSaveFields(reqVO);
        reqVO.setVersion(0);
        return reqVO;
    }

    private void fillSaveFields(CraftRouteSaveReqVO reqVO) {
        reqVO.setRoutingCode("RT-SHUTTLE");
        reqVO.setRoutingName("羽毛球标准工艺");
        reqVO.setRoutingVersion("V1.0");
        reqVO.setSourceType(1);
        reqVO.setProductIds(List.of(PRODUCT_ID));
        reqVO.setSteps(List.of(buildStepReqVO()));
    }

    private CraftRouteStepSaveReqVO buildStepReqVO() {
        CraftRouteStepSaveReqVO step = new CraftRouteStepSaveReqVO();
        step.setSequenceNo(1);
        step.setProcessId(PROCESS_ID);
        step.setInspectNode(false);
        return step;
    }

    private CraftRouteStatusReqVO buildStatusReqVO(String reason) {
        CraftRouteStatusReqVO reqVO = new CraftRouteStatusReqVO();
        reqVO.setVersion(0);
        reqVO.setReason(reason);
        return reqVO;
    }

    private CraftRouteNewVersionReqVO buildNewVersionReqVO(String newRoutingVersion) {
        CraftRouteNewVersionReqVO reqVO = new CraftRouteNewVersionReqVO();
        reqVO.setVersion(0);
        reqVO.setNewRoutingVersion(newRoutingVersion);
        reqVO.setReason("年度工艺升级");
        return reqVO;
    }

    private CraftRouteEntity buildRoute(CraftRouteStatusEnum status, Integer version) {
        CraftRouteEntity route = new CraftRouteEntity();
        route.setId(ROUTE_ID);
        route.setRoutingCode("RT-SHUTTLE");
        route.setRoutingName("羽毛球标准工艺");
        route.setRoutingVersion("V1.0");
        route.setSourceType(1);
        route.setRoutingStatus(status.getStatus());
        route.setVersion(version);
        return route;
    }

    private CraftRouteChildren buildChildren(boolean defaultRoute) {
        CraftRouteProductEntity relation = new CraftRouteProductEntity();
        relation.setRouteId(ROUTE_ID);
        relation.setProductId(PRODUCT_ID);
        relation.setDefaultRoute(defaultRoute);
        CraftRouteDetailEntity detail = new CraftRouteDetailEntity();
        detail.setRouteId(ROUTE_ID);
        detail.setSequenceNo(1);
        detail.setProcessId(PROCESS_ID);
        detail.setInspect(false);
        return new CraftRouteChildren(List.of(relation), List.of(detail));
    }
}
