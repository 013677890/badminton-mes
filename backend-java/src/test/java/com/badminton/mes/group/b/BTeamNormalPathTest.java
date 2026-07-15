package com.badminton.mes.group.b;

import com.badminton.mes.module.barcode.enums.BarcodeRuleItemTypeEnum;
import com.badminton.mes.module.barcode.service.impl.BarcodeValueComposer;
import com.badminton.mes.module.scene.constants.SceneParameterCodes;
import com.badminton.mes.module.scene.enums.SceneBatchStatusEnum;
import com.badminton.mes.module.scene.enums.SceneOperationStatusEnum;
import com.badminton.mes.module.scene.enums.SceneTaskStatusEnum;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** B 组正常路径维度：条码组合、现场参数和生产状态。 @author 范家权 */
class BTeamNormalPathTest {

    @Test
    void barcodeComposerCombinesConstantDateVariableAndSerialInOrder() {
        List<BarcodeValueComposer.RuleSegment> segments = List.of(
                new BarcodeValueComposer.RuleSegment(3, BarcodeRuleItemTypeEnum.VARIABLE.getType(),
                        "productCode", null, null),
                new BarcodeValueComposer.RuleSegment(1, BarcodeRuleItemTypeEnum.CONSTANT.getType(),
                        "PR", null, null),
                new BarcodeValueComposer.RuleSegment(4, BarcodeRuleItemTypeEnum.SERIAL.getType(),
                        null, null, 3),
                new BarcodeValueComposer.RuleSegment(2, BarcodeRuleItemTypeEnum.DATE.getType(),
                        null, "yyyyMMdd", null));

        assertThat(BarcodeValueComposer.compose(segments,
                new BarcodeValueComposer.ComposeContext(LocalDate.of(2026, 7, 15),
                        "P-01", "L-01", 7, 3))).isEqualTo("PR20260715P-01007");
    }

    @Test
    void barcodeRuleCapacityUsesConfiguredSerialLength() {
        assertThat(BarcodeValueComposer.serialCapacity(1)).isEqualTo(9L);
        assertThat(BarcodeValueComposer.serialCapacity(6)).isEqualTo(999_999L);
    }

    @Test
    void sceneParameterDefaultsProtectScanAndSkipRules() {
        assertThat(SceneParameterCodes.DEFAULT_VALUES)
                .containsEntry(SceneParameterCodes.MUST_SCAN_REPORT, "1")
                .containsEntry(SceneParameterCodes.ALLOW_SKIP_PROCESS, "0");
    }

    @Test
    void sceneStatusesExposeStableWorkflowValues() {
        assertThat(SceneTaskStatusEnum.RELEASED.getStatus()).isEqualTo(2);
        assertThat(SceneOperationStatusEnum.COMPLETED.getStatus()).isEqualTo(2);
        assertThat(SceneBatchStatusEnum.FINISHED.getStatus()).isEqualTo(5);
    }
}
