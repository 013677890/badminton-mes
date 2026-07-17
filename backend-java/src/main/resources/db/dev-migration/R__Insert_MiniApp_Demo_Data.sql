-- 微信小程序真实 API 演示数据。
-- 仅由 dev 环境加载；使用固定业务编码和幂等更新，避免重复执行产生重复数据。

SET @admin_id = (SELECT id FROM sys_user WHERE user_no = 'admin' LIMIT 1);
SET @unit_id = (SELECT id FROM base_unit WHERE unit_code = 'PCS' LIMIT 1);
SET @workshop_id = (SELECT id FROM base_workshop WHERE workshop_code = 'WS001' LIMIT 1);
SET @line_1_id = (SELECT id FROM base_production_line WHERE line_code = 'LINE-01' LIMIT 1);
SET @line_2_id = (SELECT id FROM base_production_line WHERE line_code = 'LINE-02' LIMIT 1);
SET @product_1_id = (SELECT id FROM base_product WHERE product_code = 'P001' LIMIT 1);
SET @product_2_id = (SELECT id FROM base_product WHERE product_code = 'P002' LIMIT 1);
SET @product_3_id = (SELECT id FROM base_product WHERE product_code = 'P003' LIMIT 1);
SET @process_1_id = (SELECT id FROM craft_process WHERE process_code = 'PR001' LIMIT 1);
SET @process_2_id = (SELECT id FROM craft_process WHERE process_code = 'PR002' LIMIT 1);
SET @process_3_id = (SELECT id FROM craft_process WHERE process_code = 'PR003' LIMIT 1);

INSERT INTO craft_routing (
    routing_code, routing_name, version, source_type, routing_status,
    audit_by, audit_time, create_by, is_deleted
) VALUES (
    'DEMO-ROUTE-01', '比赛级羽毛球标准工艺路线', 'V1', 1, 1,
    @admin_id, NOW(), @admin_id, 0
)
ON DUPLICATE KEY UPDATE
    routing_name = VALUES(routing_name),
    routing_status = 1,
    audit_by = VALUES(audit_by),
    audit_time = VALUES(audit_time),
    is_deleted = 0;

SET @routing_id = (
    SELECT id FROM craft_routing
    WHERE routing_code = 'DEMO-ROUTE-01' AND version = 'V1'
    ORDER BY id DESC LIMIT 1
);

INSERT INTO prod_work_order (
    work_order_no, source_type, source_system, source_order_no,
    product_id, product_name, spec, unit_id, batch_no, routing_id,
    workshop_id, plan_quantity, dispatched_quantity, input_quantity,
    finish_quantity, defect_quantity, rework_quantity, over_ratio,
    priority, plan_start_time, plan_end_time, order_status, kit_status,
    create_by, is_deleted
) VALUES
(
    'DEMO-WO-001', 1, 'MINIAPP_DEMO', 'DEMO-SOURCE-001',
    @product_1_id, '比赛级鹅毛羽毛球', '77速 12只装', @unit_id,
    'DEMO-BATCH-20260717-A', @routing_id, @workshop_id,
    1200, 1200, 760, 745, 25, 10, 2.00, 9,
    DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_ADD(NOW(), INTERVAL 4 HOUR),
    3, 2, @admin_id, 0
),
(
    'DEMO-WO-002', 1, 'MINIAPP_DEMO', 'DEMO-SOURCE-002',
    @product_2_id, '训练级鸭毛羽毛球', '76速 12只装', @unit_id,
    'DEMO-BATCH-20260717-B', @routing_id, @workshop_id,
    900, 900, 460, 430, 18, 12, 2.00, 7,
    DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_ADD(NOW(), INTERVAL 5 HOUR),
    3, 2, @admin_id, 0
),
(
    'DEMO-WO-003', 1, 'MINIAPP_DEMO', 'DEMO-SOURCE-003',
    @product_3_id, '耐打型尼龙羽毛球', '中速 6只装', @unit_id,
    'DEMO-BATCH-20260717-C', @routing_id, @workshop_id,
    600, 600, 300, 286, 8, 6, 2.00, 6,
    DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 6 HOUR),
    3, 2, @admin_id, 0
)
ON DUPLICATE KEY UPDATE
    product_id = VALUES(product_id),
    product_name = VALUES(product_name),
    spec = VALUES(spec),
    routing_id = VALUES(routing_id),
    workshop_id = VALUES(workshop_id),
    plan_quantity = VALUES(plan_quantity),
    dispatched_quantity = VALUES(dispatched_quantity),
    input_quantity = VALUES(input_quantity),
    finish_quantity = VALUES(finish_quantity),
    defect_quantity = VALUES(defect_quantity),
    rework_quantity = VALUES(rework_quantity),
    plan_start_time = VALUES(plan_start_time),
    plan_end_time = VALUES(plan_end_time),
    order_status = VALUES(order_status),
    kit_status = VALUES(kit_status),
    is_deleted = 0;

