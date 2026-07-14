package com.badminton.mes.module.scene.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.scene.controller.vo.SceneCompletionAuditReqVO;
import com.badminton.mes.module.scene.controller.vo.SceneWorkReportSubmitReqVO;
import com.badminton.mes.module.scene.dal.entity.SceneCompletionOrderEntity;
import com.badminton.mes.module.scene.dal.entity.SceneCompletionSyncRecordEntity;
import com.badminton.mes.module.scene.dal.repository.SceneCompletionOrderRepository;
import com.badminton.mes.module.scene.service.CompletionSyncResultService;
import com.badminton.mes.module.scene.service.SceneCompletionOrderService;
import com.badminton.mes.module.scene.service.SceneWorkReportService;
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
 * M3 报工、完工和同步结果的真实 MySQL 事务测试。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
class SceneM3TransactionIntegrationTest {

    private static final Long TASK_ID = 9_300_001L;
    private static final Long DETAIL_ID = 9_300_002L;
    private static final Long BARCODE_ID = 9_300_003L;
    private static final Long FINISH_ORDER_ID = 9_300_004L;
    private static final String BATCH_NO = "M3-TX-BATCH";
    private static final String BARCODE_VALUE = "M3-TX-BARCODE";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SceneWorkReportService workReportService;

    @Autowired
    private SceneCompletionOrderService completionOrderService;

    @Autowired
    private CompletionSyncResultService syncResultService;

    @Autowired
    private SceneCompletionOrderRepository completionOrderRepository;

    @BeforeEach
    void setUp() {
        cleanup();
        login();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
        cleanup();
    }

