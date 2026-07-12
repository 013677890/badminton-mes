INSERT INTO equip_maintenance_plan (
    plan_code, plan_name, equipment_id, maintenance_type, cycle_days,
    maintenance_content, responsible_user_id, last_maintenance_time,
    next_maintenance_time, remark, status, create_by
)
SELECT
    'MNT-PLAN-001', '编织机月度例行保养', equipment.id, 'ROUTINE', 30,
    '清洁设备、检查润滑状态、紧固传动部件并检查安全防护装置。', 1,
    '2026-06-15 09:00:00', '2026-07-15 09:00:00', '设备保养计划开发数据', 1, 1
FROM equip_ledger equipment
WHERE equipment.equipment_code = 'EQP-001' AND equipment.is_deleted = false
  AND NOT EXISTS (SELECT 1 FROM equip_maintenance_plan existing WHERE existing.plan_code = 'MNT-PLAN-001');

INSERT INTO equip_maintenance_plan (
    plan_code, plan_name, equipment_id, maintenance_type, cycle_days,
    maintenance_content, responsible_user_id, last_maintenance_time,
    next_maintenance_time, remark, status, create_by
)
SELECT
    'MNT-PLAN-002', '成型机预防性保养', equipment.id, 'PREVENTIVE', 60,
    '检查气动系统、传感器、成型模具和电气控制柜。', 1,
    NULL, '2026-07-20 10:00:00', '设备保养计划开发数据', 1, 1
FROM equip_ledger equipment
WHERE equipment.equipment_code = 'EQP-002' AND equipment.is_deleted = false
  AND NOT EXISTS (SELECT 1 FROM equip_maintenance_plan existing WHERE existing.plan_code = 'MNT-PLAN-002');

INSERT INTO equip_maintenance_record (
    record_no, plan_id, equipment_id, scheduled_time, start_time, finish_time,
    executor_user_id, maintenance_content, maintenance_result, record_status,
    abnormal_description, remark, create_by
)
SELECT
    'MNT-REC-202607-001', plan.id, plan.equipment_id, '2026-06-15 09:00:00',
    '2026-06-15 09:05:00', '2026-06-15 10:10:00', 1,
    plan.maintenance_content, 'NORMAL', 'COMPLETED', NULL, '已完成保养记录开发数据', 1
FROM equip_maintenance_plan plan
WHERE plan.plan_code = 'MNT-PLAN-001' AND plan.is_deleted = false
  AND NOT EXISTS (SELECT 1 FROM equip_maintenance_record existing WHERE existing.record_no = 'MNT-REC-202607-001');
