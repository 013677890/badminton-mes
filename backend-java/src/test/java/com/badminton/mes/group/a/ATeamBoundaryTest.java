package com.badminton.mes.group.a;

import com.badminton.mes.module.craft.service.support.CraftVersionValidator;
import com.badminton.mes.module.integration.service.ErpModeValidator;
import com.badminton.mes.module.production.enums.WorkOrderStatusEnum;
import com.badminton.mes.module.wage.service.support.WageAmountUtils;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A 组边界条件维度：空值、零值、终态和配置归一化。 @author 范家权 */
class ATeamBoundaryTest {

    @Test
    void wageNullConversionsRemainNullAndExactBasisRoundTrips() {
        assertThat(WageAmountUtils.fromAmountBasis((Long) null)).isNull();
        assertThat(WageAmountUtils.fromRateBasis(null)).isNull();
        assertThat(WageAmountUtils.toAmountBasis(new BigDecimal("1.2345"))).isEqualTo(12_345L);
    }

    @Test
    void wageDeductionNeverProducesNegativePay() {
        assertThat(WageAmountUtils.calculateAmount(
                BigDecimal.ONE, BigDecimal.TEN, 10_000L, 10_000)).isZero();
    }

    @Test
    void productionActiveStatusListIsReadOnly() {
        assertThatThrownBy(() -> WorkOrderStatusEnum.activeStatuses().add(99))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void craftNullVersionsCanMatchNullSnapshots() {
        assertThatCode(() -> CraftVersionValidator.validate(null, null,
                new com.badminton.mes.common.core.ErrorCode("A0500", "版本冲突", "请重试")))
                .doesNotThrowAnyException();
    }

    @Test
    void erpModeNormalizesWhitespaceAndCase() {
        assertThatCode(() -> new ErpModeValidator("  ReMoTe ",
                org.mockito.Mockito.mock(org.springframework.core.env.Environment.class)).validate())
                .doesNotThrowAnyException();
    }
}