SET @wo_1_id = (SELECT id FROM prod_work_order WHERE work_order_no = 'DEMO-WO-001');
SET @wo_2_id = (SELECT id FROM prod_work_order WHERE work_order_no = 'DEMO-WO-002');
SET @wo_3_id = (SELECT id FROM prod_work_order WHERE work_order_no = 'DEMO-WO-003');

INSERT INTO prod_task (
    task_no, source_type, work_order_id, work_order_no, product_id,
    product_code, product_name, batch_no, routing_id, routing_code,
    routing_version, workshop_id, workshop_name, line_id, line_name,
    plan_date, plan_quantity, input_quantity, good_quantity,
    defect_quantity, rework_quantity, finish_quantity,
    plan_start_time, plan_end_time, actual_start_time, task_status,
    create_by, is_deleted
) VALUES
(
    'DEMO-TASK-001', 2, @wo_1_id, 'DEMO-WO-001', @product_1_id,
    'P001', '比赛级鹅毛羽毛球', 'DEMO-BATCH-20260717-A',
    @routing_id, 'DEMO-ROUTE-01', 'V1', @workshop_id,
    '羽毛球一车间', @line_1_id, '一号成型线', CURDATE(),
    1200, 760, 720, 25, 15, 745,
    DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_ADD(NOW(), INTERVAL 4 HOUR),
    DATE_SUB(NOW(), INTERVAL 3 HOUR), 3, @admin_id, 0
),
(
    'DEMO-TASK-002', 2, @wo_2_id, 'DEMO-WO-002', @product_2_id,
    'P002', '训练级鸭毛羽毛球', 'DEMO-BATCH-20260717-B',
    @routing_id, 'DEMO-ROUTE-01', 'V1', @workshop_id,
    '羽毛球一车间', @line_2_id, '二号成型线', CURDATE(),
    900, 460, 412, 18, 18, 430,
    DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_ADD(NOW(), INTERVAL 5 HOUR),
    DATE_SUB(NOW(), INTERVAL 2 HOUR), 3, @admin_id, 0
),
(
    'DEMO-TASK-003', 2, @wo_3_id, 'DEMO-WO-003', @product_3_id,
    'P003', '耐打型尼龙羽毛球', 'DEMO-BATCH-20260717-C',
    @routing_id, 'DEMO-ROUTE-01', 'V1', @workshop_id,
    '羽毛球一车间', @line_1_id, '一号成型线', CURDATE(),
    600, 300, 278, 8, 8, 286,
    DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 6 HOUR),
    DATE_SUB(NOW(), INTERVAL 90 MINUTE), 2, @admin_id, 0
)
ON DUPLICATE KEY UPDATE
    work_order_id = VALUES(work_order_id),
    product_id = VALUES(product_id),
    product_name = VALUES(product_name),
    routing_id = VALUES(routing_id),
    workshop_id = VALUES(workshop_id),
    workshop_name = VALUES(workshop_name),
    line_id = VALUES(line_id),
    line_name = VALUES(line_name),
    plan_date = VALUES(plan_date),
    plan_quantity = VALUES(plan_quantity),
    input_quantity = VALUES(input_quantity),
    good_quantity = VALUES(good_quantity),
    defect_quantity = VALUES(defect_quantity),
    rework_quantity = VALUES(rework_quantity),
    finish_quantity = VALUES(finish_quantity),
    plan_start_time = VALUES(plan_start_time),
    plan_end_time = VALUES(plan_end_time),
    actual_start_time = VALUES(actual_start_time),
    task_status = VALUES(task_status),
    is_deleted = 0;