    @Test
    void reportFailureRollsBackBarcodeUseReportAndAggregates() {
        insertTask(0, 0, 0);
        insertDetail(Integer.MAX_VALUE, 0);
        insertBarcode();

        assertThatThrownBy(() -> workReportService.submit(reportRequest("M3-TX-ROLLBACK"), 1))
                .isInstanceOf(RuntimeException.class);

        assertThat(count("SELECT COUNT(*) FROM prod_report WHERE request_no='M3-TX-ROLLBACK'"))
                .isZero();
        assertThat(count("SELECT COUNT(*) FROM barcode_use_record WHERE barcode_id=?", BARCODE_ID))
                .isZero();
        assertThat(value("SELECT barcode_status FROM barcode WHERE id=?", BARCODE_ID))
                .isZero();
        assertThat(value("SELECT input_quantity FROM prod_task WHERE id=?", TASK_ID))
                .isZero();
        assertThat(value("SELECT good_quantity FROM prod_process_dispatch_detail WHERE id=?", DETAIL_ID))
                .isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void concurrentDuplicateRequestReturnsSameIdAndAppliesOnlyOnce() throws Exception {
        insertTask(0, 0, 0);
        insertDetail(0, 0);
        insertBarcode();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger threadNumber = new AtomicInteger();
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("m3-idempotency-" + threadNumber.incrementAndGet());
            return thread;
        };
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), threadFactory);
        try {
            Future<Long> first = executor.submit(() -> submitAfterLatch(ready, start));
            Future<Long> second = executor.submit(() -> submitAfterLatch(ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            Long firstId = first.get(30, TimeUnit.SECONDS);
            Long secondId = second.get(30, TimeUnit.SECONDS);
            assertThat(firstId).isEqualTo(secondId);
        } finally {
            executor.shutdownNow();
        }

        assertThat(count("SELECT COUNT(*) FROM prod_report WHERE request_no='M3-TX-DUPLICATE'"))
                .isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM barcode_use_record WHERE barcode_id=? AND use_type=3", BARCODE_ID))
                .isEqualTo(1);
        assertThat(value("SELECT input_quantity FROM prod_task WHERE id=?", TASK_ID))
                .isEqualTo(1);
        assertThat(value("SELECT good_quantity FROM prod_process_dispatch_detail WHERE id=?", DETAIL_ID))
                .isEqualTo(1);
    }

    @Test
    void completionAuditFailureRollsBackTaskAndOrder() {
        insertTask(20, 0, 0);
        insertCompletionOrder(1, 0);
        SceneCompletionAuditReqVO reqVO = new SceneCompletionAuditReqVO();
        reqVO.setApproved(true);
        reqVO.setRemark("X".repeat(300));

        assertThatThrownBy(() -> completionOrderService.audit(FINISH_ORDER_ID, reqVO))
                .isInstanceOf(RuntimeException.class);

        assertThat(value("SELECT finish_quantity FROM prod_task WHERE id=?", TASK_ID)).isZero();
        assertThat(value("SELECT finish_status FROM prod_finish_order WHERE id=?", FINISH_ORDER_ID))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT audit_by FROM prod_finish_order WHERE id=?", Long.class, FINISH_ORDER_ID))
                .isNull();
    }

    @Test
    void syncResultFailureRollsBackRecordAndOrderTogether() {
        insertTask(20, 0, 0);
        insertCompletionOrder(2, 0);
        SceneCompletionOrderEntity order = completionOrderRepository.findByIdAndDeletedFalse(FINISH_ORDER_ID)
                .orElseThrow();
        order.setAuditRemark("X".repeat(300));
        SceneCompletionSyncRecordEntity record = new SceneCompletionSyncRecordEntity();
        record.setFinishOrderId(FINISH_ORDER_ID);
        record.setTargetSystem("ERP");
        record.setIdempotencyKey("FINISH:M3-TX-FINISH:ERP");
        record.setSyncStatus(2);
        record.setRetryCount(0);
        record.setLastSyncTime(LocalDateTime.now());

        assertThatThrownBy(() -> syncResultService.saveResult(order, record, 2, "database failure"))
                .isInstanceOf(RuntimeException.class);

        assertThat(count("SELECT COUNT(*) FROM prod_finish_sync_record WHERE finish_order_id=?",
                FINISH_ORDER_ID)).isZero();
        assertThat(value("SELECT sync_status FROM prod_finish_order WHERE id=?", FINISH_ORDER_ID))
                .isZero();
    }

    private Long submitAfterLatch(CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        login();
        try {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("并发测试启动超时");
            }
            return workReportService.submit(reportRequest("M3-TX-DUPLICATE"), 1);
        } finally {
            SecurityContextHolder.clear();
        }
    }

    private SceneWorkReportSubmitReqVO reportRequest(String requestNo) {
        SceneWorkReportSubmitReqVO reqVO = new SceneWorkReportSubmitReqVO();
        reqVO.setRequestNo(requestNo);
        reqVO.setDispatchDetailId(DETAIL_ID);
        reqVO.setInputQuantity(1);
        reqVO.setGoodQuantity(1);
        reqVO.setDefectQuantity(0);
        reqVO.setReworkQuantity(0);
        reqVO.setBarcodeValue(BARCODE_VALUE);
        reqVO.setReportTime(LocalDateTime.now());
        return reqVO;
    }

    private void insertTask(int goodQuantity, int finishQuantity, int inputQuantity) {
        jdbcTemplate.update("""
                INSERT INTO prod_task
                    (id, task_no, source_type, work_order_id, work_order_no, product_id,
                     product_code, product_name, batch_no, routing_id, routing_code, routing_version,
                     workshop_id, workshop_name, line_id, line_name, plan_date, plan_quantity,
                     input_quantity, good_quantity, defect_quantity, rework_quantity, finish_quantity,
                     plan_start_time, plan_end_time, task_status, create_by, is_deleted)
                VALUES (?, 'M3-TX-TASK', 2, 9300100, 'M3-TX-WO', 9300200,
                        'M3-PRODUCT', 'M3事务产品', ?, 9300300, 'M3-ROUTING', 'V1',
                        9300400, 'M3事务车间', 9300500, 'M3事务产线', CURRENT_DATE, 100,
                        ?, ?, 0, 0, ?, NOW(), DATE_ADD(NOW(), INTERVAL 1 DAY), 3, 1, 0)
                """, TASK_ID, BATCH_NO, inputQuantity, goodQuantity, finishQuantity);
    }

    private void insertDetail(int goodQuantity, int defectQuantity) {
        jdbcTemplate.update("""
                INSERT INTO prod_process_dispatch_detail
                    (id, dispatch_id, task_id, process_id, process_code, process_name, seq,
                     is_key, is_inspect, is_scan, plan_quantity, good_quantity, defect_quantity,
                     detail_status, is_paused, is_deleted)
                VALUES (?, 9300600, ?, 9300700, 'M3-PROCESS', 'M3事务工序', 1,
                        0, 0, 1, 100, ?, ?, 1, 0, 0)
                """, DETAIL_ID, TASK_ID, goodQuantity, defectQuantity);
    }

    private void insertBarcode() {
        jdbcTemplate.update("""
                INSERT INTO barcode
                    (id, barcode_value, barcode_type_id, barcode_mode, product_id, batch_no,
                     task_id, source_type, barcode_status, create_by, is_deleted)
                VALUES (?, ?, 9300800, 2, 9300200, ?, ?, 2, 0, 1, 0)
                """, BARCODE_ID, BARCODE_VALUE, BATCH_NO, TASK_ID);
    }

    private void insertCompletionOrder(int finishStatus, int syncStatus) {
        jdbcTemplate.update("""
                INSERT INTO prod_finish_order
                    (id, finish_no, task_id, work_order_id, product_id, batch_no,
                     finish_quantity, good_quantity, defect_quantity, rework_quantity,
                     finish_status, sync_status, create_by, is_deleted)
                VALUES (?, 'M3-TX-FINISH', ?, 9300100, 9300200, ?,
                        10, 10, 0, 0, ?, ?, 1, 0)
                """, FINISH_ORDER_ID, TASK_ID, BATCH_NO, finishStatus, syncStatus);
    }

    private int count(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Integer.class, args);
    }

    private int value(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Integer.class, args);
    }

    private void login() {
        LoginUser user = new LoginUser();
        user.setUserId(1L);
        user.setWorkshopId(9300400L);
        user.setLineId(9300500L);
        user.setRoleCodes(List.of(RoleCodeConstants.ADMIN));
        SecurityContextHolder.set("m3-integration-token", user);
    }

    private void cleanup() {
        jdbcTemplate.update("DELETE FROM prod_finish_sync_record WHERE finish_order_id=?", FINISH_ORDER_ID);
        jdbcTemplate.update("DELETE FROM prod_finish_order WHERE id=?", FINISH_ORDER_ID);
        jdbcTemplate.update("DELETE FROM prod_report WHERE task_id=?", TASK_ID);
        jdbcTemplate.update("DELETE FROM barcode_use_record WHERE barcode_id=?", BARCODE_ID);
        jdbcTemplate.update("DELETE FROM barcode WHERE id=?", BARCODE_ID);
        jdbcTemplate.update("DELETE FROM prod_process_dispatch_detail WHERE id=?", DETAIL_ID);
        jdbcTemplate.update("DELETE FROM prod_task WHERE id=?", TASK_ID);
    }
}
