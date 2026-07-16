package com.badminton.mes.module.report.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.report.controller.vo.DefectReportRespVO;
import com.badminton.mes.module.report.controller.vo.ProductTraceQueryReqVO;
import com.badminton.mes.module.report.controller.vo.ProductTraceRespVO;
import com.badminton.mes.module.report.controller.vo.ProductionReportRespVO;
import com.badminton.mes.module.report.controller.vo.ReportQueryReqVO;
import com.badminton.mes.module.report.controller.vo.RealtimeReportQueryReqVO;
import com.badminton.mes.module.report.service.DefectReportService;
import com.badminton.mes.module.report.service.ProductTraceService;
import com.badminton.mes.module.report.service.ProductionReportService;
import com.badminton.mes.module.report.service.RealtimeProductionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * M4 报表、追溯、净额、去重和数据权限真实 MySQL 集成测试。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
class ReportM4IntegrationTest {

    private static final Long WORK_ORDER_ID = 9_400_001L;
    private static final Long TASK_ID = 9_400_002L;
    private static final Long DETAIL_ID = 9_400_003L;
    private static final Long REPORT_ID = 9_400_004L;
    private static final Long REVERSAL_ID = 9_400_005L;
    private static final Long BARCODE_ID = 9_400_006L;
    private static final Long MATERIAL_ID = 9_400_007L;
    private static final Long WORKSHOP_ID = 9_400_008L;
    private static final Long LINE_ID = 9_400_009L;
    private static final Long QUALITY_RECORD_ID = 9_400_010L;
    private static final Long EQUIPMENT_ID = 9_400_011L;
    private static final Long ANDON_EVENT_ID = 9_400_012L;
    private static final String BATCH_NO = "M4-TRACE-BATCH";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProductionReportService productionReportService;

    @Autowired
    private ProductTraceService productTraceService;

    @Autowired
    private DefectReportService defectReportService;

    @Autowired
    private RealtimeProductionService realtimeProductionService;

    @BeforeEach
    void setUp() {
        cleanup();
        insertFixture();
        login(RoleCodeConstants.ADMIN, null, null);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
        cleanup();
    }

    @Test
    void productionReportReturnsNetAndAuditAmounts() {
        ProductionReportRespVO.Summary result = productionReportService.summary(query());

        assertThat(result.getPlanQuantity()).isEqualTo(100);
        assertThat(result.getOccurrenceInputQuantity()).isEqualTo(10);
        assertThat(result.getReversalInputQuantity()).isEqualTo(4);
        assertThat(result.getInputQuantity()).isEqualTo(6);
        assertThat(result.getDefectQuantity()).isEqualTo(3);
        assertThat(productionReportService.details(query()).getList()).hasSize(2)
                .anyMatch(row -> row.getRecordType() == 2 && row.getNetInputQuantity() == -4);
    }

    @Test
    void traceReturnsCoreChainAndExplicitWarnings() {
        ProductTraceQueryReqVO reqVO = new ProductTraceQueryReqVO();
        reqVO.setBatchCode(BATCH_NO);

        ProductTraceRespVO result = productTraceService.trace(reqVO);

        assertThat(result.getTask().getId()).isEqualTo(TASK_ID);
        assertThat(result.getWorkOrder().getId()).isEqualTo(WORK_ORDER_ID);
        assertThat(result.getBarcodes()).hasSize(1);
        assertThat(result.getBarcodeUses()).hasSize(1);
        assertThat(result.getProcessHistories()).hasSize(1);
        assertThat(result.getWorkReports()).hasSize(2);
        assertThat(result.getMaterials()).hasSize(1);
        assertThat(result.getDataCompleteness()).isEqualTo("PARTIAL");
        assertThat(result.getQualityDefects()).hasSize(1);
        assertThat(result.getEquipmentStatuses()).hasSize(1);
        assertThat(result.getAndonExceptions()).hasSize(1);
        assertThat(result.getWarnings()).noneMatch(warning -> warning.startsWith("QUALITY_INSPECTION"));
    }

