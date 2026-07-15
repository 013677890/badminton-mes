package com.badminton.mes.module.barcode.service.impl;

import java.time.LocalDate;
import java.util.List;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.barcode.constants.BarcodeErrorCodeConstants;
import com.badminton.mes.module.barcode.enums.BarcodeRuleItemTypeEnum;
import com.badminton.mes.module.barcode.service.impl.BarcodeValueComposer.ComposeContext;
import com.badminton.mes.module.barcode.service.impl.BarcodeValueComposer.ComposedSegment;
import com.badminton.mes.module.barcode.service.impl.BarcodeValueComposer.RuleSegment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link BarcodeValueComposer} 单元测试。
 *
 * <p>纯函数测试，无外部依赖。覆盖"产品编码+日期+批次流水号"主路径、
 * 流水容量、变量缺值、长度上限与逐类配置校验。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
class BarcodeValueComposerTest {

    /** 测试用业务日期 */
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 7, 12);

    @Test
    @DisplayName("组合：产品编码+日期+流水号按 seq 顺序拼接，流水左补零")
    void composeBuildsProductDateSerialBarcode() {
        List<RuleSegment> segments = List.of(
                variable(1, "productCode"),
                date(2, "yyyyMMdd"),
                serial(3));

        String value = BarcodeValueComposer.compose(segments,
                new ComposeContext(BUSINESS_DATE, "YMQ01", null, 1L, 4));

        assertThat(value).isEqualTo("YMQ0120260712" + "0001");
    }

    @Test
    @DisplayName("组合：段按 seq 排序，与入参顺序无关，常量段原样输出")
    void composeSortsSegmentsBySeq() {
        List<RuleSegment> segments = List.of(
                serial(3),
                constant(1, "BM-"),
                date(2, "yyMM"));

        List<ComposedSegment> composed = BarcodeValueComposer.composeSegments(segments,
                new ComposeContext(BUSINESS_DATE, null, null, 25L, 4));

        assertThat(composed).extracting(ComposedSegment::content)
                .containsExactly("BM-", "2607", "0025");
    }

    @Test
    @DisplayName("组合：产线编码变量取上下文产线值")
    void composeResolvesLineCodeVariable() {
        List<RuleSegment> segments = List.of(variable(1, "lineCode"), serial(2));

        String value = BarcodeValueComposer.compose(segments,
                new ComposeContext(BUSINESS_DATE, null, "L01", 7L, 3));

        assertThat(value).isEqualTo("L01007");
    }

    @Test
    @DisplayName("组合：流水号超出 10^位数-1 容量时报规则容量不足，不回绕")
    void composeRejectsSerialBeyondCapacity() {
        List<RuleSegment> segments = List.of(serial(1));

        assertThatThrownBy(() -> BarcodeValueComposer.compose(segments,
                new ComposeContext(BUSINESS_DATE, null, null, 10000L, 4)))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_RULE_SERIAL_CAPACITY_EXCEEDED));
    }

    @Test
    @DisplayName("组合：变量缺少业务取值时报变量缺值并指明变量名")
    void composeRejectsMissingVariableValue() {
        List<RuleSegment> segments = List.of(variable(1, "productCode"), serial(2));

        assertThatThrownBy(() -> BarcodeValueComposer.compose(segments,
                new ComposeContext(BUSINESS_DATE, "  ", null, 1L, 4)))
                .isInstanceOfSatisfying(ServiceException.class, e -> {
                    assertThat(e.getErrorCode())
                            .isEqualTo(BarcodeErrorCodeConstants.BARCODE_RULE_VARIABLE_MISSING);
                    assertThat(e.getMessage()).contains("productCode");
                });
    }

    @Test
    @DisplayName("组合：总长度超过 64 时报条码超长")
    void composeRejectsValueBeyondMaxLength() {
        List<RuleSegment> segments = List.of(constant(1, "C".repeat(61)), serial(2));

        assertThatThrownBy(() -> BarcodeValueComposer.compose(segments,
                new ComposeContext(BUSINESS_DATE, null, null, 1L, 4)))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_VALUE_TOO_LONG));
    }

    @Test
    @DisplayName("校验：产品编码+日期+流水号标准配置合法")
    void validateAcceptsStandardConfig() {
        List<RuleSegment> segments = List.of(
                variable(1, "productCode"),
                date(2, "yyyyMMdd"),
                serial(3));

        assertThat(BarcodeValueComposer.validate(4, segments)).isEmpty();
    }

    @Test
    @DisplayName("校验：组成顺序重复被拒绝")
    void validateRejectsDuplicateSeq() {
        List<RuleSegment> segments = List.of(constant(1, "A"), serial(1));

        assertThat(BarcodeValueComposer.validate(4, segments))
                .anyMatch(error -> error.contains("重复"));
    }

    @Test
    @DisplayName("校验：缺少流水号组成项被拒绝(生成结果必须唯一)")
    void validateRejectsMissingSerialSegment() {
        List<RuleSegment> segments = List.of(constant(1, "A"), date(2, "yyyyMMdd"));

        assertThat(BarcodeValueComposer.validate(4, segments))
                .anyMatch(error -> error.contains("流水号"));
    }

    @Test
    @DisplayName("校验：多个流水号组成项被拒绝")
    void validateRejectsMultipleSerialSegments() {
        List<RuleSegment> segments = List.of(serial(1), serial(2));

        assertThat(BarcodeValueComposer.validate(4, segments))
                .anyMatch(error -> error.contains("只能包含一个流水号"));
    }

    @Test
    @DisplayName("校验：常量段缺少常量值被拒绝")
    void validateRejectsBlankConstant() {
        List<RuleSegment> segments = List.of(constant(1, " "), serial(2));

        assertThat(BarcodeValueComposer.validate(4, segments))
                .anyMatch(error -> error.contains("常量值不能为空"));
    }

    @Test
    @DisplayName("校验：日期格式非法被拒绝")
    void validateRejectsInvalidDateFormat() {
        List<RuleSegment> segments = List.of(date(1, "TTT"), serial(2));

        assertThat(BarcodeValueComposer.validate(4, segments))
                .anyMatch(error -> error.contains("日期格式不合法"));
    }

    @Test
    @DisplayName("校验：不支持的变量名被拒绝")
    void validateRejectsUnsupportedVariable() {
        List<RuleSegment> segments = List.of(variable(1, "batchNo"), serial(2));

        assertThat(BarcodeValueComposer.validate(4, segments))
                .anyMatch(error -> error.contains("变量名不支持"));
    }

    @Test
    @DisplayName("校验：流水号段长度与规则流水位数不一致被拒绝")
    void validateRejectsSerialLengthMismatch() {
        RuleSegment mismatched = new RuleSegment(1, BarcodeRuleItemTypeEnum.SERIAL.getType(),
                null, null, 6);

        assertThat(BarcodeValueComposer.validate(4, List.of(mismatched)))
                .anyMatch(error -> error.contains("流水位数不一致"));
    }

    @Test
    @DisplayName("校验：按已知段长估算超过 64 被拒绝")
    void validateRejectsKnownLengthBeyondMax() {
        List<RuleSegment> segments = List.of(constant(1, "C".repeat(61)), serial(2));

        assertThat(BarcodeValueComposer.validate(4, segments))
                .anyMatch(error -> error.contains("上限 64"));
    }

    @Test
    @DisplayName("容量：10^流水位数-1")
    void serialCapacityFollowsLength() {
        assertThat(BarcodeValueComposer.serialCapacity(1)).isEqualTo(9L);
        assertThat(BarcodeValueComposer.serialCapacity(4)).isEqualTo(9999L);
        assertThat(BarcodeValueComposer.serialCapacity(9)).isEqualTo(999999999L);
    }

    /**
     * 构造常量段。
     *
     * @param seq   组成顺序
     * @param value 常量值
     * @return 规则组成段
     */
    private RuleSegment constant(int seq, String value) {
        return new RuleSegment(seq, BarcodeRuleItemTypeEnum.CONSTANT.getType(), value, null, null);
    }

    /**
     * 构造日期段。
     *
     * @param seq    组成顺序
     * @param format 日期格式
     * @return 规则组成段
     */
    private RuleSegment date(int seq, String format) {
        return new RuleSegment(seq, BarcodeRuleItemTypeEnum.DATE.getType(), null, format, null);
    }

    /**
     * 构造变量段。
     *
     * @param seq      组成顺序
     * @param variable 变量名
     * @return 规则组成段
     */
    private RuleSegment variable(int seq, String variable) {
        return new RuleSegment(seq, BarcodeRuleItemTypeEnum.VARIABLE.getType(), variable, null, null);
    }

    /**
     * 构造流水号段。
     *
     * @param seq 组成顺序
     * @return 规则组成段
     */
    private RuleSegment serial(int seq) {
        return new RuleSegment(seq, BarcodeRuleItemTypeEnum.SERIAL.getType(), null, null, null);
    }
}
