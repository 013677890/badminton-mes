-- -----------------------------------------------------------------------------
-- 本地 Docker Compose 演示数据：从 2026-07-05 到容器当前日期的生产事实。
--
-- 仅通过 dev profile 的 MES_FLYWAY_LOCATIONS 加载；所有业务编号使用 DEMO 前缀，
-- 每次重复迁移执行时先清理旧数据再重建，保证报表与电子看板数据可重复验证。
-- -----------------------------------------------------------------------------

DELETE FROM prod_report WHERE report_no LIKE 'RPT-DEMO-%';
DELETE FROM prod_batch_status WHERE batch_no LIKE 'DEMO-%';
DELETE FROM prod_process_dispatch_detail WHERE dispatch_id IN (
    SELECT id FROM prod_process_dispatch WHERE dispatch_no LIKE 'PD-DEMO-%'
);
DELETE FROM prod_process_dispatch WHERE dispatch_no LIKE 'PD-DEMO-%';
DELETE FROM prod_task WHERE task_no LIKE 'TASK-DEMO-%';
DELETE FROM prod_work_order WHERE work_order_no LIKE 'WO-DEMO-%';

-- 最小工艺主档。与历史迁移已有记录兼容，不覆盖用户维护的数据。
INSERT INTO craft_process (
    process_code, process_name, process_type, standard_time_seconds,
    is_key_process, is_quality_required, is_scan_required, is_piece_rate_enabled,
    status, version, create_by, update_by
)
SELECT 'DEMO-PREP', '演示准备工序', 'PROCESS', 45, 1, 0, 1, 1, 1, 0, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM craft_process WHERE process_code = 'DEMO-PREP');

INSERT INTO craft_process (
    process_code, process_name, process_type, standard_time_seconds,
    is_key_process, is_quality_required, is_scan_required, is_piece_rate_enabled,
    status, version, create_by, update_by
)
SELECT 'DEMO-INSPECT', '演示终检工序', 'INSPECT', 60, 1, 1, 1, 0, 1, 0, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM craft_process WHERE process_code = 'DEMO-INSPECT');

INSERT INTO craft_routing (
    routing_code, routing_name, version, source_type, routing_status,
    audit_by, audit_time, create_by
)
SELECT 'DEMO-ROUTE', '电子看板演示路线', 'V1', 1, 1, 1, NOW(), 1
WHERE NOT EXISTS (
    SELECT 1 FROM craft_routing WHERE routing_code = 'DEMO-ROUTE' AND version = 'V1'
);

-- 每日两类产品各一张工单，覆盖 7 月 5 日到启动当日。
INSERT INTO prod_work_order (
    work_order_no, source_type, product_id, product_name, spec, unit_id, batch_no,
    workshop_id, plan_quantity, dispatched_quantity, input_quantity, finish_quantity,
    defect_quantity, rework_quantity, priority, plan_start_time, plan_end_time,
    order_status, kit_status, create_by
)
WITH RECURSIVE date_series AS (
    SELECT DATE('2026-07-05') AS work_date
    UNION ALL
    SELECT work_date + INTERVAL 1 DAY FROM date_series WHERE work_date < CURDATE()
)
SELECT
    CONCAT('WO-DEMO-', DATE_FORMAT(ds.work_date, '%Y%m%d'), '-', p.product_code),
    1, p.id, p.product_name, p.spec, p.unit_id,
    CONCAT('DEMO-', DATE_FORMAT(ds.work_date, '%Y%m%d'), '-', p.product_code),
    w.id, 3000 + IF(p.product_code = 'P001', 400, 0),
    2800, 2800, 2720, 80, 20, 5,
    ds.work_date + INTERVAL 8 HOUR, ds.work_date + INTERVAL 18 HOUR,
    IF(ds.work_date = CURDATE(), 2, 4), 1, 1
FROM date_series ds
JOIN base_product p ON p.product_code IN ('P001', 'P002') AND p.is_deleted = 0
JOIN base_workshop w ON w.workshop_code = 'WS001' AND w.is_deleted = 0;

