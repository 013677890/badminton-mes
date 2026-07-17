package com.badminton.mes.stress;

import com.badminton.mes.common.core.ErrorCode;
import com.badminton.mes.module.craft.service.support.CraftVersionValidator;
import com.badminton.mes.module.integration.enums.IntegrationWriteStatusEnum;
import com.badminton.mes.module.production.enums.WorkOrderStatusEnum;
import com.badminton.mes.module.wage.service.support.WageAmountUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.math.BigDecimal;

/** A 组生产、工艺、工资和集成无状态规则并发压力测试。 @author 范家权 */
@Tag("stress")
@Timeout(90)
class ATeamConcurrencyStressTest {

    private static final int OPERATIONS = Integer.getInteger("mes.stress.operations", 10_000);
    private static final ErrorCode VERSION_CONFLICT =
            new ErrorCode("A0500", "version conflict", "retry");

    @Test
    void productionAndCraftRulesRemainStableUnderContention() throws Exception {
        ConcurrentStressRunner.run("a-production-craft", OPERATIONS, index -> {
            if (!WorkOrderStatusEnum.activeStatuses().contains(index % 5)) {
                throw new AssertionError("active work-order status missing");
            }
            CraftVersionValidator.validate(index, index, VERSION_CONFLICT);
        });
    }

    @Test
    void wageCalculationHasNoCrossThreadRoundingState() throws Exception {
        ConcurrentStressRunner.run("a-wage", OPERATIONS, index -> {
            long amount = WageAmountUtils.calculateAmount(
                    BigDecimal.TEN, BigDecimal.ONE, 25_000L, 1_000);
            if (amount != 247_500L) {
                throw new AssertionError("unexpected wage amount: " + amount);
            }
        });
    }

    @Test
    void integrationStatusMetadataIsSafeForConcurrentReads() throws Exception {
        IntegrationWriteStatusEnum[] statuses = IntegrationWriteStatusEnum.values();
        ConcurrentStressRunner.run("a-integration", OPERATIONS, index -> {
            IntegrationWriteStatusEnum status = statuses[index % statuses.length];
            if (status.getStatus() == null || status.getCode() == null) {
                throw new AssertionError("incomplete integration status");
            }
        });
    }
}
