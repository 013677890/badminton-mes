package com.badminton.mes.module.report.service.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 不良来源批次防御性复制与净额计算契约测试。 @author 范家权 */
class DefectSourceDtoContractTest {

    @Test
    void nullCollectionsNormalizeToImmutableEmptyLists() {
        DefectSourceBatch batch = new DefectSourceBatch(null, null);

        assertThat(batch.records()).isEmpty();
        assertThat(batch.warnings()).isEmpty();
        assertThatThrownBy(() -> batch.warnings().add("x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void constructorDefensivelyCopiesMutableInputs() {
        List<String> warnings = new ArrayList<>(List.of("QUALITY unavailable"));
        DefectSourceBatch batch = new DefectSourceBatch(List.of(record(5, 2)), warnings);
        warnings.add("late mutation");

        assertThat(batch.warnings()).containsExactly("QUALITY unavailable");
    }

    @Test
    void netQuantityPreservesPositiveZeroAndNegativeReversals() {
        assertThat(record(5, 2).netQuantity()).isEqualTo(3);
        assertThat(record(5, 5).netQuantity()).isZero();
        assertThat(record(2, 5).netQuantity()).isEqualTo(-3);
    }

    private static DefectSourceRecord record(long occurrence, long reversal) {
        return new DefectSourceRecord("SCENE_REPORT", 1L, null, "G-1", 2L,
                "T-2", "WO-2", 3L, "羽毛球", "B-1", 4L, 5L, 6L,
                "穿线", "D-1", "断线", occurrence, reversal,
                LocalDateTime.of(2026, 7, 17, 9, 0));
    }
}
