package com.badminton.mes.module.wage.service.support;

import java.math.BigDecimal;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.wage.constants.WageErrorCodeConstants;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** {@link WageAmountUtils} 金额边界测试。 */
class WageAmountUtilsTest {

    @Test
    @DisplayName("金额换算：Long 范围内保持万分之一元精度")
    void toAmountBasisKeepsExactPrecision() {
        long amountBasis = WageAmountUtils.toAmountBasis(new BigDecimal("123.4567"));

        assertThat(amountBasis).isEqualTo(1_234_567L);
    }

    @Test
    @DisplayName("金额换算：超过 Long 范围时转换为业务异常")
    void toAmountBasisRejectsOverflow() {
        assertThatThrownBy(() -> WageAmountUtils.toAmountBasis(
                new BigDecimal("922337203685477.5808")))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isSameAs(WageErrorCodeConstants.AMOUNT_OUT_OF_RANGE));
    }

    @Test
    @DisplayName("金额计算：单条计算超过 Long 范围时转换为业务异常")
    void calculateAmountRejectsOverflow() {
        assertThatThrownBy(() -> WageAmountUtils.calculateAmount(
                new BigDecimal("99999999.9999"), BigDecimal.ZERO,
                Long.MAX_VALUE, 0))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isSameAs(WageErrorCodeConstants.AMOUNT_OUT_OF_RANGE));
    }
}
