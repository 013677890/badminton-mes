package com.badminton.mes.group.a;

import com.badminton.mes.module.craft.service.support.CraftVersionValidator;
import com.badminton.mes.module.integration.enums.IntegrationWriteStatusEnum;
import com.badminton.mes.module.integration.service.ErpModeValidator;
import com.badminton.mes.module.production.enums.WorkOrderStatusEnum;
import com.badminton.mes.module.wage.service.support.WageAmountUtils;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/** A 组正常路径维度：生产订单、工艺、计件工资和外部接口。 @author 范家权 */
class ATeamNormalPathTest {

    @Test
    void productionActiveStatusesExcludeTerminalStates() {
        assertThat(WorkOrderStatusEnum.activeStatuses())
                .containsExactly(0, 1, 2, 3, 4)
                .doesNotContain(WorkOrderStatusEnum.CLOSED.getStatus(),
                        WorkOrderStatusEnum.CANCELLED.getStatus());
    }

    @Test
    void craftVersionValidatorAcceptsMatchingSnapshot() {
        CraftVersionValidator.validate(7, 7,
                new com.badminton.mes.common.core.ErrorCode("A0500", "版本冲突", "请重试"));
    }

    @Test
    void wageCalculatesQualifiedAmountAfterDefectDeduction() {
        long amount = WageAmountUtils.calculateAmount(
                BigDecimal.TEN, BigDecimal.ONE, 25_000L, 1_000);

        assertThat(amount).isEqualTo(247_500L);
        assertThat(WageAmountUtils.fromAmountBasis(amount)).isEqualByComparingTo("24.7500");
    }

    @Test
    void integrationStatusesExposeStableStorageAndExternalCodes() {
        assertThat(Arrays.stream(IntegrationWriteStatusEnum.values())
                .allMatch(status -> status.getStatus() != null && status.getCode() != null))
                .isTrue();
        new ErpModeValidator(" remote ", org.mockito.Mockito.mock(
                org.springframework.core.env.Environment.class)).validate();
    }
}