SET @task_1_id = (SELECT id FROM prod_task WHERE task_no = 'DEMO-TASK-001');
SET @task_2_id = (SELECT id FROM prod_task WHERE task_no = 'DEMO-TASK-002');
SET @task_3_id = (SELECT id FROM prod_task WHERE task_no = 'DEMO-TASK-003');

INSERT INTO prod_process_dispatch (
    dispatch_no, task_id, routing_id, routing_code, routing_version,
    dispatch_status, create_by, is_deleted
) VALUES
('DEMO-DISPATCH-001', @task_1_id, @routing_id, 'DEMO-ROUTE-01', 'V1', 2, @admin_id, 0),
('DEMO-DISPATCH-002', @task_2_id, @routing_id, 'DEMO-ROUTE-01', 'V1', 2, @admin_id, 0),
('DEMO-DISPATCH-003', @task_3_id, @routing_id, 'DEMO-ROUTE-01', 'V1', 2, @admin_id, 0)
ON DUPLICATE KEY UPDATE
    task_id = VALUES(task_id),
    routing_id = VALUES(routing_id),
    dispatch_status = VALUES(dispatch_status),
    is_deleted = 0;

SET @dispatch_1_id = (SELECT id FROM prod_process_dispatch WHERE dispatch_no = 'DEMO-DISPATCH-001');
SET @dispatch_2_id = (SELECT id FROM prod_process_dispatch WHERE dispatch_no = 'DEMO-DISPATCH-002');
SET @dispatch_3_id = (SELECT id FROM prod_process_dispatch WHERE dispatch_no = 'DEMO-DISPATCH-003');

INSERT INTO prod_process_dispatch_detail (
    dispatch_id, task_id, process_id, process_code, process_name, seq,
    is_key, is_inspect, is_scan, user_id, plan_quantity, good_quantity,
    defect_quantity, detail_status, actual_start_time, is_deleted
) VALUES
(@dispatch_1_id, @task_1_id, @process_1_id, 'PR001', '毛片分选', 1, 1, 1, 1, @admin_id, 1200, 720, 25, 2, DATE_SUB(NOW(), INTERVAL 3 HOUR), 0),
(@dispatch_2_id, @task_2_id, @process_2_id, 'PR002', '插毛成型', 1, 1, 1, 1, @admin_id, 900, 412, 18, 2, DATE_SUB(NOW(), INTERVAL 2 HOUR), 0),
(@dispatch_3_id, @task_3_id, @process_3_id, 'PR003', '称重质检', 1, 1, 1, 1, @admin_id, 600, 278, 8, 1, DATE_SUB(NOW(), INTERVAL 80 MINUTE), 0)
ON DUPLICATE KEY UPDATE
    process_id = VALUES(process_id),
    process_code = VALUES(process_code),
    process_name = VALUES(process_name),
    user_id = VALUES(user_id),
    plan_quantity = VALUES(plan_quantity),
    good_quantity = VALUES(good_quantity),
    defect_quantity = VALUES(defect_quantity),
    detail_status = VALUES(detail_status),
    actual_start_time = VALUES(actual_start_time),
    is_deleted = 0;

SET @detail_1_id = (
    SELECT id FROM prod_process_dispatch_detail
    WHERE task_id = @task_1_id AND seq = 1 ORDER BY id DESC LIMIT 1
);
SET @detail_2_id = (
    SELECT id FROM prod_process_dispatch_detail
    WHERE task_id = @task_2_id AND seq = 1 ORDER BY id DESC LIMIT 1
);
SET @detail_3_id = (
    SELECT id FROM prod_process_dispatch_detail
    WHERE task_id = @task_3_id AND seq = 1 ORDER BY id DESC LIMIT 1
);