    @Test
    void defectReportUsesReversalNetAndGroupNumber() {
        DefectReportRespVO.Summary result = defectReportService.summary(query());

        assertThat(result.getSceneOccurrenceQuantity()).isEqualTo(5);
        assertThat(result.getSceneReversalQuantity()).isEqualTo(2);
        assertThat(result.getSceneDefectQuantity()).isEqualTo(3);
        assertThat(result.getQualityDefectQuantity()).isEqualTo(3);
        assertThat(result.getComprehensiveDefectQuantity()).isEqualTo(3);
        assertThat(result.getComprehensiveEventCount()).isEqualTo(1);
        assertThat(result.getWarnings()).isEmpty();
    }

    @Test
    void realtimeOverviewIncludesEquipmentAndAndonSupport() {
        RealtimeReportQueryReqVO request = new RealtimeReportQueryReqVO();
        request.setWorkshopId(WORKSHOP_ID);
        request.setLineId(LINE_ID);

        var result = realtimeProductionService.overview(request);

        assertThat(result.getEquipmentTotalCount()).isEqualTo(1);
        assertThat(result.getRunningEquipmentCount()).isEqualTo(1);
        assertThat(result.getOpenAndonCount()).isEqualTo(1);
        assertThat(result.getCriticalAndonCount()).isEqualTo(1);
    }

    @Test
    void reportQueryRejectsCrossLineExpansion() {
        login(RoleCodeConstants.OPERATOR, WORKSHOP_ID, LINE_ID);
        ReportQueryReqVO reqVO = query();
        reqVO.setLineId(LINE_ID + 1);

        assertThatThrownBy(() -> productionReportService.summary(reqVO))
                .isInstanceOf(ServiceException.class);
    }

    private ReportQueryReqVO query() {
        ReportQueryReqVO reqVO = new ReportQueryReqVO();
        reqVO.setStartTime(LocalDateTime.now().minusDays(1));
        reqVO.setEndTime(LocalDateTime.now().plusDays(1));
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        return reqVO;
    }

