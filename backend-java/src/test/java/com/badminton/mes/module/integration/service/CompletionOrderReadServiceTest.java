package com.badminton.mes.module.integration.service;

import java.time.LocalDateTime;
import java.util.List;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.integration.constants.IntegrationErrorCodeConstants;
import com.badminton.mes.module.integration.controller.vo.CompletionOrderPageReqVO;
import com.badminton.mes.module.integration.controller.vo.CompletionReadLogPageReqVO;
import com.badminton.mes.module.integration.dal.entity.CompletionOrderEntity;
import com.badminton.mes.module.integration.dal.entity.CompletionReadLogEntity;
import com.badminton.mes.module.integration.dal.repository.CompletionOrderRepository;
import com.badminton.mes.module.integration.dal.repository.CompletionReadLogRepository;
import com.badminton.mes.module.integration.enums.CompletionAuditStatusEnum;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CompletionOrderReadService} 单元测试。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@ExtendWith(MockitoExtension.class)
class CompletionOrderReadServiceTest {

    private static final Long READER_ID = 9L;

    @Mock
    private CompletionOrderRepository completionOrderRepository;

    @Mock
    private CompletionReadLogRepository readLogRepository;

    private CompletionOrderReadService readService;

    @BeforeEach
    void setUp() {
        readService = new CompletionOrderReadService(
                completionOrderRepository, readLogRepository);
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(READER_ID);
        SecurityContextHolder.set("completion-read-test", loginUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    @DisplayName("完工单读取：每条返回记录写一条日志且不修改完工单")
    @SuppressWarnings("unchecked")
    void getCompletionOrderPageWritesOneLogPerReturnedOrder() {
        CompletionOrderPageReqVO reqVO = buildOrderReqVO();
        CompletionOrderEntity first = buildOrder(10L, "CO-001", "WO-001");
        CompletionOrderEntity second = buildOrder(11L, "CO-002", "WO-002");
        when(completionOrderRepository.count(any(Specification.class))).thenReturn(2L);
        when(completionOrderRepository.findAll(
                any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(first, second)));

        var result = readService.getCompletionOrderPage(reqVO);

        assertThat(result.getList()).extracting("completionNo")
                .containsExactly("CO-001", "CO-002");
        ArgumentCaptor<List<CompletionReadLogEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(readLogRepository).saveAllAndFlush(captor.capture());
        assertThat(captor.getValue()).hasSize(2)
                .allSatisfy(log -> {
                    assertThat(log.getSourceSystem()).isEqualTo("ERP-MAIN");
                    assertThat(log.getReadBy()).isEqualTo(READER_ID);
                });
        assertThat(captor.getValue()).extracting(CompletionReadLogEntity::getCompletionNo)
                .containsExactly("CO-001", "CO-002");
        verify(completionOrderRepository, never()).save(any());
        verify(completionOrderRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("完工单读取：无已审核数据时直接返回空页且不写日志")
    @SuppressWarnings("unchecked")
    void getCompletionOrderPageReturnsEmptyWithoutLogs() {
        when(completionOrderRepository.count(any(Specification.class))).thenReturn(0L);

        var result = readService.getCompletionOrderPage(buildOrderReqVO());

        assertThat(result.getTotal()).isZero();
        assertThat(result.getList()).isEmpty();
        verify(completionOrderRepository, never()).findAll(
                any(Specification.class), any(Pageable.class));
        verify(readLogRepository, never()).saveAllAndFlush(any());
    }

    @Test
    @DisplayName("完工单读取：结束时间早于开始时间时拒绝查询")
    void getCompletionOrderPageRejectsReversedTimeRange() {
        CompletionOrderPageReqVO reqVO = buildOrderReqVO();
        reqVO.setStartTime(LocalDateTime.of(2026, 7, 14, 0, 0));
        reqVO.setEndTime(LocalDateTime.of(2026, 7, 13, 0, 0));

        assertThatThrownBy(() -> readService.getCompletionOrderPage(reqVO))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                IntegrationErrorCodeConstants.COMPLETION_TIME_RANGE_INVALID));
        verify(completionOrderRepository, never()).count(any(Specification.class));
    }

    @Test
    @DisplayName("读取日志：超界页码修正为最后一页并转换响应")
    @SuppressWarnings("unchecked")
    void getReadLogPageNormalizesOverflowPage() {
        CompletionReadLogPageReqVO reqVO = new CompletionReadLogPageReqVO();
        reqVO.setPageNo(9);
        reqVO.setPageSize(10);
        CompletionReadLogEntity log = new CompletionReadLogEntity();
        log.setId(20L);
        log.setCompletionOrderId(10L);
        log.setCompletionNo("CO-001");
        log.setWorkOrderNo("WO-001");
        log.setSourceSystem("ERP-MAIN");
        log.setReadBy(READER_ID);
        LocalDateTime readTime = LocalDateTime.of(2026, 7, 13, 12, 0);
        log.setReadTime(readTime);
        when(readLogRepository.count(any(Specification.class))).thenReturn(11L);
        when(readLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        var result = readService.getReadLogPage(reqVO);

        assertThat(result.getPageNo()).isEqualTo(2);
        assertThat(result.getList()).singleElement()
                .satisfies(response -> {
                    assertThat(response.getCompletionNo()).isEqualTo("CO-001");
                    assertThat(response.getReadTime()).isEqualTo(readTime);
                });
    }

    private CompletionOrderPageReqVO buildOrderReqVO() {
        CompletionOrderPageReqVO reqVO = new CompletionOrderPageReqVO();
        reqVO.setSourceSystem("erp-main");
        return reqVO;
    }

    private CompletionOrderEntity buildOrder(Long id, String completionNo, String workOrderNo) {
        CompletionOrderEntity order = new CompletionOrderEntity();
        order.setId(id);
        order.setCompletionNo(completionNo);
        order.setWorkOrderNo(workOrderNo);
        order.setProductCode("P001");
        order.setProductName("比赛级羽毛球");
        order.setBatchNo("BATCH-001");
        order.setCompletionQuantity(100);
        order.setGoodQuantity(98);
        order.setDefectQuantity(2);
        order.setAuditStatus(CompletionAuditStatusEnum.APPROVED.getStatus());
        order.setAuditTime(LocalDateTime.of(2026, 7, 13, 11, 0));
        return order;
    }
}
