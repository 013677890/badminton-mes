-- -----------------------------------------------------------------------------
-- 本地 Compose 跨模块关联 DEMO 数据。仅由 dev profile 加载，所有业务标识使用 DEMO。
-- 每张表目标 2,000 行；本脚本在生产 DEMO 数据之后执行，可重复迁移。
-- -----------------------------------------------------------------------------

DROP TEMPORARY TABLE IF EXISTS demo_sequence;
CREATE TEMPORARY TABLE demo_sequence (n INT NOT NULL PRIMARY KEY);
INSERT INTO demo_sequence (n)
WITH digits AS (
    SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
)
SELECT d1.n + d2.n * 10 + d3.n * 100 + d4.n * 1000 + 1
FROM digits AS d1
CROSS JOIN digits AS d2
CROSS JOIN digits AS d3
CROSS JOIN digits AS d4
WHERE d1.n + d2.n * 10 + d3.n * 100 + d4.n * 1000 < 2000;

DROP TEMPORARY TABLE IF EXISTS demo_task_map;
CREATE TEMPORARY TABLE demo_task_map AS
SELECT ROW_NUMBER() OVER (ORDER BY t.id) AS n, t.id, t.work_order_id, t.product_id,
       t.line_id, t.batch_no
FROM prod_task AS t
WHERE t.task_no LIKE 'TASK-DEMO-%';

-- ------------------------------ 质量域 --------------------------------------
DELETE FROM quality_inspection_result WHERE item_code_snapshot LIKE 'QI-DEMO-%';
DELETE FROM quality_inspection_record WHERE inspection_no LIKE 'QIR-DEMO-%';
DELETE FROM quality_inspection_plan_item WHERE plan_id IN (
    SELECT id FROM quality_inspection_plan WHERE plan_code LIKE 'QIP-DEMO-%'
);
DELETE FROM quality_inspection_plan WHERE plan_code LIKE 'QIP-DEMO-%';
DELETE FROM quality_inspection_item WHERE item_code LIKE 'QI-DEMO-%';
DELETE FROM quality_inspection_category WHERE category_code LIKE 'QC-DEMO-%';

INSERT INTO quality_inspection_category (category_code, category_name, enabled_status, remark, create_by)
SELECT CONCAT('QC-DEMO-', LPAD(s.n, 4, '0')), CONCAT('演示质量分类', s.n), 1, '跨模块演示数据', 1
FROM demo_sequence AS s;

INSERT INTO quality_inspection_item (
    item_code, item_name, category_id, value_type, unit, standard_value,
    lower_limit, upper_limit, judgment_method, inspection_method, required_flag,
    enabled_status, remark, create_by
)
SELECT CONCAT('QI-DEMO-', LPAD(s.n, 4, '0')), CONCAT('演示检验项', s.n), c.id,
       'NUMBER', '件', '合格', 0, 100, 'RANGE', '抽样检验', 1, 1, '跨模块演示数据', 1
FROM demo_sequence AS s
JOIN quality_inspection_category AS c ON c.category_code = CONCAT('QC-DEMO-', LPAD(s.n, 4, '0'));

INSERT INTO quality_inspection_plan (
    plan_code, plan_name, product_id, inspection_type, version_no, plan_status,
    effective_date, default_flag, remark, create_by, audit_by, audit_time
)
SELECT CONCAT('QIP-DEMO-', LPAD(s.n, 4, '0')), CONCAT('演示检验计划', s.n),
       (SELECT MIN(p.id) FROM base_product AS p WHERE p.is_deleted = 0),
       'PATROL', 1, 'APPROVED', DATE('2026-07-05'), 0, '跨模块演示数据', 1, 1, NOW()
FROM demo_sequence AS s;

INSERT INTO quality_inspection_plan_item (
    plan_id, inspection_item_id, sort_order, sample_quantity, required_flag,
    standard_value, lower_limit, upper_limit, judgment_method
)
SELECT p.id, i.id, 1, 10, 1, '合格', 0, 100, 'RANGE'
FROM quality_inspection_plan AS p
JOIN quality_inspection_item AS i
  ON i.item_code = REPLACE(p.plan_code, 'QIP-', 'QI-')
WHERE p.plan_code LIKE 'QIP-DEMO-%';

