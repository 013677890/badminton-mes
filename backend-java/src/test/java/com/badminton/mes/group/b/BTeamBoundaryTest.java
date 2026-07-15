package com.badminton.mes.group.b;

import com.badminton.mes.module.barcode.enums.BarcodeRuleItemTypeEnum;
import com.badminton.mes.module.barcode.service.impl.BarcodeValueComposer;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** B 组边界条件维度：规则为空、顺序重复、长度上限和流水边界。 @author 范家权 */
class BTeamBoundaryTest {

    @Test
    void emptyRuleReturnsActionableValidationError() {
        assertThat(BarcodeValueComposer.validate(3, List.of()))
                .containsExactly("规则组成明细不能为空");
    }

    @Test
    void duplicateSequenceAndMissingSerialAreReportedTogether() {
        List<BarcodeValueComposer.RuleSegment> segments = List.of(
                new BarcodeValueComposer.RuleSegment(1, BarcodeRuleItemTypeEnum.CONSTANT.getType(),
                        "A", null, null),
                new BarcodeValueComposer.RuleSegment(1, BarcodeRuleItemTypeEnum.CONSTANT.getType(),
                        "B", null, null));

        assertThat(BarcodeValueComposer.validate(3, segments))
                .anySatisfy(error -> assertThat(error).contains("重复"))
                .anySatisfy(error -> assertThat(error).contains("流水号"));
    }

    @Test
    void serialAtCapacityIsAcceptedAndNextValueIsRejected() {
        List<BarcodeValueComposer.RuleSegment> serial = List.of(
                new BarcodeValueComposer.RuleSegment(1, BarcodeRuleItemTypeEnum.SERIAL.getType(),
                        null, null, 2));
        assertThat(BarcodeValueComposer.compose(serial,
                new BarcodeValueComposer.ComposeContext(LocalDate.now(), null, null, 99, 2)))
                .isEqualTo("99");
        assertThatThrownBy(() -> BarcodeValueComposer.compose(serial,
                new BarcodeValueComposer.ComposeContext(LocalDate.now(), null, null, 100, 2)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void exactMaximumBarcodeLengthIsAccepted() {
        String constant = "X".repeat(63);
        List<BarcodeValueComposer.RuleSegment> segments = List.of(
                new BarcodeValueComposer.RuleSegment(1, BarcodeRuleItemTypeEnum.CONSTANT.getType(),
                        constant, null, null),
                new BarcodeValueComposer.RuleSegment(2, BarcodeRuleItemTypeEnum.SERIAL.getType(),
                        null, null, 1));
        assertThat(BarcodeValueComposer.compose(segments,
                new BarcodeValueComposer.ComposeContext(LocalDate.now(), null, null, 1, 1)))
                .hasSize(64);
    }
}
