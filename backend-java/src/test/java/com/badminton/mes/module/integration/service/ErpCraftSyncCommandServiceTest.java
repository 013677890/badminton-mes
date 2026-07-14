package com.badminton.mes.module.integration.service;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.craft.controller.vo.CraftRouteSaveReqVO;
import com.badminton.mes.module.craft.dal.entity.CraftProcessEntity;
import com.badminton.mes.module.craft.dal.repository.CraftProcessRepository;
import com.badminton.mes.module.craft.service.CraftRouteService;
import com.badminton.mes.module.integration.constants.IntegrationErrorCodeConstants;
import com.badminton.mes.module.integration.dal.entity.ErpCraftPendingEntity;
import com.badminton.mes.module.integration.dal.repository.ErpCraftPendingRepository;
import com.badminton.mes.module.integration.enums.ErpCraftPendingStatusEnum;
import com.badminton.mes.module.integration.enums.IntegrationInterfaceTypeEnum;
import com.badminton.mes.module.integration.service.dto.ErpCraftDTO;
import com.badminton.mes.module.integration.service.dto.ErpCraftStepDTO;
import com.badminton.mes.module.integration.service.dto.ErpCraftSyncResult;
import com.badminton.mes.module.production.dal.entity.ProductEntity;
import com.badminton.mes.module.production.dal.repository.ProductRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpCraftSyncCommandService} 事务、校验与确认锁回归测试。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@ExtendWith(MockitoExtension.class)
class ErpCraftSyncCommandServiceTest {

    private static final Long OPERATOR_ID = 9L;

    private static final String SOURCE_SYSTEM = "ERP-MAIN";

    @Mock
    private ErpCraftPendingRepository pendingRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CraftProcessRepository processRepository;

    @Mock
    private CraftRouteService craftRouteService;

    @Mock
    private IntegrationAuditService auditService;

    private ErpCraftSyncCommandService commandService;

    @BeforeEach
    void setUp() {
        commandService = new ErpCraftSyncCommandService(
                pendingRepository,
                productRepository,
                processRepository,
                craftRouteService,
                auditService,
                JsonMapper.builder().build());
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(OPERATOR_ID);
        SecurityContextHolder.set("erp-sync-test-token", loginUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    @DisplayName("工艺同步：空步骤保存异常暂存并在当前事务记录失败日志")
    void emptyStepsSaveFailedPendingAndCurrentTransactionAudit() {
        ErpCraftDTO craft = new ErpCraftDTO(
                "ROUTE-001", "标准路线", "V1", "PRODUCT-001", List.of());
        when(pendingRepository.findBySourceSystemAndErpRoutingCodeAndErpRoutingVersion(
                SOURCE_SYSTEM, "ROUTE-001", "V1")).thenReturn(Optional.empty());
        doAnswer(invocation -> {
            ErpCraftPendingEntity pending = invocation.getArgument(0);
            pending.setId(10L);
            return pending;
        }).when(pendingRepository).saveAndFlush(any(ErpCraftPendingEntity.class));
        when(auditService.recordFailureInCurrentTransaction(
                any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(100L);

        ErpCraftSyncResult result = commandService.syncCraft(
                craft, "{}", SOURCE_SYSTEM);

        assertThat(result.duplicate()).isFalse();
        assertThat(result.pending()).satisfies(pending -> {
            assertThat(pending.getId()).isEqualTo(10L);
            assertThat(pending.getStatus())
                    .isEqualTo(ErpCraftPendingStatusEnum.FAILED.getStatus());
            assertThat(pending.getCreateBy()).isEqualTo(OPERATOR_ID);
            assertThat(pending.getProcessSteps()).isEqualTo("[]");
        });
        verify(auditService).recordFailureInCurrentTransaction(
                eq(IntegrationInterfaceTypeEnum.ERP_CRAFT_SYNC),
                eq(SOURCE_SYSTEM),
                eq("ROUTE-001:V1"),
                eq("{}"),
                eq(10L),
                eq("ROUTE-001:V1"),
                eq(IntegrationErrorCodeConstants.ERP_SOURCE_DATA_INVALID),
                any());
        verify(auditService, never()).recordFailure(
                any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("工艺确认：以悲观写锁读取暂存记录后创建 ERP 草稿路线")
    void confirmCraftUsesLockedPendingRecord() {
        ErpCraftPendingEntity pending = buildPending();
        ProductEntity product = new ProductEntity();
        product.setId(20L);
        product.setStatus(CommonStatusEnum.ENABLED.getStatus());
        CraftProcessEntity process = new CraftProcessEntity();
        process.setId(30L);
        process.setQualityRequired(Boolean.TRUE);
        when(pendingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(pending));
        when(productRepository.findByProductCodeAndDeletedFalse("PRODUCT-001"))
                .thenReturn(Optional.of(product));
        when(processRepository.findByProcessCodeAndDeletedFalse("PR001"))
                .thenReturn(Optional.of(process));
        when(craftRouteService.createRoute(any(CraftRouteSaveReqVO.class)))
                .thenReturn(40L);

        Long routeId = commandService.confirmCraft(10L);

        assertThat(routeId).isEqualTo(40L);
        assertThat(pending.getStatus())
                .isEqualTo(ErpCraftPendingStatusEnum.CONFIRMED.getStatus());
        assertThat(pending.getConfirmedRouteId()).isEqualTo(40L);
        assertThat(pending.getConfirmedBy()).isEqualTo(OPERATOR_ID);
        verify(pendingRepository).findByIdForUpdate(10L);
        verify(pendingRepository, never()).findById(10L);

        ArgumentCaptor<CraftRouteSaveReqVO> requestCaptor =
                ArgumentCaptor.forClass(CraftRouteSaveReqVO.class);
        verify(craftRouteService).createRoute(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getSourceType()).isEqualTo(2);
        assertThat(requestCaptor.getValue().getProductIds()).containsExactly(20L);
        assertThat(requestCaptor.getValue().getSteps()).singleElement().satisfies(step -> {
            assertThat(step.getSequenceNo()).isEqualTo(1);
            assertThat(step.getProcessId()).isEqualTo(30L);
        });
    }

    private ErpCraftPendingEntity buildPending() {
        ErpCraftPendingEntity pending = new ErpCraftPendingEntity();
        pending.setId(10L);
        pending.setSourceSystem(SOURCE_SYSTEM);
        pending.setErpRoutingCode("ROUTE-001");
        pending.setErpRoutingName("标准路线");
        pending.setErpRoutingVersion("V1");
        pending.setProductCode("PRODUCT-001");
        pending.setStatus(ErpCraftPendingStatusEnum.PENDING.getStatus());
        pending.setProcessSteps(JsonMapper.builder().build().writeValueAsString(
                List.of(new ErpCraftStepDTO(1, "PR001", "羽毛分拣"))));
        return pending;
    }
}
