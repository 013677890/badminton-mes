package com.badminton.mes.module.wage.service.impl;

import java.time.LocalDate;
import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.craft.dal.repository.CraftProcessRepository;
import com.badminton.mes.module.system.dal.repository.UserRepository;
import com.badminton.mes.module.wage.controller.vo.WageSettlementActionReqVO;
import com.badminton.mes.module.wage.controller.vo.WageSummaryReqVO;
import com.badminton.mes.module.wage.dal.entity.WageSettlementEntity;
import com.badminton.mes.module.wage.dal.repository.WageSettlementAuditLogRepository;
import com.badminton.mes.module.wage.dal.repository.WageSettlementDetailRepository;
import com.badminton.mes.module.wage.dal.repository.WageSettlementRepository;
import com.badminton.mes.module.wage.dal.repository.WageWorkRecordRepository;
import com.badminton.mes.module.wage.enums.WageSettlementStatusEnum;
import com.badminton.mes.module.wage.service.WageSettlementAuditService;
import com.badminton.mes.module.wage.service.WageSettlementCalculator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import tools.jackson.databind.ObjectMapper;

/** {@link WageSettlementServiceImpl} 结算状态与资源边界测试。 */
@ExtendWith(MockitoExtension.class)
class WageSettlementServiceImplTest {

    private static final Long SETTLEMENT_ID = 100L;

    @Mock
    private WageSettlementRepository settlementRepository;
    @Mock
    private WageSettlementDetailRepository detailRepository;
    @Mock
    private WageSettlementAuditLogRepository auditLogRepository;
    @Mock
    private WageWorkRecordRepository workRecordRepository;
    @Mock
    private WageSettlementCalculator calculator;
    @Mock
    private WageSettlementAuditService auditService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CraftProcessRepository processRepository;

    private WageSettlementServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new WageSettlementServiceImpl(settlementRepository, detailRepository,
                auditLogRepository, workRecordRepository, calculator, auditService,
                userRepository, processRepository, new ObjectMapper());
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(9L);
        SecurityContextHolder.set("wage-settlement-test-token", loginUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    @DisplayName("驳回结算：释放有效明细并保留驳回状态")
    void rejectSettlementReleasesActiveDetails() {
        WageSettlementEntity settlement = new WageSettlementEntity();
        settlement.setId(SETTLEMENT_ID);
        settlement.setVersion(0);
        settlement.setSettlementStatus(WageSettlementStatusEnum.PENDING.getStatus());
        when(settlementRepository.findByIdAndDeletedFalseForUpdate(SETTLEMENT_ID))
                .thenReturn(Optional.of(settlement));
        WageSettlementActionReqVO request = new WageSettlementActionReqVO();
        request.setVersion(0);
        request.setReason("数据有误");

        service.rejectSettlement(SETTLEMENT_ID, request);

        verify(detailRepository).deactivateBySettlementId(SETTLEMENT_ID);
        verify(settlementRepository).saveAndFlush(settlement);
        assertThat(settlement.getSettlementStatus())
                .isEqualTo(WageSettlementStatusEnum.REJECTED.getStatus());
    }

    @Test
    @DisplayName("工资汇总：拒绝超过一个日历年的统计周期")
    void summarizeEmployeesRejectsPeriodLongerThanCalendarYear() {
        WageSummaryReqVO request = new WageSummaryReqVO();
        request.setPeriodStart(LocalDate.of(2024, 2, 29));
        request.setPeriodEnd(LocalDate.of(2025, 2, 28));

        assertThatThrownBy(() -> service.summarizeEmployees(request))
                .isInstanceOf(ServiceException.class)
                .hasMessage("工资汇总周期最长一个日历年");
        verify(detailRepository, never()).summarizeEmployees(
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean(),
                org.mockito.ArgumentMatchers.anyCollection(), org.mockito.ArgumentMatchers.any());
    }
}