    private void insertFixture() {
        jdbcTemplate.update("""
                INSERT INTO prod_work_order
                    (id, work_order_no, source_type, product_id, product_name, spec, unit_id,
                     batch_no, workshop_id, plan_quantity, input_quantity, finish_quantity,
                     defect_quantity, rework_quantity, plan_start_time, plan_end_time,
                     order_status, create_by, is_deleted)
                VALUES (?, 'M4-WO', 1, 9400200, 'M4产品', 'M4规格', 1,
                        ?, ?, 100, 6, 0, 3, 0, NOW(), DATE_ADD(NOW(), INTERVAL 1 DAY), 2, 1, 0)
                """, WORK_ORDER_ID, BATCH_NO, WORKSHOP_ID);
        jdbcTemplate.update("""
                INSERT INTO prod_task
                    (id, task_no, source_type, work_order_id, work_order_no, product_id,
                     product_code, product_name, batch_no, routing_id, routing_code, routing_version,
                     workshop_id, workshop_name, line_id, line_name, plan_date, plan_quantity,
                     input_quantity, good_quantity, defect_quantity, rework_quantity, finish_quantity,
                     plan_start_time, plan_end_time, actual_start_time, task_status, create_by, is_deleted)
                VALUES (?, 'M4-TASK', 2, ?, 'M4-WO', 9400200,
                        'M4-P', 'M4产品', ?, 9400300, 'M4-R', 'V1',
                        ?, 'M4车间', ?, 'M4产线', CURRENT_DATE, 100,
                        6, 3, 3, 0, 0, NOW(), DATE_ADD(NOW(), INTERVAL 1 DAY), NOW(), 3, 1, 0)
                """, TASK_ID, WORK_ORDER_ID, BATCH_NO, WORKSHOP_ID, LINE_ID);
        jdbcTemplate.update("""
                INSERT INTO prod_process_dispatch_detail
                    (id, dispatch_id, task_id, process_id, process_code, process_name, seq,
                     is_key, is_inspect, is_scan, plan_quantity, good_quantity, defect_quantity,
                     detail_status, is_paused, is_deleted)
                VALUES (?, 9400400, ?, 9400500, 'M4-PROC', 'M4工序', 1,
                        0, 0, 0, 100, 3, 3, 1, 0, 0)
                """, DETAIL_ID, TASK_ID);
        jdbcTemplate.update("""
                INSERT INTO prod_report
                    (id, report_no, request_no, task_id, dispatch_detail_id, process_id, batch_no,
                     report_type, record_type, user_id, input_quantity, good_quantity,
                     defect_quantity, rework_quantity, source_type, report_time, is_deleted)
                VALUES (?, 'M4-R1', 'M4-REQ1', ?, ?, 9400500, ?, 1, 1, 1, 10, 5, 5, 0, 1, NOW(), 0)
                """, REPORT_ID, TASK_ID, DETAIL_ID, BATCH_NO);
        jdbcTemplate.update("""
                INSERT INTO prod_report
                    (id, report_no, request_no, task_id, dispatch_detail_id, process_id, batch_no,
                     report_type, record_type, source_report_id, user_id, input_quantity, good_quantity,
                     defect_quantity, rework_quantity, source_type, reverse_reason, report_time, is_deleted)
                VALUES (?, 'M4-R2', 'M4-REQ2', ?, ?, 9400500, ?, 1, 2, ?, 1, 4, 2, 2, 0, 1,
                        '测试冲销', NOW(), 0)
                """, REVERSAL_ID, TASK_ID, DETAIL_ID, BATCH_NO, REPORT_ID);
        jdbcTemplate.update("""
                INSERT INTO prod_report_defect
                    (report_id, defect_reason_id, defect_quantity, defect_group_no, is_deleted)
                VALUES (?, 9400600, 5, 'M4-D001', 0), (?, 9400600, 2, 'M4-D001', 0)
                """, REPORT_ID, REVERSAL_ID);
        jdbcTemplate.update("""
                INSERT INTO barcode
                    (id, barcode_value, barcode_type_id, barcode_mode, product_id, batch_no,
                     work_order_id, task_id, source_type, barcode_status, create_by, is_deleted)
                VALUES (?, 'M4-BARCODE', 9400700, 2, 9400200, ?, ?, ?, 2, 1, 1, 0)
                """, BARCODE_ID, BATCH_NO, WORK_ORDER_ID, TASK_ID);
        jdbcTemplate.update("""
                INSERT INTO barcode_use_record
                    (barcode_id, task_id, process_id, user_id, equipment_id,
                     use_type, business_time, is_deleted)
                VALUES (?, ?, 9400500, 1, ?, 3, NOW(), 0)
                """, BARCODE_ID, TASK_ID, EQUIPMENT_ID);
        jdbcTemplate.update("""
                INSERT INTO prod_batch_process_history
                    (batch_status_id, task_id, dispatch_detail_id, batch_no, process_id,
                     process_code, process_name, action_type, operator_id, operate_time, is_deleted)
                VALUES (9400800, ?, ?, ?, 9400500, 'M4-PROC', 'M4工序', 2, 1, NOW(), 0)
                """, TASK_ID, DETAIL_ID, BATCH_NO);
        jdbcTemplate.update("""
                INSERT INTO base_material
                    (id, material_code, material_name, material_type, unit_id, is_key_material,
                     status, create_by, update_by, is_deleted)
                VALUES (?, 'M4-MATERIAL', 'M4关键物料', 1, 1, 1, 1, 1, 1, 0)
                """, MATERIAL_ID);
        jdbcTemplate.update("""
                INSERT INTO prod_work_order_material
                    (work_order_id, material_id, require_quantity, issued_quantity, is_deleted)
                VALUES (?, ?, 100.0000, 80.0000, 0)
                """, WORK_ORDER_ID, MATERIAL_ID);
        jdbcTemplate.update("""
                INSERT INTO quality_inspection_record
                    (id, inspection_no, inspection_type, plan_id, plan_code_snapshot,
                     plan_version_snapshot, work_order_id, production_task_id, product_id,
                     production_line_id, process_id, batch_no, sample_quantity, record_status,
                     conclusion, release_status, defect_group_no, defect_quantity,
                     nonconformance_description, disposition, inspector_id, inspected_at,
                     create_by, is_deleted)
                VALUES (?, 'M4-QI', 'PATROL', 9400900, 'M4-PLAN', 1, ?, ?, 9400200,
                        ?, 9400500, ?, 10, 'SUBMITTED', 'REWORK', 'BLOCKED', 'M4-D001', 3,
                        '外观破损', '返修', 1, NOW(), 1, 0)
                """, QUALITY_RECORD_ID, WORK_ORDER_ID, TASK_ID, LINE_ID, BATCH_NO);
        jdbcTemplate.update("""
                INSERT INTO equip_ledger
                    (id, equipment_code, equipment_name, category_id, workshop_id,
                     production_line_id, equipment_status, status, create_by, is_deleted)
                VALUES (?, 'M4-EQ', 'M4设备', 1, ?, ?, 'RUNNING', 1, 1, 0)
                """, EQUIPMENT_ID, WORKSHOP_ID, LINE_ID);
        jdbcTemplate.update("""
                INSERT INTO andon_event
                    (id, event_no, andon_type_id, source_channel, severity, workshop_id,
                     production_line_id, work_order_id, production_task_id, process_id,
                     equipment_id, batch_no, description, event_status, timeout_status,
                     light_status, initiated_by, is_deleted)
                VALUES (?, 'M4-ANDON', 1, 'SYSTEM', 'CRITICAL', ?, ?, ?, ?, 9400500,
                        ?, ?, '设备异常停机', 'PROCESSING', 'NORMAL', 'NOT_REQUIRED', 1, 0)
                """, ANDON_EVENT_ID, WORKSHOP_ID, LINE_ID, WORK_ORDER_ID, TASK_ID,
                EQUIPMENT_ID, BATCH_NO);
    }