INSERT INTO prod_report (
    report_no, request_no, task_id, dispatch_detail_id, process_id,
    batch_no, report_type, record_type, user_id,
    input_quantity, good_quantity, defect_quantity, rework_quantity,
    source_type, report_time, is_deleted
) VALUES
('DEMO-RPT-001-A', 'DEMO-REQ-001-A', @task_1_id, @detail_1_id, @process_1_id, 'DEMO-BATCH-20260717-A', 1, 1, @admin_id, 260, 244, 10, 6, 1, DATE_SUB(NOW(), INTERVAL 150 MINUTE), 0),
('DEMO-RPT-001-B', 'DEMO-REQ-001-B', @task_1_id, @detail_1_id, @process_1_id, 'DEMO-BATCH-20260717-A', 1, 1, @admin_id, 280, 268, 8, 4, 1, DATE_SUB(NOW(), INTERVAL 90 MINUTE), 0),
('DEMO-RPT-001-C', 'DEMO-REQ-001-C', @task_1_id, @detail_1_id, @process_1_id, 'DEMO-BATCH-20260717-A', 1, 1, @admin_id, 220, 208, 7, 5, 1, DATE_SUB(NOW(), INTERVAL 30 MINUTE), 0),
('DEMO-RPT-002-A', 'DEMO-REQ-002-A', @task_2_id, @detail_2_id, @process_2_id, 'DEMO-BATCH-20260717-B', 1, 1, @admin_id, 240, 218, 12, 10, 1, DATE_SUB(NOW(), INTERVAL 120 MINUTE), 0),
('DEMO-RPT-002-B', 'DEMO-REQ-002-B', @task_2_id, @detail_2_id, @process_2_id, 'DEMO-BATCH-20260717-B', 1, 1, @admin_id, 220, 194, 6, 8, 1, DATE_SUB(NOW(), INTERVAL 45 MINUTE), 0),
('DEMO-RPT-003-A', 'DEMO-REQ-003-A', @task_3_id, @detail_3_id, @process_3_id, 'DEMO-BATCH-20260717-C', 1, 1, @admin_id, 300, 278, 8, 8, 1, DATE_SUB(NOW(), INTERVAL 20 MINUTE), 0)
ON DUPLICATE KEY UPDATE
    task_id = VALUES(task_id),
    dispatch_detail_id = VALUES(dispatch_detail_id),
    process_id = VALUES(process_id),
    input_quantity = VALUES(input_quantity),
    good_quantity = VALUES(good_quantity),
    defect_quantity = VALUES(defect_quantity),
    rework_quantity = VALUES(rework_quantity),
    report_time = VALUES(report_time),
    is_deleted = 0;

INSERT INTO prod_batch_status (
    batch_no, task_id, product_id, current_process_id, current_process_name,
    batch_status, is_abnormal, is_deleted
) VALUES
('DEMO-BATCH-20260717-A', @task_1_id, @product_1_id, @process_1_id, '毛片分选', 2, 0, 0),
('DEMO-BATCH-20260717-B', @task_2_id, @product_2_id, @process_2_id, '插毛成型', 2, 1, 0),
('DEMO-BATCH-20260717-C', @task_3_id, @product_3_id, @process_3_id, '称重质检', 1, 0, 0)
ON DUPLICATE KEY UPDATE
    task_id = VALUES(task_id),
    product_id = VALUES(product_id),
    current_process_id = VALUES(current_process_id),
    current_process_name = VALUES(current_process_name),
    batch_status = VALUES(batch_status),
    is_abnormal = VALUES(is_abnormal),
    is_deleted = 0;

SET @batch_status_1_id = (
    SELECT id FROM prod_batch_status WHERE batch_no = 'DEMO-BATCH-20260717-A'
);

INSERT INTO equip_ledger (
    equipment_code, equipment_name, category_id, equipment_model,
    workshop_id, production_line_id, installation_location,
    equipment_status, responsible_person, status, create_by, is_deleted
) VALUES
('DEMO-EQ-001', '智能毛片分选机', 1, 'SORT-A1', @workshop_id, @line_1_id, '一号线前段', 'RUNNING', '系统管理员', 1, @admin_id, 0),
('DEMO-EQ-002', '自动插毛成型机', 1, 'FORM-B2', @workshop_id, @line_2_id, '二号线中段', 'RUNNING', '系统管理员', 1, @admin_id, 0),
('DEMO-EQ-003', '动平衡检测机', 1, 'TEST-C3', @workshop_id, @line_1_id, '一号线质检位', 'STOPPED', '系统管理员', 1, @admin_id, 0),
('DEMO-EQ-004', '自动称重包装机', 1, 'PACK-D4', @workshop_id, @line_2_id, '二号线末段', 'MAINTAINING', '系统管理员', 1, @admin_id, 0)
ON DUPLICATE KEY UPDATE
    equipment_name = VALUES(equipment_name),
    workshop_id = VALUES(workshop_id),
    production_line_id = VALUES(production_line_id),
    equipment_status = VALUES(equipment_status),
    responsible_person = VALUES(responsible_person),
    status = 1,
    is_deleted = 0;

