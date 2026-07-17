package com.badminton.mes.group.a;

import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.craft.service.support.CraftVersionValidator;
import com.badminton.mes.module.integration.enums.IntegrationWriteStatusEnum;
import com.badminton.mes.module.wage.service.support.WageAmountUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A 组权限/并发维度：登录上下文隔离与无状态规则并发安全。 @author 范家权 */
class ATeamPermissionConcurrencyTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clear();
    }

    @Test
    void missingOperatorIsRejectedBySharedPermissionContext() {
        SecurityContextHolder.clear();
        assertThatThrownBy(SecurityContextHolder::getRequiredLoginUser)
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("用户登录已过期");
    }

    @Test
    void independentWorkerThreadsDoNotShareOperators() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Long> first = executor.submit(() -> operatorId(101L));
            Future<Long> second = executor.submit(() -> operatorId(202L));
            assertThat(first.get()).isEqualTo(101L);
            assertThat(second.get()).isEqualTo(202L);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void wageAndCraftRulesRemainDeterministicUnderParallelCalls() {
        var error = new com.badminton.mes.common.core.ErrorCode("A0500", "版本冲突", "请重试");
        List<Long> amounts = IntStream.range(0, 200).parallel().mapToObj(index -> {
            CraftVersionValidator.validate(index, index, error);
            return WageAmountUtils.calculateAmount(BigDecimal.TEN, BigDecimal.ONE, 25_000L, 1_000);
        }).toList();

        assertThat(amounts).hasSize(200).allMatch(amount -> amount == 247_500L);
        assertThat(IntegrationWriteStatusEnum.values()).hasSize(3);
    }

    private static Long operatorId(long id) {
        LoginUser user = new LoginUser();
        user.setUserId(id);
        user.setRoleCodes(List.of("OPERATOR"));
        SecurityContextHolder.set("token-" + id, user);
        try {
            return SecurityContextHolder.getRequiredLoginUserId();
        } finally {
            SecurityContextHolder.clear();
        }
    }
}
