-- 现场追溯、质量不良与完工读取演示数据：均关联既有 2026-07 报工。
DROP TEMPORARY TABLE IF EXISTS demo_scene_sequence;
CREATE TEMPORARY TABLE demo_scene_sequence (n INT NOT NULL PRIMARY KEY);
INSERT INTO demo_scene_sequence (n)
WITH digits AS (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9)
SELECT d1.n + d2.n * 10 + d3.n * 100 + d4.n * 1000 + 1
FROM digits d1 CROSS JOIN digits d2 CROSS JOIN digits d3 CROSS JOIN digits d4
WHERE d1.n + d2.n * 10 + d3.n * 100 + d4.n * 1000 < 2000;

DROP TEMPORARY TABLE IF EXISTS demo_scene_report;
CREATE TEMPORARY TABLE demo_scene_report AS
SELECT ROW_NUMBER() OVER (ORDER BY r.id) AS n, r.id, r.task_id, r.dispatch_detail_id, r.process_id, r.batch_no, r.report_time
FROM prod_report r WHERE r.report_no LIKE 'RPT-DEMO-%' LIMIT 2000;

DELETE FROM integration_completion_read_log WHERE completion_no LIKE 'CO-DEMO-%';
DELETE FROM prod_completion_order WHERE completion_no LIKE 'CO-DEMO-%';
DELETE FROM prod_report_defect WHERE defect_group_no LIKE 'DEF-DEMO-%';
DELETE FROM prod_batch_process_history WHERE action_reason = 'DEMO_SEED';
DELETE FROM prod_batch_status_history WHERE change_reason = 'DEMO_SEED';
DELETE FROM prod_task_operate_log WHERE reason = 'DEMO_SEED';
DELETE FROM craft_process_defect_reason WHERE reason_code LIKE 'DR-DEMO-%';

INSERT INTO craft_process_defect_reason (process_id, reason_code, reason_name, status, version, create_by, update_by)
SELECT r.process_id, CONCAT('DR-DEMO-', LPAD(r.n, 4, '0')), CONCAT('演示不良原因', r.n), 1, 1, 1, 1
FROM demo_scene_report r;

INSERT INTO prod_report_defect (report_id, defect_reason_id, defect_quantity, defect_position, handle_type, defect_group_no)
SELECT r.id, d.id, 1, '羽球头部', 1, CONCAT('DEF-DEMO-', LPAD(r.n, 4, '0'))
FROM demo_scene_report r JOIN craft_process_defect_reason d ON d.reason_code = CONCAT('DR-DEMO-', LPAD(r.n, 4, '0'));

INSERT INTO prod_task_operate_log (task_id, operate_type, from_status, to_status, reason, terminal_type, operator_id, operate_time)
SELECT r.task_id, 1, 2, 3, 'DEMO_SEED', 1, 1, r.report_time FROM demo_scene_report r;

INSERT INTO prod_batch_status_history (batch_status_id, task_id, batch_no, from_status, to_status, process_id, change_reason, operator_id, operate_time)
SELECT b.id, r.task_id, r.batch_no, 1, 2, r.process_id, 'DEMO_SEED', 1, r.report_time
FROM demo_scene_report r JOIN prod_batch_status b ON b.task_id = r.task_id;

INSERT INTO prod_batch_process_history (batch_status_id, task_id, dispatch_detail_id, batch_no, process_id, process_code, process_name, action_type, operator_id, action_reason, operate_time)
SELECT b.id, r.task_id, r.dispatch_detail_id, r.batch_no, r.process_id, d.process_code, d.process_name, 5, 1, 'DEMO_SEED', r.report_time
FROM demo_scene_report r JOIN prod_batch_status b ON b.task_id = r.task_id JOIN prod_process_dispatch_detail d ON d.id = r.dispatch_detail_id;

INSERT INTO prod_completion_order (completion_no, work_order_id, work_order_no, product_id, product_code, product_name, batch_no, completion_quantity, good_quantity, defect_quantity, audit_status, audit_by, audit_time, create_by, update_by)
SELECT CONCAT('CO-DEMO-', LPAD(r.n, 4, '0')), t.work_order_id, t.work_order_no, t.product_id, t.product_code, t.product_name, r.batch_no, 5, 5, 0, 1, 1, r.report_time, 1, 1
FROM demo_scene_report r JOIN prod_task t ON t.id = r.task_id;

INSERT INTO integration_completion_read_log (completion_order_id, completion_no, work_order_no, source_system, read_by, read_time)
SELECT c.id, c.completion_no, c.work_order_no, 'DEMO', 1, c.audit_time FROM prod_completion_order c WHERE c.completion_no LIKE 'CO-DEMO-%';

DROP TEMPORARY TABLE IF EXISTS demo_scene_report;
DROP TEMPORARY TABLE IF EXISTS demo_scene_sequence;