SET @equipment_1_id = (SELECT id FROM equip_ledger WHERE equipment_code = 'DEMO-EQ-001');
SET @equipment_2_id = (SELECT id FROM equip_ledger WHERE equipment_code = 'DEMO-EQ-002');

UPDATE prod_process_dispatch_detail
SET equipment_id = @equipment_1_id
WHERE task_id = @task_1_id AND seq = 1;

UPDATE prod_process_dispatch_detail
SET equipment_id = @equipment_2_id
WHERE task_id = @task_2_id AND seq = 1;

INSERT INTO andon_type (
    type_code, type_name, exception_category, handling_mode,
    response_minutes, responsible_role_code, notification_channels,
    light_control_enabled, enabled_status, remark, create_by, is_deleted
) VALUES (
    'DEMO-PRODUCTION', '生产异常', 'PRODUCTION', 'MANUAL',
    10, 'TEAM_LEADER', 'MINI_APP', 1, 1,
    '小程序看板演示安灯类型', @admin_id, 0
)
ON DUPLICATE KEY UPDATE
    type_name = VALUES(type_name),
    response_minutes = VALUES(response_minutes),
    enabled_status = 1,
    is_deleted = 0;

SET @andon_type_id = (SELECT id FROM andon_type WHERE type_code = 'DEMO-PRODUCTION');

INSERT INTO andon_event (
    event_no, andon_type_id, source_channel, severity, workshop_id,
    production_line_id, work_order_id, production_task_id, process_id,
    equipment_id, batch_no, description, event_status,
    assigned_user_id, assigned_role_code, response_deadline,
    escalation_deadline, timeout_status, light_status, light_message,
    impact_minutes, affected_quantity, initiated_by, is_deleted
) VALUES
(
    'DEMO-ANDON-001', @andon_type_id, 'MINI_APP', 'CRITICAL',
    @workshop_id, @line_2_id, @wo_2_id, @task_2_id, @process_2_id,
    @equipment_2_id, 'DEMO-BATCH-20260717-B',
    '二号线插毛成型工位出现羽片定位偏差，请立即处理。',
    'PROCESSING', @admin_id, 'TEAM_LEADER',
    DATE_ADD(NOW(), INTERVAL 10 MINUTE), DATE_ADD(NOW(), INTERVAL 20 MINUTE),
    'NORMAL', 'ON', '严重异常处理中', 18, 36, @admin_id, 0
),
(
    'DEMO-ANDON-002', @andon_type_id, 'MINI_APP', 'NORMAL',
    @workshop_id, @line_1_id, @wo_1_id, @task_1_id, @process_1_id,
    @equipment_1_id, 'DEMO-BATCH-20260717-A',
    '毛片分选机需要补充原料周转箱。',
    'OPEN', @admin_id, 'TEAM_LEADER',
    DATE_ADD(NOW(), INTERVAL 15 MINUTE), DATE_ADD(NOW(), INTERVAL 30 MINUTE),
    'NORMAL', 'ON', '等待班组长响应', 6, 12, @admin_id, 0
)
ON DUPLICATE KEY UPDATE
    severity = VALUES(severity),
    production_line_id = VALUES(production_line_id),
    work_order_id = VALUES(work_order_id),
    production_task_id = VALUES(production_task_id),
    process_id = VALUES(process_id),
    equipment_id = VALUES(equipment_id),
    description = VALUES(description),
    event_status = VALUES(event_status),
    assigned_user_id = VALUES(assigned_user_id),
    response_deadline = VALUES(response_deadline),
    escalation_deadline = VALUES(escalation_deadline),
    light_status = VALUES(light_status),
    light_message = VALUES(light_message),
    is_deleted = 0;

