package com.badminton.mes.group.a;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.craft.service.support.CraftVersionValidator;
import com.badminton.mes.module.integration.service.ErpModeValidator;
import com.badminton.mes.module.wage.constants.WageErrorCodeConstants;
import com.badminton.mes.module.wage.service.support.WageAmountUtils;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** A 组异常维度：版本冲突、金额精度和 ERP 运行模式拒绝。 @author 范家权 */
class ATeamExceptionTest {

    @Test
    void craftVersionMismatchReturnsConflictError() {
        var error = new com.badminton.mes.common.core.ErrorCode("A0500", "版本冲突", "请重试");
        assertThatThrownBy(() -> CraftVersionValidator.validate(2, 1, error))
                .isInstanceOfSatisfying(ServiceException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isSameAs(error));
    }

    @Test
    void wageRejectsSubBasisPrecisionInsteadOfSilentlyRounding() {
        assertThatThrownBy(() -> WageAmountUtils.toAmountBasis(new BigDecimal("0.00001")))
                .isInstanceOfSatisfying(ServiceException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isSameAs(WageErrorCodeConstants.AMOUNT_OUT_OF_RANGE));
    }

    @Test
    void erpRejectsUnsupportedMode() {
        assertThatThrownBy(() -> new ErpModeValidator("sandbox", mock(Environment.class)).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unsupported mes.erp.mode");
    }

    @Test
    void erpRejectsMockModeUnderProductionProfile() {
        Environment environment = mock(Environment.class);
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);

        assertThatThrownBy(() -> new ErpModeValidator("mock", environment).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requires mes.erp.mode=remote");
    }
}
