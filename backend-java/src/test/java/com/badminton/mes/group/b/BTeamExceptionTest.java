package com.badminton.mes.group.b;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.barcode.constants.BarcodeErrorCodeConstants;
import com.badminton.mes.module.barcode.enums.BarcodeRuleItemTypeEnum;
import com.badminton.mes.module.barcode.service.impl.BarcodeValueComposer;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** B 组异常维度：条码变量、日期格式、类型和流水容量异常。 @author 范家权 */
class BTeamExceptionTest {

    @Test
    void missingProductVariableUsesStableBusinessError() {
        assertThatThrownBy(() -> BarcodeValueComposer.compose(List.of(
                        new BarcodeValueComposer.RuleSegment(1,
                                BarcodeRuleItemTypeEnum.VARIABLE.getType(), "productCode", null, null)),
                new BarcodeValueComposer.ComposeContext(LocalDate.now(), null, "L1", 1, 2)))
                .isInstanceOfSatisfying(ServiceException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isSameAs(BarcodeErrorCodeConstants.BARCODE_RULE_VARIABLE_MISSING));
    }

    @Test
    void invalidDateFormatUsesConfigurationError() {
        assertThatThrownBy(() -> BarcodeValueComposer.compose(List.of(
                        new BarcodeValueComposer.RuleSegment(1,
                                BarcodeRuleItemTypeEnum.DATE.getType(), null, "invalid", null)),
                new BarcodeValueComposer.ComposeContext(LocalDate.now(), null, null, 1, 2)))
                .isInstanceOfSatisfying(ServiceException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isSameAs(BarcodeErrorCodeConstants.BARCODE_RULE_CONFIG_INVALID));
    }

    @Test
    void unsupportedItemTypeUsesConfigurationError() {
        assertThatThrownBy(() -> BarcodeValueComposer.compose(List.of(
                        new BarcodeValueComposer.RuleSegment(1, 99, "x", null, null)),
                new BarcodeValueComposer.ComposeContext(LocalDate.now(), null, null, 1, 2)))
                .isInstanceOfSatisfying(ServiceException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isSameAs(BarcodeErrorCodeConstants.BARCODE_RULE_CONFIG_INVALID));
    }

    @Test
    void serialZeroAndOverflowNeverWrapAround() {
        List<BarcodeValueComposer.RuleSegment> serial = List.of(
                new BarcodeValueComposer.RuleSegment(1, BarcodeRuleItemTypeEnum.SERIAL.getType(),
                        null, null, 2));
        assertThatThrownBy(() -> BarcodeValueComposer.compose(serial,
                new BarcodeValueComposer.ComposeContext(LocalDate.now(), null, null, 0, 2)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("流水号");
        assertThatThrownBy(() -> BarcodeValueComposer.compose(serial,
                new BarcodeValueComposer.ComposeContext(LocalDate.now(), null, null, 100, 2)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("流水号");
    }
}
