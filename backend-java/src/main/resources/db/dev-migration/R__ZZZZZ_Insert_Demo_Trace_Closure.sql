-- 答辩追溯闭环补数：为演示工单补齐物料需求，并为首个批次补齐返修闭环。
INSERT INTO prod_work_order_material (work_order_id, material_id, require_quantity, issued_quantity)
SELECT w.id, m.id,
       CASE m.material_code
         WHEN 'M001' THEN 2800.0000
         WHEN 'M002' THEN 44800.0000
         WHEN 'M003' THEN 56.0000
         ELSE 233.5200
       END,
       CASE m.material_code
         WHEN 'M003' THEN 20.0000
         ELSE 999999.0000
       END
FROM prod_work_order AS w
CROSS JOIN base_material AS m
WHERE w.work_order_no LIKE 'WO-DEMO-%'
  AND m.material_code IN ('M001', 'M002', 'M003', 'M004')
ON DUPLICATE KEY UPDATE
  require_quantity = VALUES(require_quantity),
  issued_quantity = VALUES(issued_quantity),
  is_deleted = 0;

INSERT INTO prod_work_order_status_log
    (work_order_id, from_status, to_status, change_type, change_reason, operate_by, operate_time)
SELECT w.id, 0, 1, 1, 'DEMO_SEED_RELEASE', 1, w.plan_start_time
FROM prod_work_order AS w
WHERE w.work_order_no = 'WO-DEMO-20260705-P001'
  AND NOT EXISTS (
    SELECT 1 FROM prod_work_order_status_log AS l
    WHERE l.work_order_id = w.id AND l.change_reason = 'DEMO_SEED_RELEASE'
  );

INSERT INTO prod_work_order_status_log
    (work_order_id, from_status, to_status, change_type, change_reason, operate_by, operate_time)
SELECT w.id, 1, 2, 1, 'DEMO_SEED_START', 1, w.plan_start_time
FROM prod_work_order AS w
WHERE w.work_order_no = 'WO-DEMO-20260705-P001'
  AND NOT EXISTS (
    SELECT 1 FROM prod_work_order_status_log AS l
    WHERE l.work_order_id = w.id AND l.change_reason = 'DEMO_SEED_START'
  );

INSERT INTO scene_repair_work_order
    (repair_no, source_report_id, task_id, batch_no, defect_quantity, repair_quantity,
     status, reason, assignee_id, recheck_result, recheck_quantity, created_by, created_time, updated_time)
SELECT 'RW-DEMO-20260705-001', r.id, r.task_id, r.batch_no, 1, 1,
       'CLOSED', 'DEMO_SEED_DEFECT', 1, 'RELEASED', 1, 1, r.report_time, r.report_time
FROM prod_report AS r
WHERE r.task_id = (SELECT t.id FROM prod_task AS t WHERE t.task_no = 'TASK-DEMO-20260705-P001')
  AND r.defect_quantity > 0
ORDER BY r.id
LIMIT 1
ON DUPLICATE KEY UPDATE
  task_id = VALUES(task_id), batch_no = VALUES(batch_no), defect_quantity = VALUES(defect_quantity),
  repair_quantity = VALUES(repair_quantity), status = VALUES(status), recheck_result = VALUES(recheck_result),
  recheck_quantity = VALUES(recheck_quantity), updated_time = VALUES(updated_time), is_deleted = 0;

INSERT INTO scene_repair_record (repair_work_order_id, quantity, description, operator_id, created_time)
SELECT r.id, 1, 'DEMO_SEED_REPAIR', 1, r.updated_time
FROM scene_repair_work_order AS r
WHERE r.repair_no = 'RW-DEMO-20260705-001'
  AND NOT EXISTS (
    SELECT 1 FROM scene_repair_record AS d
    WHERE d.repair_work_order_id = r.id AND d.description = 'DEMO_SEED_REPAIR'
  );

INSERT INTO scene_repair_recheck_record (repair_work_order_id, result, quantity, inspector_id, created_time)
SELECT r.id, 'RELEASED', 1, 1, r.updated_time
FROM scene_repair_work_order AS r
WHERE r.repair_no = 'RW-DEMO-20260705-001'
  AND NOT EXISTS (
    SELECT 1 FROM scene_repair_recheck_record AS d
    WHERE d.repair_work_order_id = r.id AND d.result = 'RELEASED'
  );