INSERT INTO quality_inspection_record (
    inspection_no, inspection_type, plan_id, plan_code_snapshot, plan_version_snapshot,
    work_order_id, production_task_id, product_id, production_line_id, process_id,
    batch_no, sample_quantity, record_status, conclusion, release_status,
    defect_group_no, defect_quantity, nonconformance_description, disposition,
    inspector_id, inspected_at, create_by
)
SELECT CONCAT('QIR-DEMO-', LPAD(s.n, 4, '0')), 'PATROL', p.id, p.plan_code, 1,
       t.work_order_id, t.id, t.product_id, t.line_id,
       (SELECT MIN(d.process_id) FROM prod_process_dispatch_detail AS d WHERE d.task_id = t.id),
       t.batch_no, 10, 'SUBMITTED', IF(MOD(s.n, 10) = 0, 'REWORK', 'PASS'),
       IF(MOD(s.n, 10) = 0, 'BLOCKED', 'RELEASED'), CONCAT('QDG-DEMO-', s.n),
       IF(MOD(s.n, 10) = 0, 1, 0), IF(MOD(s.n, 10) = 0, '演示不良', NULL),
       IF(MOD(s.n, 10) = 0, '返修', NULL), 1, DATE_ADD('2026-07-05 09:00:00', INTERVAL s.n MINUTE), 1
FROM demo_sequence AS s
JOIN quality_inspection_plan AS p ON p.plan_code = CONCAT('QIP-DEMO-', LPAD(s.n, 4, '0'))
JOIN demo_task_map AS m ON m.n = MOD(s.n - 1, 24) + 1
JOIN prod_task AS t ON t.id = m.id;

INSERT INTO quality_inspection_result (
    inspection_record_id, inspection_item_id, item_code_snapshot, item_name_snapshot,
    value_type_snapshot, unit_snapshot, required_flag, standard_value_snapshot,
    lower_limit_snapshot, upper_limit_snapshot, judgment_method_snapshot, measured_value,
    judgment_result, defect_description, sort_order
)
SELECT r.id, i.id, i.item_code, i.item_name, i.value_type, i.unit, 1, i.standard_value,
       i.lower_limit, i.upper_limit, i.judgment_method, '95',
       IF(r.conclusion = 'PASS', 'PASS', 'FAIL'), r.nonconformance_description, 1
FROM quality_inspection_record AS r
JOIN quality_inspection_item AS i
  ON i.item_code = REPLACE((SELECT p.plan_code FROM quality_inspection_plan AS p WHERE p.id = r.plan_id), 'QIP-', 'QI-')
WHERE r.inspection_no LIKE 'QIR-DEMO-%';

-- ------------------------------ 条码域 --------------------------------------
DELETE FROM barcode_print_record WHERE barcode_id IN (SELECT id FROM barcode WHERE barcode_value LIKE 'BC-DEMO-%');
DELETE FROM barcode_use_record WHERE barcode_id IN (SELECT id FROM barcode WHERE barcode_value LIKE 'BC-DEMO-%');
DELETE FROM barcode WHERE barcode_value LIKE 'BC-DEMO-%';
DELETE FROM barcode_apply_rule WHERE version = 'DEMO';
DELETE FROM barcode_rule_item WHERE rule_id IN (SELECT id FROM barcode_rule WHERE rule_code LIKE 'BR-DEMO-%');
DELETE FROM barcode_serial WHERE rule_id IN (SELECT id FROM barcode_rule WHERE rule_code LIKE 'BR-DEMO-%');
DELETE FROM barcode_rule WHERE rule_code LIKE 'BR-DEMO-%';
DELETE FROM barcode_template_field WHERE template_id IN (SELECT id FROM barcode_template WHERE template_code LIKE 'BT-DEMO-%');
DELETE FROM barcode_template WHERE template_code LIKE 'BT-DEMO-%';
DELETE FROM barcode_type WHERE type_code LIKE 'BCT-DEMO-%';

