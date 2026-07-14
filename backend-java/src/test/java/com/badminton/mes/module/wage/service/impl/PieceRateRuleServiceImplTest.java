package com.badminton.mes.module.wage.service.impl;

import java.time.LocalDate;
import java.util.Optional;

import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.craft.dal.entity.CraftProcessEntity;
import com.badminton.mes.module.craft.dal.repository.CraftProcessRepository;
import com.badminton.mes.module.production.dal.repository.ProductRepository;
import com.badminton.mes.module.wage.controller.vo.PieceRateRuleStatusReqVO;
import com.badminton.mes.module.wage.dal.entity.PieceRateRuleEntity;
import com.badminton.mes.module.wage.dal.repository.PieceRateRuleRepository;
import com.badminton.mes.module.wage.dal.repository.WageRuleChangeLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import tools.jackson.databind.ObjectMapper;

/** {@link PieceRateRuleServiceImpl} 锁顺序测试。 */
@ExtendWith(MockitoExtension.class)
class PieceRateRuleServiceImplTest {

    private static final Long RULE_ID = 100L;
    private static final Long PROCESS_ID = 200L;

    @Mock
    private PieceRateRuleRepository ruleRepository;
    @Mock
    private WageRuleChangeLogRepository changeLogRepository;
    @Mock
    private CraftProcessRepository processRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ObjectMapper objectMapper;

    private PieceRateRuleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PieceRateRuleServiceImpl(ruleRepository, changeLogRepository,
                processRepository, productRepository, objectMapper);
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(9L);
        SecurityContextHolder.set("wage-rule-test-token", loginUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    @DisplayName("启用规则：按工序后规则的统一顺序获取写锁")
    void enableRuleLocksProcessBeforeRule() throws Exception {
        PieceRateRuleEntity currentRule = buildRule(0);
        PieceRateRuleEntity lockedRule = buildRule(0);
        CraftProcessEntity process = new CraftProcessEntity();
        process.setId(PROCESS_ID);
        process.setStatus(1);
        process.setPieceRateEnabled(true);
        when(ruleRepository.findByIdAndDeletedFalse(RULE_ID)).thenReturn(Optional.of(currentRule));
        when(processRepository.findByIdAndDeletedFalseForUpdate(PROCESS_ID))
                .thenReturn(Optional.of(process));
        when(ruleRepository.findByIdAndDeletedFalseForUpdate(RULE_ID))
                .thenReturn(Optional.of(lockedRule));
        when(objectMapper.writeValueAsString(org.mockito.ArgumentMatchers.any()))
                .thenReturn("{}");
        PieceRateRuleStatusReqVO request = new PieceRateRuleStatusReqVO();
        request.setVersion(0);
        request.setStatus(1);
        request.setReason("恢复计件");

        service.updateRuleStatus(RULE_ID, request);

        InOrder lockOrder = inOrder(processRepository, ruleRepository);
        lockOrder.verify(processRepository).findByIdAndDeletedFalseForUpdate(PROCESS_ID);
        lockOrder.verify(ruleRepository).findByIdAndDeletedFalseForUpdate(RULE_ID);
        assertThat(lockedRule.getStatus()).isEqualTo(1);
        verify(ruleRepository).saveAndFlush(lockedRule);
    }

    /** 构造计件规则。 */
    private PieceRateRuleEntity buildRule(Integer status) {
        PieceRateRuleEntity rule = new PieceRateRuleEntity();
        rule.setId(RULE_ID);
        rule.setProcessId(PROCESS_ID);
        rule.setProductId(null);
        rule.setUnitPriceBasis(10_000L);
        rule.setDefectDeductionRate(0);
        rule.setEffectiveStart(LocalDate.of(2026, 1, 1));
        rule.setStatus(status);
        rule.setVersion(0);
        return rule;
    }
}
