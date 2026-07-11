-- =============================================
-- 设备报修任务表 - 测试数据
-- 作者: 角色C
-- 日期: 2026/07/10
-- =============================================

SET @equipment_forming = (SELECT id FROM equip_ledger WHERE equipment_code = 'EQP-002' AND is_deleted = false);
SET @equipment_painting = (SELECT id FROM equip_ledger WHERE equipment_code = 'EQP-004' AND is_deleted = false);
SET @equipment_weight = (SELECT id FROM equip_ledger WHERE equipment_code = 'EQP-005' AND is_deleted = false);

SET @fault_sensor = (SELECT id FROM equip_fault_principle WHERE fault_code = 'FLT-002' AND is_deleted = false);
SET @fault_temperature = (SELECT id FROM equip_fault_principle WHERE fault_code = 'FLT-006' AND is_deleted = false);
SET @fault_weight = (SELECT id FROM equip_fault_principle WHERE fault_code = 'FLT-007' AND is_deleted = false);

INSERT INTO equip_repair_order (
    repair_no,
    equipment_id,
    fault_principle_id,
    fault_description,
    report_time,
    report_user_id,
    repair_user_id,
    repair_start_time,
    repair_end_time,
    repair_result,
    repair_status,
    remark,
    create_by
) VALUES
('REP-20260710-001', @equipment_forming, @fault_sensor, '成型机进料检测传感器偶发无信号，影响自动节拍。', '2026-07-10 08:30:00', 1, 1, '2026-07-10 09:00:00', NULL, NULL, 'REPAIRING', '测试报修单：维修中', 1),
('REP-20260710-002', @equipment_painting, @fault_temperature, '喷涂烘干一体机温控波动过大，烘干温度不稳定。', '2026-07-10 10:15:00', 1, 1, '2026-07-10 10:40:00', '2026-07-10 11:30:00', '更换热电偶并重新校准温控参数，设备恢复正常。', 'FINISHED', '测试报修单：已完成', 1),
('REP-20260710-003', @equipment_weight, @fault_weight, '成品重量检测仪连续三次校验偏差超过允许范围。', '2026-07-10 13:20:00', 1, NULL, NULL, NULL, NULL, 'REPORTED', '测试报修单：待派工', 1);