INSERT INTO barcode_type (type_code, type_name, apply_object, status)
SELECT CONCAT('BCT-DEMO-', LPAD(n, 4, '0')), CONCAT('演示条码类型', n), 'PRODUCT', 1 FROM demo_sequence;
INSERT INTO barcode_template (template_code, template_name, paper_width, paper_height, version, status)
SELECT CONCAT('BT-DEMO-', LPAD(n, 4, '0')), CONCAT('演示标签模板', n), 50, 30, 'DEMO', 1 FROM demo_sequence;
INSERT INTO barcode_rule (rule_code, rule_name, barcode_type_id, serial_length, serial_reset_cycle, status)
SELECT CONCAT('BR-DEMO-', LPAD(s.n, 4, '0')), CONCAT('演示编码规则', s.n), t.id, 8, 1, 1
FROM demo_sequence AS s JOIN barcode_type AS t ON t.type_code = CONCAT('BCT-DEMO-', LPAD(s.n, 4, '0'));
INSERT INTO barcode_rule_item (rule_id, seq, item_type, item_value, item_length)
SELECT r.id, 1, 1, 'DEMO', 8 FROM barcode_rule AS r WHERE r.rule_code LIKE 'BR-DEMO-%';
INSERT INTO barcode_serial (rule_id, serial_scope, current_serial)
SELECT r.id, CONCAT('DEMO-SCOPE-', r.id), 2000 FROM barcode_rule AS r WHERE r.rule_code LIKE 'BR-DEMO-%';
INSERT INTO barcode_template_field (template_id, field_name, field_type, data_source, pos_x, pos_y, font_size)
SELECT t.id, 'barcodeValue', 1, 'barcode_value', 5, 5, 12 FROM barcode_template AS t WHERE t.template_code LIKE 'BT-DEMO-%';
INSERT INTO barcode_apply_rule (
    object_type, product_id, barcode_type_id, barcode_mode, rule_id, template_id,
    source_type, is_default, version, status
)
SELECT 1, (SELECT MIN(p.id) FROM base_product AS p WHERE p.is_deleted = 0), ty.id, 2, ru.id, te.id, 1, 0, 'DEMO', 1
FROM demo_sequence AS s
JOIN barcode_type AS ty ON ty.type_code = CONCAT('BCT-DEMO-', LPAD(s.n, 4, '0'))
JOIN barcode_rule AS ru ON ru.rule_code = CONCAT('BR-DEMO-', LPAD(s.n, 4, '0'))
JOIN barcode_template AS te ON te.template_code = CONCAT('BT-DEMO-', LPAD(s.n, 4, '0'));
INSERT INTO barcode (barcode_value, barcode_type_id, barcode_mode, apply_rule_id, product_id, batch_no, work_order_id, task_id, source_type, barcode_status, create_by)
SELECT CONCAT('BC-DEMO-', LPAD(s.n, 6, '0')), ar.barcode_type_id, 2, ar.id, t.product_id, t.batch_no, t.work_order_id, t.id, 1, 1, 1
FROM demo_sequence AS s
JOIN barcode_apply_rule AS ar ON ar.version = 'DEMO' AND ar.id = (SELECT MIN(a.id) + s.n - 1 FROM barcode_apply_rule AS a WHERE a.version = 'DEMO')
JOIN demo_task_map AS m ON m.n = MOD(s.n - 1, 24) + 1
JOIN prod_task AS t ON t.id = m.id;
INSERT INTO barcode_use_record (barcode_id, task_id, process_id, user_id, equipment_id, use_type, business_time)
SELECT b.id, b.task_id, (SELECT MIN(d.process_id) FROM prod_process_dispatch_detail AS d WHERE d.task_id = b.task_id), 1,
       (SELECT MIN(e.id) FROM equip_ledger AS e WHERE e.is_deleted = 0), 1, NOW()
FROM barcode AS b WHERE b.barcode_value LIKE 'BC-DEMO-%';
INSERT INTO barcode_print_record (barcode_id, template_id, template_version, preview_content, print_by, print_count, print_time)
SELECT b.id, ar.template_id, 'DEMO', JSON_OBJECT('barcodeValue', b.barcode_value), 1, 1, NOW()
FROM barcode AS b JOIN barcode_apply_rule AS ar ON ar.id = b.apply_rule_id WHERE b.barcode_value LIKE 'BC-DEMO-%';

