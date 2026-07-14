package com.badminton.mes.module.production.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link WorkOrderStatusEnum} 状态分组测试。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
class WorkOrderStatusEnumTest {

    @Test
    @DisplayName("活动状态：包含全部非终态且排除关闭和作废")
    void activeStatusesExcludeTerminalStates() {
        assertThat(WorkOrderStatusEnum.activeStatuses())
                .containsExactly(
                        WorkOrderStatusEnum.CREATED.getStatus(),
                        WorkOrderStatusEnum.RELEASED.getStatus(),
                        WorkOrderStatusEnum.IN_PRODUCTION.getStatus(),
                        WorkOrderStatusEnum.PAUSED.getStatus(),
                        WorkOrderStatusEnum.FINISHED.getStatus())
                .doesNotContain(
                        WorkOrderStatusEnum.CLOSED.getStatus(),
                        WorkOrderStatusEnum.CANCELLED.getStatus());
    }
}