INSERT INTO prod_work_order_material (
    work_order_id, material_id, require_quantity, issued_quantity, is_deleted
)
SELECT @wo_1_id, id,
       CASE material_code WHEN 'M001' THEN 1200.0000 ELSE 2400.0000 END,
       CASE material_code WHEN 'M001' THEN 900.0000 ELSE 1900.0000 END,
       0
FROM base_material
WHERE material_code IN ('M001', 'M002')
ON DUPLICATE KEY UPDATE
    require_quantity = VALUES(require_quantity),
    issued_quantity = VALUES(issued_quantity),
    is_deleted = 0;

INSERT INTO barcode_type (
    type_code, type_name, apply_object, status, is_deleted
) VALUES (
    'DEMO-BATCH', '演示批次条码', 'PRODUCTION_BATCH', 1, 0
)
ON DUPLICATE KEY UPDATE
    type_name = VALUES(type_name),
    apply_object = VALUES(apply_object),
    status = 1,
    is_deleted = 0;

SET @barcode_type_id = (SELECT id FROM barcode_type WHERE type_code = 'DEMO-BATCH');

INSERT INTO barcode (
    barcode_value, barcode_type_id, barcode_mode, product_id, batch_no,
    work_order_id, task_id, source_type, barcode_status, create_by, is_deleted
) VALUES (
    'BC-DEMO-20260717-A-001', @barcode_type_id, 2, @product_1_id,
    'DEMO-BATCH-20260717-A', @wo_1_id, @task_1_id, 1, 1, @admin_id, 0
)
ON DUPLICATE KEY UPDATE
    barcode_type_id = VALUES(barcode_type_id),
    product_id = VALUES(product_id),
    batch_no = VALUES(batch_no),
    work_order_id = VALUES(work_order_id),
    task_id = VALUES(task_id),
    barcode_status = 1,
    is_deleted = 0;

SET @barcode_id = (
    SELECT id FROM barcode WHERE barcode_value = 'BC-DEMO-20260717-A-001'
);

DELETE FROM barcode_use_record
WHERE barcode_id = @barcode_id AND task_id = @task_1_id;

INSERT INTO barcode_use_record (
    barcode_id, task_id, process_id, user_id, equipment_id,
    use_type, business_time, is_deleted
) VALUES
(@barcode_id, @task_1_id, @process_1_id, @admin_id, @equipment_1_id, 1, DATE_SUB(NOW(), INTERVAL 170 MINUTE), 0),
(@barcode_id, @task_1_id, @process_2_id, @admin_id, @equipment_2_id, 2, DATE_SUB(NOW(), INTERVAL 80 MINUTE), 0)
ON DUPLICATE KEY UPDATE
    equipment_id = VALUES(equipment_id),
    business_time = VALUES(business_time),
    is_deleted = 0;

DELETE FROM prod_batch_process_history
WHERE task_id = @task_1_id AND batch_no = 'DEMO-BATCH-20260717-A';

INSERT INTO prod_batch_process_history (
    batch_status_id, task_id, dispatch_detail_id, batch_no, process_id,
    process_code, process_name, action_type, operator_id,
    action_reason, operate_time, is_deleted
) VALUES
(@batch_status_1_id, @task_1_id, @detail_1_id, 'DEMO-BATCH-20260717-A', @process_1_id, 'PR001', '毛片分选', 1, @admin_id, '批次投入生产', DATE_SUB(NOW(), INTERVAL 180 MINUTE), 0),
(@batch_status_1_id, @task_1_id, @detail_1_id, 'DEMO-BATCH-20260717-A', @process_2_id, 'PR002', '插毛成型', 2, @admin_id, '完成首道工序并转序', DATE_SUB(NOW(), INTERVAL 100 MINUTE), 0),
(@batch_status_1_id, @task_1_id, @detail_1_id, 'DEMO-BATCH-20260717-A', @process_3_id, 'PR003', '称重质检', 3, @admin_id, '抽检通过，继续生产', DATE_SUB(NOW(), INTERVAL 40 MINUTE), 0)
ON DUPLICATE KEY UPDATE
    action_reason = VALUES(action_reason),
    operate_time = VALUES(operate_time),
    is_deleted = 0;