-- ------------------------------ 安灯域 --------------------------------------
DELETE FROM andon_notification_record WHERE event_id IN (SELECT id FROM andon_event WHERE event_no LIKE 'AE-DEMO-%');
DELETE FROM andon_process_log WHERE event_id IN (SELECT id FROM andon_event WHERE event_no LIKE 'AE-DEMO-%');
DELETE FROM andon_event WHERE event_no LIKE 'AE-DEMO-%';
DELETE FROM andon_configuration WHERE remark = '跨模块演示数据';
DELETE FROM andon_reason WHERE reason_code LIKE 'AR-DEMO-%';
DELETE FROM andon_type WHERE type_code LIKE 'AT-DEMO-%';
INSERT INTO andon_type (type_code, type_name, exception_category, handling_mode, response_minutes, responsible_role_code, notification_channels, light_control_enabled, enabled_status, remark, create_by)
SELECT CONCAT('AT-DEMO-', LPAD(n, 4, '0')), CONCAT('演示安灯类型', n), 'QUALITY', 'MANUAL', 30, 'ADMIN', 'IN_APP', 0, 1, '跨模块演示数据', 1 FROM demo_sequence;
INSERT INTO andon_reason (reason_code, reason_name, andon_type_id, reason_description, enabled_status, create_by)
SELECT CONCAT('AR-DEMO-', LPAD(s.n, 4, '0')), CONCAT('演示安灯原因', s.n), t.id, '跨模块演示数据', 1, 1
FROM demo_sequence AS s JOIN andon_type AS t ON t.type_code = CONCAT('AT-DEMO-', LPAD(s.n, 4, '0'));
INSERT INTO andon_configuration (andon_type_id, production_line_id, scope_line_id, handler_user_id, response_minutes, escalation_minutes, notification_channels, enabled_status, remark, create_by)
SELECT t.id, m.line_id, m.line_id, 1, 30, 60, 'IN_APP', 1, '跨模块演示数据', 1
FROM demo_sequence AS s JOIN andon_type AS t ON t.type_code = CONCAT('AT-DEMO-', LPAD(s.n, 4, '0')) JOIN demo_task_map AS m ON m.n = MOD(s.n - 1, 24) + 1;
INSERT INTO andon_event (event_no, andon_type_id, reason_id, source_channel, severity, workshop_id, production_line_id, work_order_id, production_task_id, process_id, equipment_id, batch_no, description, event_status, timeout_status, light_status, initiated_by)
SELECT CONCAT('AE-DEMO-', LPAD(s.n, 4, '0')), ty.id, re.id, 'SYSTEM', IF(MOD(s.n, 10)=0, 'CRITICAL', 'NORMAL'),
       t.workshop_id, t.line_id, t.work_order_id, t.id, (SELECT MIN(d.process_id) FROM prod_process_dispatch_detail AS d WHERE d.task_id=t.id),
       (SELECT MIN(e.id) FROM equip_ledger AS e WHERE e.is_deleted=0), t.batch_no, '跨模块演示安灯', IF(MOD(s.n, 3)=0, 'PROCESSING', 'CLOSED'), 'NORMAL', 'NOT_REQUIRED', 1
FROM demo_sequence AS s JOIN andon_type AS ty ON ty.type_code=CONCAT('AT-DEMO-', LPAD(s.n,4,'0')) JOIN andon_reason AS re ON re.reason_code=CONCAT('AR-DEMO-', LPAD(s.n,4,'0')) JOIN demo_task_map AS m ON m.n=MOD(s.n-1,24)+1 JOIN prod_task AS t ON t.id=m.id;
INSERT INTO andon_process_log (event_id, action_type, from_status, to_status, operator_id, action_content)
SELECT e.id, 'CREATE', NULL, e.event_status, 1, '跨模块演示安灯日志' FROM andon_event AS e WHERE e.event_no LIKE 'AE-DEMO-%';
INSERT INTO andon_notification_record (event_id, notification_type, channel, receiver_user_id, send_status, send_message, sent_at)
SELECT e.id, 'ALERT', 'IN_APP', 1, 'SENT', '跨模块演示安灯通知', NOW() FROM andon_event AS e WHERE e.event_no LIKE 'AE-DEMO-%';

