package com.badminton.mes.module.report.dal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.report.dal.ReportQueryRows.Aggregate;
import com.badminton.mes.module.report.dal.ReportQueryRows.RealtimeTask;
import com.badminton.mes.module.report.dal.ReportQueryRows.RealtimeSupport;
import com.badminton.mes.module.report.dal.ReportQueryRows.ReportDetail;
import com.badminton.mes.module.report.dal.ReportQueryRows.TraceBarcode;
import com.badminton.mes.module.report.dal.ReportQueryRows.TraceBarcodeUse;
import com.badminton.mes.module.report.dal.ReportQueryRows.TraceMaterial;
import com.badminton.mes.module.report.dal.ReportQueryRows.TraceOptionalSource;
import com.badminton.mes.module.report.dal.ReportQueryRows.TraceProcessHistory;
import com.badminton.mes.module.report.dal.ReportQueryRows.TraceTask;
import com.badminton.mes.module.report.dal.ReportQueryRows.TraceWorkOrder;
import com.badminton.mes.module.report.service.dto.DefectSourceRecord;
import com.badminton.mes.module.report.service.dto.ReportQueryCriteria;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * M4 报表与追溯只读查询 Repository。
 *
 * <p>所有动态条件仅拼接服务端固定 SQL 片段，外部值全部使用命名参数绑定。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
@Repository
public class ReportQueryRepository {