-- 工单快照、任务状态和当日产线状态为实时看板提供数据源。
INSERT INTO prod_task (
    task_no, source_type, work_order_id, work_order_no, product_id, product_code,
    product_name, batch_no, routing_id, routing_code, routing_version,
    workshop_id, workshop_name, line_id, line_name, shift_id, plan_date,
    plan_quantity, input_quantity, good_quantity, defect_quantity, rework_quantity,
    finish_quantity, plan_start_time, plan_end_time, actual_start_time, actual_end_time,
    task_status, create_by
)
SELECT
    CONCAT('TASK-DEMO-', DATE_FORMAT(w.plan_start_time, '%Y%m%d'), '-', p.product_code),
    2, w.id, w.work_order_no, p.id, p.product_code, p.product_name, w.batch_no,
    r.id, r.routing_code, r.version,
    ws.id, ws.workshop_name, l.id, l.line_name, s.id, DATE(w.plan_start_time),
    w.plan_quantity, 2800, 2720, 80, 20, 2740,
    w.plan_start_time, w.plan_end_time, w.plan_start_time,
    IF(DATE(w.plan_start_time) = CURDATE(), NULL, w.plan_end_time),
    CASE
        WHEN DATE(w.plan_start_time) = CURDATE() AND p.product_code = 'P001' THEN 3
        WHEN DATE(w.plan_start_time) = CURDATE() THEN 4
        ELSE 5
    END,
    1
FROM prod_work_order w
JOIN base_product p ON p.id = w.product_id
JOIN base_workshop ws ON ws.id = w.workshop_id
JOIN base_production_line l ON l.line_code = 'LINE-01' AND l.is_deleted = 0
JOIN base_shift s ON s.shift_code = 'DAY' AND s.is_deleted = 0
JOIN craft_routing r ON r.routing_code = 'DEMO-ROUTE' AND r.version = 'V1' AND r.is_deleted = 0
WHERE w.work_order_no LIKE 'WO-DEMO-%';

INSERT INTO prod_batch_status (
    batch_no, task_id, product_id, current_process_id, current_process_name,
    batch_status, is_abnormal
)
SELECT
    t.batch_no, t.id, t.product_id, inspect.id, inspect.process_name,
    CASE WHEN t.task_status = 5 THEN 5 WHEN t.task_status = 4 THEN 4 ELSE 1 END,
    CASE WHEN t.product_code = 'P002' AND t.task_status <> 5 THEN 1 ELSE 0 END
FROM prod_task t
JOIN craft_process inspect ON inspect.process_code = 'DEMO-INSPECT'
WHERE t.task_no LIKE 'TASK-DEMO-%';

INSERT INTO prod_process_dispatch (
    dispatch_no, task_id, routing_id, routing_code, routing_version, dispatch_status, create_by
)
SELECT
    CONCAT('PD-DEMO-', DATE_FORMAT(t.plan_date, '%Y%m%d'), '-', t.product_code),
    t.id, t.routing_id, t.routing_code, t.routing_version,
    CASE WHEN t.task_status = 5 THEN 3 ELSE 2 END, 1
FROM prod_task t
WHERE t.task_no LIKE 'TASK-DEMO-%';

INSERT INTO prod_process_dispatch_detail (
    dispatch_id, task_id, process_id, process_code, process_name, seq,
    is_key, is_inspect, is_scan, plan_quantity, good_quantity, defect_quantity,
    detail_status, is_paused, actual_start_time, actual_end_time
)
SELECT
    d.id, t.id, p.id, p.process_code, p.process_name, steps.seq,
    p.is_key_process, p.is_quality_required, p.is_scan_required,
    t.plan_quantity, 2720, 80,
    CASE WHEN t.task_status = 5 THEN 2 ELSE 1 END,
    CASE WHEN t.task_status = 4 THEN 1 ELSE 0 END,
    t.actual_start_time,
    CASE WHEN t.task_status = 5 THEN t.actual_end_time ELSE NULL END
FROM prod_process_dispatch d
JOIN prod_task t ON t.id = d.task_id
JOIN (
    SELECT 1 AS seq, 'DEMO-PREP' AS process_code
    UNION ALL SELECT 2, 'DEMO-INSPECT'
) steps
JOIN craft_process p ON p.process_code = steps.process_code
WHERE d.dispatch_no LIKE 'PD-DEMO-%';

-- 每任务两道工序、每道工序四次报工，共生成 8 × 天数 × 产品数条事实数据。
INSERT INTO prod_report (
    report_no, request_no, task_id, dispatch_detail_id, process_id, batch_no,
    report_type, record_type, user_id, input_quantity, good_quantity,
    defect_quantity, rework_quantity, source_type, report_time
)
SELECT
    CONCAT('RPT-DEMO-', DATE_FORMAT(t.plan_date, '%Y%m%d'), '-', t.id, '-', d.seq, '-', n.step_no),
    CONCAT('REQ-DEMO-', t.id, '-', d.id, '-', n.step_no),
    t.id, d.id, d.process_id, t.batch_no,
    1, 1, 1, 350, 340, 10, 2, 2,
    t.plan_start_time + INTERVAL (n.step_no * 2 + d.seq) HOUR
FROM prod_task t
JOIN prod_process_dispatch_detail d ON d.task_id = t.id AND d.is_deleted = 0
JOIN (
    SELECT 1 AS step_no UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
) n
WHERE t.task_no LIKE 'TASK-DEMO-%';
