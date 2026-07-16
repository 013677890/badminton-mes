package com.badminton.mes;

import com.badminton.mes.module.report.service.ProductTraceService;
import com.badminton.mes.module.report.service.ProductionReportService;
import com.badminton.mes.module.report.service.kanban.KanbanSnapshotService;
import com.badminton.mes.module.report.service.miniapp.MiniAppDashboardService;
import com.badminton.mes.module.scene.service.SceneRepairWorkOrderService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * M6 集成验收的应用装配冒烟测试。
 *
 * <p>该测试不写入业务数据，确保交付包中 M5 关键边界均能在同一 Spring
 * 上下文中完成装配，作为全链路接口验收前的最低门槛。</p>
 *
 * @author 刘涵
 * @date 2026/07/14
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
class M6IntegrationAcceptanceTest {

    @Autowired
    private SceneRepairWorkOrderService sceneRepairWorkOrderService;

    @Autowired
    private MiniAppDashboardService miniAppDashboardService;

    @Autowired
    private KanbanSnapshotService kanbanSnapshotService;

    @Autowired
    private ProductionReportService productionReportService;

    @Autowired
    private ProductTraceService productTraceService;

    @Test
    void m5AndM6DeliveryServicesAreWiredTogether() {
        assertThat(sceneRepairWorkOrderService).isNotNull();
        assertThat(miniAppDashboardService).isNotNull();
        assertThat(kanbanSnapshotService).isNotNull();
        assertThat(productionReportService).isNotNull();
        assertThat(productTraceService).isNotNull();
    }
}
