-- =============================================
-- 设备故障原理表 - 测试数据
-- 作者: 角色C
-- 日期: 2026/07/10
-- =============================================

SET @category_weaving = (SELECT id FROM equip_category WHERE category_code = 'EQ_CAT_001_01' AND is_deleted = false);
SET @category_forming = (SELECT id FROM equip_category WHERE category_code = 'EQ_CAT_001_02' AND is_deleted = false);
SET @category_painting = (SELECT id FROM equip_category WHERE category_code = 'EQ_CAT_001_04' AND is_deleted = false);
SET @category_weight = (SELECT id FROM equip_category WHERE category_code = 'EQ_CAT_002_02' AND is_deleted = false);

INSERT INTO equip_fault_principle (
    fault_code,
    fault_name,
    category_id,
    fault_level,
    fault_description,
    suggested_solution,
    sort_order,
    remark,
    status,
    create_by
) VALUES
('FLT-001', '电机过热', NULL, 'HIGH', '设备运行时电机温度超过安全范围，可能导致停机或损坏。', '检查散热风扇、润滑状态和负载情况，必要时停机降温。', 1, '通用电气类故障', 1, 1),
('FLT-002', '传感器异常', NULL, 'MEDIUM', '检测传感器数据波动、无信号或信号超出合理范围。', '检查传感器接线、安装位置和校准状态。', 2, '通用检测类故障', 1, 1),
('FLT-003', '皮带松动', @category_weaving, 'LOW', '传动皮带松弛，导致传动效率下降或运行异响。', '调整皮带张紧度，检查皮带磨损情况并按需更换。', 3, '编织设备常见故障', 1, 1),
('FLT-004', '气压不足', @category_forming, 'MEDIUM', '气动设备工作压力低于工艺要求，影响成型动作稳定性。', '检查气源压力、过滤减压阀、气管泄漏和执行元件状态。', 4, '成型设备常见故障', 1, 1),
('FLT-005', '主轴卡滞', @category_weaving, 'CRITICAL', '主轴无法正常转动或转动阻力异常，存在设备损坏风险。', '立即停机，检查轴承、润滑、异物卡阻和主轴电机状态。', 5, '严重机械故障', 1, 1),
('FLT-006', '温控失效', @category_painting, 'HIGH', '加热或烘干温控模块异常，导致温度无法稳定控制。', '检查温控器、热电偶、加热管和控制线路。', 6, '涂装烘干设备常见故障', 1, 1),
('FLT-007', '称重漂移', @category_weight, 'MEDIUM', '重量检测读数持续偏移或重复测量差异过大。', '清洁称台，执行校准流程，检查传感器和环境振动。', 7, '质量检测设备常见故障', 1, 1);
