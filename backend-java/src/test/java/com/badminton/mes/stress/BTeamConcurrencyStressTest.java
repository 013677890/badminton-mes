package com.badminton.mes.stress;

import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.barcode.enums.BarcodeRuleItemTypeEnum;
import com.badminton.mes.module.barcode.service.impl.BarcodeValueComposer;
import com.badminton.mes.module.scene.constants.SceneParameterCodes;
import com.badminton.mes.module.scene.enums.SceneOperationStatusEnum;
import com.badminton.mes.module.scene.enums.SceneTaskStatusEnum;
import com.badminton.mes.module.report.dal.redis.KanbanRedisKeyConstants;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.LocalDate;
import java.util.List;

/** B 组条码组合、现场状态和登录上下文并发压力测试。 @author 范家权 */
@Tag("stress")
@Timeout(90)
class BTeamConcurrencyStressTest {

    private static final int OPERATIONS = Integer.getInteger("mes.stress.operations", 10_000);
    private static final List<BarcodeValueComposer.RuleSegment> RULE = List.of(
            new BarcodeValueComposer.RuleSegment(1, BarcodeRuleItemTypeEnum.CONSTANT.getType(),
                    "B", null, null),
            new BarcodeValueComposer.RuleSegment(2, BarcodeRuleItemTypeEnum.SERIAL.getType(),
                    null, null, 4));

    @Test
    void barcodeComposerHandlesParallelFormattingWithoutSharedBuffers() throws Exception {
        ConcurrentStressRunner.run("b-barcode", OPERATIONS, index -> {
            long serial = index % 9_999L + 1;
            String value = BarcodeValueComposer.compose(RULE,
                    new BarcodeValueComposer.ComposeContext(
                            LocalDate.of(2026, 7, 16), null, null, serial, 4));
            if (!value.startsWith("B") || value.length() != 5) {
                throw new AssertionError("invalid barcode: " + value);
            }
        });
    }

    @Test
    void sceneEnumsAndFrozenDefaultsRemainStableUnderParallelReads() throws Exception {
        SceneTaskStatusEnum[] taskStatuses = SceneTaskStatusEnum.values();
        SceneOperationStatusEnum[] operationStatuses = SceneOperationStatusEnum.values();
        ConcurrentStressRunner.run("b-scene", OPERATIONS, index -> {
            if (taskStatuses[index % taskStatuses.length].getStatus() == null
                    || operationStatuses[index % operationStatuses.length].getStatus() == null
                    || !"1".equals(SceneParameterCodes.DEFAULT_VALUES.get(
                    SceneParameterCodes.MUST_SCAN_REPORT))) {
                throw new AssertionError("scene contract changed during read");
            }
        });
    }

    @Test
    void securityContextIsIsolatedAndClearedForEveryOperation() throws Exception {
        ConcurrentStressRunner.run("b-security-context", OPERATIONS, index -> {
            LoginUser user = new LoginUser();
            user.setUserId((long) index + 1);
            user.setRoleCodes(List.of("OPERATOR"));
            SecurityContextHolder.set("stress-" + index, user);
            try {
                if (!Long.valueOf(index + 1L).equals(SecurityContextHolder.getRequiredLoginUserId())) {
                    throw new AssertionError("operator context leaked");
                }
            } finally {
                SecurityContextHolder.clear();
            }
            if (SecurityContextHolder.getLoginUser() != null) {
                throw new AssertionError("operator context not cleared");
            }
        });
    }

    @Test
    void reportKanbanSnapshotKeysRemainScopeIsolated() throws Exception {
        ConcurrentStressRunner.run("b-report-kanban", OPERATIONS, index -> {
            long id = index + 1L;
            String line = KanbanRedisKeyConstants.snapshotKey("line", id);
            String workshop = KanbanRedisKeyConstants.snapshotKey("workshop", id);
            if (line.equals(workshop)
                    || !line.equals("report:kanban:snapshot:line:" + id)
                    || !workshop.equals("report:kanban:snapshot:workshop:" + id)) {
                throw new AssertionError("kanban snapshot scope collision");
            }
        });
    }
}