    private void login(String role, Long workshopId, Long lineId) {
        SecurityContextHolder.clear();
        LoginUser user = new LoginUser();
        user.setUserId(1L);
        user.setWorkshopId(workshopId);
        user.setLineId(lineId);
        user.setRoleCodes(List.of(role));
        SecurityContextHolder.set("m4-integration", user);
    }

    private void cleanup() {
        jdbcTemplate.update("DELETE FROM andon_event WHERE id=?", ANDON_EVENT_ID);
        jdbcTemplate.update("DELETE FROM quality_inspection_record WHERE id=?", QUALITY_RECORD_ID);
        jdbcTemplate.update("DELETE FROM prod_work_order_material WHERE work_order_id=?", WORK_ORDER_ID);
        jdbcTemplate.update("DELETE FROM base_material WHERE id=?", MATERIAL_ID);
        jdbcTemplate.update("DELETE FROM prod_batch_process_history WHERE task_id=?", TASK_ID);
        jdbcTemplate.update("DELETE FROM barcode_use_record WHERE barcode_id=?", BARCODE_ID);
        jdbcTemplate.update("DELETE FROM equip_ledger WHERE id=?", EQUIPMENT_ID);
        jdbcTemplate.update("DELETE FROM barcode WHERE id=?", BARCODE_ID);
        jdbcTemplate.update("DELETE FROM prod_report_defect WHERE report_id IN (?, ?)", REPORT_ID, REVERSAL_ID);
        jdbcTemplate.update("DELETE FROM prod_report WHERE task_id=?", TASK_ID);
        jdbcTemplate.update("DELETE FROM prod_process_dispatch_detail WHERE id=?", DETAIL_ID);
        jdbcTemplate.update("DELETE FROM prod_task WHERE id=?", TASK_ID);
        jdbcTemplate.update("DELETE FROM prod_work_order WHERE id=?", WORK_ORDER_ID);
    }
}