-- ------------------------------ 工资域 --------------------------------------
DELETE FROM wage_settlement_audit_log WHERE settlement_id IN (SELECT id FROM wage_settlement WHERE settlement_no LIKE 'WS-DEMO-%');
DELETE FROM wage_settlement_detail WHERE settlement_id IN (SELECT id FROM wage_settlement WHERE settlement_no LIKE 'WS-DEMO-%');
DELETE FROM wage_settlement WHERE settlement_no LIKE 'WS-DEMO-%';
DELETE FROM wage_rule_change_log WHERE rule_id IN (SELECT id FROM wage_piece_rate_rule WHERE version BETWEEN 100001 AND 102000);
DELETE FROM wage_work_record WHERE source_report_id IN (SELECT id FROM prod_report WHERE report_no LIKE 'RPT-DEMO-%');
DELETE FROM wage_piece_rate_rule WHERE version BETWEEN 100001 AND 102000;
DROP TEMPORARY TABLE IF EXISTS demo_report_map;
CREATE TEMPORARY TABLE demo_report_map AS SELECT ROW_NUMBER() OVER (ORDER BY r.id) AS n, r.id, r.task_id, r.process_id FROM prod_report AS r WHERE r.report_no LIKE 'RPT-DEMO-%' LIMIT 2000;
INSERT INTO wage_piece_rate_rule (process_id, product_id, unit_price_basis, defect_deduction_rate, effective_start, status, version, create_by, update_by)
SELECT rm.process_id, t.product_id, 100, 10, DATE_ADD(DATE('2020-01-01'), INTERVAL rm.n DAY), 1, 100000 + rm.n, 1, 1
FROM demo_report_map AS rm JOIN prod_task AS t ON t.id=rm.task_id;
INSERT INTO wage_rule_change_log (rule_id, change_type, before_snapshot, after_snapshot, change_reason, operate_by)
SELECT r.id, 'CREATE', NULL, JSON_OBJECT('unitPriceBasis', 100), '跨模块演示数据', 1 FROM wage_piece_rate_rule AS r WHERE r.version BETWEEN 100001 AND 102000;
INSERT INTO wage_work_record (source_report_id, employee_id, work_date, work_order_id, process_id, product_id, qualified_quantity, defect_quantity, source_audit_time, create_by)
SELECT rm.id, 1, DATE(pr.report_time), t.work_order_id, rm.process_id, t.product_id, pr.good_quantity, pr.defect_quantity, pr.report_time, 1
FROM demo_report_map AS rm JOIN prod_report AS pr ON pr.id=rm.id JOIN prod_task AS t ON t.id=rm.task_id;
INSERT INTO wage_settlement (settlement_no, period_start, period_end, employee_scope, settlement_status, total_qualified_quantity, total_defect_quantity, total_amount_basis, version, create_by, update_by)
SELECT CONCAT('WS-DEMO-', LPAD(n,4,'0')), DATE('2026-07-05'), DATE('2026-07-16'), JSON_ARRAY(1), 1, 5, 1, 500, 1, 1, 1 FROM demo_sequence;
INSERT INTO wage_settlement_detail (settlement_id, work_record_id, rule_id, employee_id, work_date, work_order_id, process_id, product_id, qualified_quantity, defect_quantity, unit_price_basis, defect_deduction_rate, calculated_amount_basis, final_amount_basis, is_active)
SELECT s.id, w.id, r.id, 1, w.work_date, w.work_order_id, w.process_id, w.product_id, w.qualified_quantity, w.defect_quantity, 100, 10, 500, 500, 1
FROM wage_settlement AS s JOIN wage_work_record AS w ON w.source_report_id = (SELECT rm.id FROM demo_report_map AS rm WHERE rm.n = CAST(RIGHT(s.settlement_no,4) AS UNSIGNED)) JOIN wage_piece_rate_rule AS r ON r.version = 100000 + CAST(RIGHT(s.settlement_no,4) AS UNSIGNED) WHERE s.settlement_no LIKE 'WS-DEMO-%';
INSERT INTO wage_settlement_audit_log (settlement_id, detail_id, action_type, from_status, to_status, before_amount_basis, after_amount_basis, action_reason, operate_by)
SELECT d.settlement_id, d.id, 'SUBMIT', 0, 1, d.calculated_amount_basis, d.final_amount_basis, '跨模块演示数据', 1 FROM wage_settlement_detail AS d;

DROP TEMPORARY TABLE IF EXISTS demo_report_map;
DROP TEMPORARY TABLE IF EXISTS demo_task_map;
DROP TEMPORARY TABLE IF EXISTS demo_sequence;