    private static final String REPORT_FROM = """
            FROM prod_report AS r
            INNER JOIN prod_task AS t ON t.id = r.task_id AND t.is_deleted = 0
            LEFT JOIN prod_process_dispatch_detail AS d
              ON d.id = r.dispatch_detail_id AND d.is_deleted = 0
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ReportQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 查询产量净额、发生额和冲销额汇总。 */
    public Aggregate aggregate(ReportQueryCriteria criteria) {
        QueryParts parts = reportFilters(criteria);
        String quantitySql = """
                SELECT
                  COALESCE(SUM(CASE WHEN r.record_type = 1 THEN r.input_quantity ELSE -r.input_quantity END), 0) AS input_quantity,
                  COALESCE(SUM(CASE WHEN r.record_type = 1 THEN r.good_quantity ELSE -r.good_quantity END), 0) AS good_quantity,
                  COALESCE(SUM(CASE WHEN r.record_type = 1 THEN r.defect_quantity ELSE -r.defect_quantity END), 0) AS defect_quantity,
                  COALESCE(SUM(CASE WHEN r.record_type = 1 THEN r.rework_quantity ELSE -r.rework_quantity END), 0) AS rework_quantity,
                  COALESCE(SUM(CASE WHEN r.record_type = 1 THEN r.input_quantity ELSE 0 END), 0) AS occurrence_input,
                  COALESCE(SUM(CASE WHEN r.record_type = 2 THEN r.input_quantity ELSE 0 END), 0) AS reversal_input,
                  COALESCE(SUM(CASE WHEN r.record_type = 1 THEN r.good_quantity ELSE 0 END), 0) AS occurrence_good,
                  COALESCE(SUM(CASE WHEN r.record_type = 2 THEN r.good_quantity ELSE 0 END), 0) AS reversal_good,
                  COALESCE(SUM(CASE WHEN r.record_type = 1 THEN r.defect_quantity ELSE 0 END), 0) AS occurrence_defect,
                  COALESCE(SUM(CASE WHEN r.record_type = 2 THEN r.defect_quantity ELSE 0 END), 0) AS reversal_defect
                """ + REPORT_FROM + parts.where();
        Aggregate quantity = jdbcTemplate.queryForObject(quantitySql, parts.parameters(), (rs, rowNum) ->
                new Aggregate(0L, rs.getLong("input_quantity"), rs.getLong("good_quantity"),
                        rs.getLong("defect_quantity"), rs.getLong("rework_quantity"), 0L,
                        rs.getLong("occurrence_input"), rs.getLong("reversal_input"),
                        rs.getLong("occurrence_good"), rs.getLong("reversal_good"),
                        rs.getLong("occurrence_defect"), rs.getLong("reversal_defect")));
        String taskSql = """
                SELECT COALESCE(SUM(x.plan_quantity), 0) AS plan_quantity,
                       COALESCE(SUM(x.finish_quantity), 0) AS finish_quantity
                FROM (
                  SELECT DISTINCT t.id, t.plan_quantity, t.finish_quantity
                """ + REPORT_FROM + parts.where() + ") AS x";
        long[] taskTotals = jdbcTemplate.queryForObject(taskSql, parts.parameters(), (rs, rowNum) ->
                new long[]{rs.getLong("plan_quantity"), rs.getLong("finish_quantity")});
        return new Aggregate(taskTotals[0], quantity.inputQuantity(), quantity.goodQuantity(),
                quantity.defectQuantity(), quantity.reworkQuantity(), taskTotals[1],
                quantity.occurrenceInputQuantity(), quantity.reversalInputQuantity(),
                quantity.occurrenceGoodQuantity(), quantity.reversalGoodQuantity(),
                quantity.occurrenceDefectQuantity(), quantity.reversalDefectQuantity());
    }

    /** 分页查询报工审计明细。 */
    public PageResult<ReportDetail> pageReports(ReportQueryCriteria criteria, int pageNo, int pageSize) {
        QueryParts parts = reportFilters(criteria);
        String countSql = "SELECT COUNT(*) " + REPORT_FROM + parts.where();
        long total = jdbcTemplate.queryForObject(countSql, parts.parameters(), Long.class);
        if (total == 0L) {
            return PageResult.empty(pageNo, pageSize);
        }
        int pages = (int) ((total + pageSize - 1) / pageSize);
        int effectivePageNo = Math.min(pageNo, pages);
        MapSqlParameterSource parameters = copy(parts.parameters())
                .addValue("limit", pageSize)
                .addValue("offset", (effectivePageNo - 1) * pageSize);
        String sql = reportDetailSelect() + REPORT_FROM + parts.where()
                + " ORDER BY r.report_time DESC, r.id DESC LIMIT :limit OFFSET :offset";
        List<ReportDetail> rows = jdbcTemplate.query(sql, parameters, this::mapReportDetail);
        return PageResult.of(rows, total, effectivePageNo, pageSize);
    }

    /** 查询同步导出的明细，limit 由 Service 传入最大行数加一。 */
    public List<ReportDetail> listReports(ReportQueryCriteria criteria, int limit) {
        QueryParts parts = reportFilters(criteria);
        MapSqlParameterSource parameters = copy(parts.parameters()).addValue("limit", limit);
        String sql = reportDetailSelect() + REPORT_FROM + parts.where()
                + " ORDER BY r.report_time DESC, r.id DESC LIMIT :limit";
        return jdbcTemplate.query(sql, parameters, this::mapReportDetail);
    }

    /** 查询当前授权范围内在制任务。 */
    public List<RealtimeTask> listRealtimeTasks(Long workshopId, Long lineId, Long productId) {
        StringBuilder sql = new StringBuilder("""
                SELECT t.id AS task_id, t.task_no, t.work_order_no, t.product_id, t.product_name,
                       t.batch_no, t.workshop_id, t.workshop_name, t.line_id, t.line_name,
                       t.plan_quantity, t.input_quantity, t.good_quantity, t.defect_quantity,
                       t.finish_quantity, t.task_status,
                       CASE WHEN COALESCE(b.is_abnormal, 0) = 1 THEN 1 ELSE 0 END AS abnormal,
                       t.actual_start_time, t.update_time
                FROM prod_task AS t
                LEFT JOIN prod_batch_status AS b
                  ON b.task_id = t.id AND b.is_deleted = 0
                WHERE t.is_deleted = 0 AND t.task_status IN (2, 3, 4)
                """);
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        appendEquals(sql, parameters, "t.workshop_id", "workshopId", workshopId);
        appendEquals(sql, parameters, "t.line_id", "lineId", lineId);
        appendEquals(sql, parameters, "t.product_id", "productId", productId);
        sql.append(" ORDER BY t.task_status DESC, t.id DESC");
        return jdbcTemplate.query(sql.toString(), parameters, this::mapRealtimeTask);
    }

    /** 查询授权范围内 C 组设备状态和未关闭安灯数量。 */
    public RealtimeSupport loadRealtimeSupport(Long workshopId, Long lineId) {
        StringBuilder equipmentSql = new StringBuilder("""
                SELECT COUNT(*) AS total_count,
                       COALESCE(SUM(CASE WHEN e.equipment_status = 'RUNNING' THEN 1 ELSE 0 END), 0)
                           AS running_count,
                       COALESCE(SUM(CASE WHEN e.equipment_status IN
                           ('STOPPED', 'REPAIRING', 'MAINTAINING') THEN 1 ELSE 0 END), 0)
                           AS unavailable_count
                FROM equip_ledger AS e
                WHERE e.is_deleted = 0 AND e.status = 1
                """);
        MapSqlParameterSource equipmentParameters = new MapSqlParameterSource();
        appendEquals(equipmentSql, equipmentParameters, "e.workshop_id", "workshopId", workshopId);
        appendEquals(equipmentSql, equipmentParameters, "e.production_line_id", "lineId", lineId);
        long[] equipment = jdbcTemplate.queryForObject(equipmentSql.toString(), equipmentParameters,
                (rs, rowNum) -> new long[]{rs.getLong("total_count"), rs.getLong("running_count"),
                        rs.getLong("unavailable_count")});

        StringBuilder andonSql = new StringBuilder("""
                SELECT COUNT(*) AS open_count,
                       COALESCE(SUM(CASE WHEN a.severity = 'CRITICAL' THEN 1 ELSE 0 END), 0)
                           AS critical_count
                FROM andon_event AS a
                WHERE a.is_deleted = 0 AND a.event_status <> 'CLOSED'
                """);
        MapSqlParameterSource andonParameters = new MapSqlParameterSource();
        appendEquals(andonSql, andonParameters, "a.workshop_id", "workshopId", workshopId);
        appendEquals(andonSql, andonParameters, "a.production_line_id", "lineId", lineId);
        long[] andon = jdbcTemplate.queryForObject(andonSql.toString(), andonParameters,
                (rs, rowNum) -> new long[]{rs.getLong("open_count"), rs.getLong("critical_count")});
        return new RealtimeSupport(equipment[0], equipment[1], equipment[2], andon[0], andon[1]);
    }

    /** 按批次、条码、工单号或任务号定位追溯主任务。 */
    public Optional<TraceTask> findTraceTask(String batchCode, String barcodeValue,
                                             String workOrderNo, String taskNo) {
        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT t.id, t.task_no, t.work_order_id, t.work_order_no, t.product_id,
                       t.product_code, t.product_name, t.batch_no, t.workshop_id, t.workshop_name,
                       t.line_id, t.line_name, t.plan_quantity, t.input_quantity, t.good_quantity,
                       t.defect_quantity, t.rework_quantity, t.finish_quantity, t.task_status,
                       t.actual_start_time, t.actual_end_time
                FROM prod_task AS t
                LEFT JOIN barcode AS b
                  ON (b.task_id = t.id OR (b.batch_no = t.batch_no AND b.product_id = t.product_id))
                 AND b.is_deleted = 0
                WHERE t.is_deleted = 0
                """);
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        appendEquals(sql, parameters, "t.batch_no", "batchCode", textOrNull(batchCode));
        appendEquals(sql, parameters, "b.barcode_value", "barcodeValue", textOrNull(barcodeValue));
        appendEquals(sql, parameters, "t.work_order_no", "workOrderNo", textOrNull(workOrderNo));
        appendEquals(sql, parameters, "t.task_no", "taskNo", textOrNull(taskNo));
        sql.append(" ORDER BY t.id DESC LIMIT 1");
        return jdbcTemplate.query(sql.toString(), parameters, this::mapTraceTask).stream().findFirst();
    }

    /** 查询追溯任务的上游工单。 */
    public Optional<TraceWorkOrder> findWorkOrder(Long workOrderId) {
        String sql = """
                SELECT w.id, w.work_order_no, w.batch_no, w.product_id, w.product_name, w.spec,
                       w.plan_quantity, w.input_quantity, w.finish_quantity, w.defect_quantity,
                       w.rework_quantity, w.order_status
                FROM prod_work_order AS w
                WHERE w.id = :workOrderId AND w.is_deleted = 0
                """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource("workOrderId", workOrderId),
                this::mapTraceWorkOrder).stream().findFirst();
    }

    /** 查询任务/批次关联条码。 */
    public List<TraceBarcode> listTraceBarcodes(TraceTask task) {
        String sql = """
                SELECT b.id, b.barcode_value, b.barcode_type_id, b.barcode_mode, b.product_id,
                       b.material_id, b.batch_no, b.barcode_status, b.create_time
                FROM barcode AS b
                WHERE b.is_deleted = 0
                  AND (b.task_id = :taskId OR (b.batch_no = :batchNo AND b.product_id = :productId))
                ORDER BY b.id ASC
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("taskId", task.id()).addValue("batchNo", task.batchNo())
                .addValue("productId", task.productId());
        return jdbcTemplate.query(sql, parameters, this::mapTraceBarcode);
    }

    /** 查询任务关联条码的扫码使用记录。 */
    public List<TraceBarcodeUse> listTraceBarcodeUses(Long taskId) {
        String sql = """
                SELECT u.id, u.barcode_id, u.process_id, u.user_id, u.equipment_id,
                       u.use_type, u.business_time
                FROM barcode_use_record AS u
                INNER JOIN barcode AS b ON b.id = u.barcode_id AND b.is_deleted = 0
                WHERE u.is_deleted = 0 AND (u.task_id = :taskId OR b.task_id = :taskId)
                ORDER BY u.business_time ASC, u.id ASC
                """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource("taskId", taskId),
                this::mapTraceBarcodeUse);
    }

    /** 查询批次工序履历。 */
    public List<TraceProcessHistory> listTraceProcessHistories(Long taskId) {
        String sql = """
                SELECT h.id, h.process_id, h.process_code, h.process_name, h.action_type,
                       h.operator_id, h.action_reason, h.operate_time
                FROM prod_batch_process_history AS h
                WHERE h.task_id = :taskId AND h.is_deleted = 0
                ORDER BY h.operate_time ASC, h.id ASC
                """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource("taskId", taskId),
                this::mapTraceProcessHistory);
    }

    /** 查询任务全部报工，追溯页面保留发生与冲销记录。 */
    public List<ReportDetail> listTraceReports(Long taskId) {
        String sql = reportDetailSelect() + REPORT_FROM
                + " WHERE r.is_deleted = 0 AND r.task_id = :taskId ORDER BY r.report_time ASC, r.id ASC";
        return jdbcTemplate.query(sql, new MapSqlParameterSource("taskId", taskId),
                this::mapReportDetail);
    }

    /** 查询工单物料需求，实际消耗批次等待跨组契约。 */
    public List<TraceMaterial> listTraceMaterials(Long workOrderId) {
        String sql = """
                SELECT wom.material_id, m.material_code, m.material_name,
                       wom.require_quantity, wom.issued_quantity
                FROM prod_work_order_material AS wom
                INNER JOIN base_material AS m ON m.id = wom.material_id AND m.is_deleted = 0
                WHERE wom.work_order_id = :workOrderId AND wom.is_deleted = 0
                ORDER BY wom.id ASC
                """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource("workOrderId", workOrderId),
                this::mapTraceMaterial);
    }

    /** 查询批次关联的 C 组质量检验结果。 */
    public List<TraceOptionalSource> listTraceQualityDefects(TraceTask task) {
        String sql = """
                SELECT q.inspection_no, q.conclusion, q.defect_quantity,
                       q.nonconformance_description, q.inspected_at
                FROM quality_inspection_record AS q
                WHERE q.is_deleted = 0 AND q.record_status = 'SUBMITTED'
                  AND (q.production_task_id = :taskId
                    OR (q.production_task_id IS NULL AND q.work_order_id = :workOrderId)
                    OR (q.production_task_id IS NULL AND q.work_order_id IS NULL
                        AND q.batch_no = :batchNo AND q.product_id = :productId))
                ORDER BY q.inspected_at ASC, q.id ASC
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("taskId", task.id()).addValue("workOrderId", task.workOrderId())
                .addValue("batchNo", task.batchNo()).addValue("productId", task.productId());
        return jdbcTemplate.query(sql, parameters, (rs, rowNum) -> {
            String description = rs.getString("nonconformance_description");
            String summary = rs.getString("conclusion") + "，不良数量 " + rs.getInt("defect_quantity");
            if (StringUtils.hasText(description)) {
                summary += "，" + description;
            }
            return new TraceOptionalSource("QUALITY_INSPECTION", rs.getString("inspection_no"),
                    summary, rs.getObject("inspected_at", LocalDateTime.class));
        });
    }

    /** 查询任务实际关联设备的当前状态。 */
    public List<TraceOptionalSource> listTraceEquipmentStatuses(TraceTask task) {
        String sql = """
                SELECT DISTINCT e.equipment_code, e.equipment_name, e.equipment_status, e.update_time
                FROM equip_ledger AS e
                WHERE e.is_deleted = 0
                  AND e.id IN (
                    SELECT u.equipment_id FROM barcode_use_record AS u
                    WHERE u.task_id = :taskId AND u.is_deleted = 0 AND u.equipment_id IS NOT NULL
                    UNION
                    SELECT d.equipment_id FROM prod_process_dispatch_detail AS d
                    WHERE d.task_id = :taskId AND d.is_deleted = 0 AND d.equipment_id IS NOT NULL
                  )
                ORDER BY e.equipment_code ASC
                """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource("taskId", task.id()),
                (rs, rowNum) -> new TraceOptionalSource("EQUIPMENT", rs.getString("equipment_code"),
                        rs.getString("equipment_name") + "，状态 " + rs.getString("equipment_status"),
                        rs.getObject("update_time", LocalDateTime.class)));
    }

    /** 查询任务、工单或批次关联的 C 组安灯异常。 */
    public List<TraceOptionalSource> listTraceAndonEvents(TraceTask task) {
        String sql = """
                SELECT a.event_no, a.severity, a.event_status, a.description, a.create_time
                FROM andon_event AS a
                WHERE a.is_deleted = 0
                  AND (a.production_task_id = :taskId
                    OR (a.production_task_id IS NULL AND a.work_order_id = :workOrderId)
                    OR (a.production_task_id IS NULL AND a.work_order_id IS NULL
                        AND a.batch_no = :batchNo))
                ORDER BY a.create_time ASC, a.id ASC
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("taskId", task.id()).addValue("workOrderId", task.workOrderId())
                .addValue("batchNo", task.batchNo());
        return jdbcTemplate.query(sql, parameters,
                (rs, rowNum) -> new TraceOptionalSource("ANDON", rs.getString("event_no"),
                        rs.getString("severity") + " / " + rs.getString("event_status")
                                + "，" + rs.getString("description"),
                        rs.getObject("create_time", LocalDateTime.class)));
    }

    /** 查询 B 组报工不良事实，数量按正常/冲销拆分。 */
    public List<DefectSourceRecord> listSceneDefects(ReportQueryCriteria criteria, int limit) {
        QueryParts parts = reportFilters(criteria);
        MapSqlParameterSource parameters = copy(parts.parameters()).addValue("limit", limit);
        String sql = """
                SELECT r.id AS source_id, dg.source_detail_id, dg.defect_group_no,
                       t.id AS task_id, t.task_no, t.work_order_no, t.product_id, t.product_name,
                       t.batch_no, t.workshop_id, t.line_id, r.process_id,
                       CASE WHEN r.record_type = 1 THEN r.defect_quantity ELSE 0 END AS occurrence_quantity,
                       CASE WHEN r.record_type = 2 THEN r.defect_quantity ELSE 0 END AS reversal_quantity,
                       r.report_time AS detected_time
                FROM prod_report AS r
                INNER JOIN prod_task AS t ON t.id = r.task_id AND t.is_deleted = 0
                LEFT JOIN (
                  SELECT d.report_id, MIN(d.id) AS source_detail_id,
                         MIN(d.defect_group_no) AS defect_group_no
                  FROM prod_report_defect AS d
                  WHERE d.is_deleted = 0
                  GROUP BY d.report_id
                ) AS dg ON dg.report_id = r.id
                """ + parts.where() + " AND r.defect_quantity > 0"
                + " ORDER BY r.report_time DESC, r.id DESC LIMIT :limit";
        return jdbcTemplate.query(sql, parameters, (rs, rowNum) -> new DefectSourceRecord(
                "SCENE_WORK_REPORT", rs.getLong("source_id"), nullableLong(rs, "source_detail_id"),
                rs.getString("defect_group_no"), rs.getLong("task_id"), rs.getString("task_no"),
                rs.getString("work_order_no"), rs.getLong("product_id"), rs.getString("product_name"),
                rs.getString("batch_no"), rs.getLong("workshop_id"), rs.getLong("line_id"),
                rs.getLong("process_id"), null, null, "未分类",
                rs.getLong("occurrence_quantity"), rs.getLong("reversal_quantity"),
                rs.getObject("detected_time", LocalDateTime.class)));
    }

    /** 查询 C 组已提交质检不良事实。 */
    public List<DefectSourceRecord> listQualityDefects(ReportQueryCriteria criteria, int limit) {
        QueryParts parts = qualityFilters(criteria);
        MapSqlParameterSource parameters = copy(parts.parameters()).addValue("limit", limit);
        String sql = """
                SELECT q.id AS source_id, q.defect_group_no, q.production_task_id AS task_id,
                       t.task_no, w.work_order_no, q.product_id, w.product_name, q.batch_no,
                       COALESCE(t.workshop_id, w.workshop_id) AS workshop_id,
                       COALESCE(t.line_id, q.production_line_id) AS line_id,
                       q.process_id, p.process_name, q.conclusion,
                       q.nonconformance_description, q.defect_quantity, q.inspected_at
                FROM quality_inspection_record AS q
                LEFT JOIN prod_task AS t ON t.id = q.production_task_id AND t.is_deleted = 0
                LEFT JOIN prod_work_order AS w ON w.id = q.work_order_id AND w.is_deleted = 0
                LEFT JOIN craft_process AS p ON p.id = q.process_id AND p.is_deleted = 0
                """ + parts.where()
                + " ORDER BY q.inspected_at DESC, q.id DESC LIMIT :limit";
        return jdbcTemplate.query(sql, parameters, (rs, rowNum) -> new DefectSourceRecord(
                "QUALITY_INSPECTION", rs.getLong("source_id"), null,
                rs.getString("defect_group_no"), nullableLong(rs, "task_id"), rs.getString("task_no"),
                rs.getString("work_order_no"), nullableLong(rs, "product_id"), rs.getString("product_name"),
                rs.getString("batch_no"), nullableLong(rs, "workshop_id"), nullableLong(rs, "line_id"),
                nullableLong(rs, "process_id"), rs.getString("process_name"), rs.getString("conclusion"),
                rs.getString("nonconformance_description"), rs.getLong("defect_quantity"), 0L,
                rs.getObject("inspected_at", LocalDateTime.class)));
    }

    private QueryParts reportFilters(ReportQueryCriteria criteria) {
        StringBuilder where = new StringBuilder(" WHERE r.is_deleted = 0")
                .append(" AND r.report_time >= :startTime AND r.report_time <= :endTime");
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("startTime", criteria.startTime()).addValue("endTime", criteria.endTime());
        appendEquals(where, parameters, "t.workshop_id", "workshopId", criteria.workshopId());
        appendEquals(where, parameters, "t.line_id", "lineId", criteria.lineId());
        appendEquals(where, parameters, "t.product_id", "productId", criteria.productId());
        appendEquals(where, parameters, "t.work_order_id", "workOrderId", criteria.workOrderId());
        appendEquals(where, parameters, "t.id", "taskId", criteria.taskId());
        appendEquals(where, parameters, "r.process_id", "processId", criteria.processId());
        appendEquals(where, parameters, "t.shift_id", "shiftId", criteria.shiftId());
        appendEquals(where, parameters, "t.batch_no", "batchNo", textOrNull(criteria.batchNo()));
        appendEquals(where, parameters, "t.task_status", "status", criteria.status());
        return new QueryParts(where.toString(), parameters);
    }

    private QueryParts qualityFilters(ReportQueryCriteria criteria) {
        StringBuilder where = new StringBuilder(" WHERE q.is_deleted = 0")
                .append(" AND q.record_status = 'SUBMITTED' AND q.conclusion <> 'PASS'")
                .append(" AND q.defect_quantity > 0")
                .append(" AND q.inspected_at >= :startTime AND q.inspected_at <= :endTime");
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("startTime", criteria.startTime()).addValue("endTime", criteria.endTime());
        appendEquals(where, parameters, "COALESCE(t.workshop_id, w.workshop_id)",
                "workshopId", criteria.workshopId());
        appendEquals(where, parameters, "COALESCE(t.line_id, q.production_line_id)",
                "lineId", criteria.lineId());
        appendEquals(where, parameters, "q.product_id", "productId", criteria.productId());
        appendEquals(where, parameters, "q.work_order_id", "workOrderId", criteria.workOrderId());
        appendEquals(where, parameters, "q.production_task_id", "taskId", criteria.taskId());
        appendEquals(where, parameters, "q.process_id", "processId", criteria.processId());
        appendEquals(where, parameters, "q.batch_no", "batchNo", textOrNull(criteria.batchNo()));
        return new QueryParts(where.toString(), parameters);
    }

    private String reportDetailSelect() {
        return """
                SELECT r.id AS report_id, r.report_no, t.id AS task_id, t.task_no,
                       t.work_order_no, t.product_id, t.product_name, t.batch_no,
                       t.workshop_id, t.workshop_name, t.line_id, t.line_name,
                       r.process_id, d.process_name, r.record_type, r.source_report_id,
                       r.input_quantity, r.good_quantity, r.defect_quantity, r.rework_quantity,
                       r.reverse_reason, r.report_time
                """;
    }

    private ReportDetail mapReportDetail(ResultSet rs, int rowNum) throws SQLException {
        return new ReportDetail(rs.getLong("report_id"), rs.getString("report_no"),
                rs.getLong("task_id"), rs.getString("task_no"), rs.getString("work_order_no"),
                rs.getLong("product_id"), rs.getString("product_name"), rs.getString("batch_no"),
                rs.getLong("workshop_id"), rs.getString("workshop_name"), rs.getLong("line_id"),
                rs.getString("line_name"), rs.getLong("process_id"), rs.getString("process_name"),
                rs.getInt("record_type"), nullableLong(rs, "source_report_id"),
                rs.getInt("input_quantity"), rs.getInt("good_quantity"),
                rs.getInt("defect_quantity"), rs.getInt("rework_quantity"), rs.getString("reverse_reason"),
                rs.getObject("report_time", LocalDateTime.class));
    }

    private RealtimeTask mapRealtimeTask(ResultSet rs, int rowNum) throws SQLException {
        return new RealtimeTask(rs.getLong("task_id"), rs.getString("task_no"),
                rs.getString("work_order_no"), rs.getLong("product_id"), rs.getString("product_name"),
                rs.getString("batch_no"), rs.getLong("workshop_id"), rs.getString("workshop_name"),
                rs.getLong("line_id"), rs.getString("line_name"), rs.getInt("plan_quantity"),
                rs.getInt("input_quantity"), rs.getInt("good_quantity"), rs.getInt("defect_quantity"),
                rs.getInt("finish_quantity"), rs.getInt("task_status"), rs.getBoolean("abnormal"),
                rs.getObject("actual_start_time", LocalDateTime.class),
                rs.getObject("update_time", LocalDateTime.class));
    }

    private TraceTask mapTraceTask(ResultSet rs, int rowNum) throws SQLException {
        return new TraceTask(rs.getLong("id"), rs.getString("task_no"), rs.getLong("work_order_id"),
                rs.getString("work_order_no"), rs.getLong("product_id"), rs.getString("product_code"),
                rs.getString("product_name"), rs.getString("batch_no"), rs.getLong("workshop_id"),
                rs.getString("workshop_name"), rs.getLong("line_id"), rs.getString("line_name"),
                rs.getInt("plan_quantity"), rs.getInt("input_quantity"), rs.getInt("good_quantity"),
                rs.getInt("defect_quantity"), rs.getInt("rework_quantity"), rs.getInt("finish_quantity"),
                rs.getInt("task_status"), rs.getObject("actual_start_time", LocalDateTime.class),
                rs.getObject("actual_end_time", LocalDateTime.class));
    }

    private TraceWorkOrder mapTraceWorkOrder(ResultSet rs, int rowNum) throws SQLException {
        return new TraceWorkOrder(rs.getLong("id"), rs.getString("work_order_no"),
                rs.getString("batch_no"), rs.getLong("product_id"), rs.getString("product_name"),
                rs.getString("spec"), rs.getInt("plan_quantity"), rs.getInt("input_quantity"),
                rs.getInt("finish_quantity"), rs.getInt("defect_quantity"),
                rs.getInt("rework_quantity"), rs.getInt("order_status"));
    }

    private TraceBarcode mapTraceBarcode(ResultSet rs, int rowNum) throws SQLException {
        return new TraceBarcode(rs.getLong("id"), rs.getString("barcode_value"),
                rs.getLong("barcode_type_id"), rs.getInt("barcode_mode"),
                nullableLong(rs, "product_id"), nullableLong(rs, "material_id"),
                rs.getString("batch_no"), rs.getInt("barcode_status"),
                rs.getObject("create_time", LocalDateTime.class));
    }

    private TraceBarcodeUse mapTraceBarcodeUse(ResultSet rs, int rowNum) throws SQLException {
        return new TraceBarcodeUse(rs.getLong("id"), rs.getLong("barcode_id"),
                nullableLong(rs, "process_id"), rs.getLong("user_id"),
                nullableLong(rs, "equipment_id"), rs.getInt("use_type"),
                rs.getObject("business_time", LocalDateTime.class));
    }

    private TraceProcessHistory mapTraceProcessHistory(ResultSet rs, int rowNum) throws SQLException {
        return new TraceProcessHistory(rs.getLong("id"), nullableLong(rs, "process_id"),
                rs.getString("process_code"), rs.getString("process_name"), rs.getInt("action_type"),
                rs.getLong("operator_id"), rs.getString("action_reason"),
                rs.getObject("operate_time", LocalDateTime.class));
    }

    private TraceMaterial mapTraceMaterial(ResultSet rs, int rowNum) throws SQLException {
        return new TraceMaterial(rs.getLong("material_id"), rs.getString("material_code"),
                rs.getString("material_name"), rs.getBigDecimal("require_quantity"),
                rs.getBigDecimal("issued_quantity"));
    }

    private Long nullableLong(ResultSet rs, String columnName) throws SQLException {
        return rs.getObject(columnName, Long.class);
    }

    private void appendEquals(StringBuilder sql, MapSqlParameterSource parameters,
                              String column, String parameter, Object value) {
        if (value != null) {
            sql.append(" AND ").append(column).append(" = :").append(parameter);
            parameters.addValue(parameter, value);
        }
    }

    private MapSqlParameterSource copy(MapSqlParameterSource source) {
        MapSqlParameterSource copy = new MapSqlParameterSource();
        for (String parameterName : source.getParameterNames()) {
            copy.addValue(parameterName, source.getValue(parameterName));
        }
        return copy;
    }

    private String textOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record QueryParts(String where, MapSqlParameterSource parameters) {
    }
}
