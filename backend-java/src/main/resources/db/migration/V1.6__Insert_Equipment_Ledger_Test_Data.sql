-- =============================================
-- 设备台账表 - 测试数据
-- 作者: 角色C
-- 日期: 2026/07/09
-- =============================================

SET @category_weaving = (SELECT id FROM equip_category WHERE category_code = 'EQ_CAT_001_01' AND is_deleted = false);
SET @category_forming = (SELECT id FROM equip_category WHERE category_code = 'EQ_CAT_001_02' AND is_deleted = false);
SET @category_assembly = (SELECT id FROM equip_category WHERE category_code = 'EQ_CAT_001_03' AND is_deleted = false);
SET @category_painting = (SELECT id FROM equip_category WHERE category_code = 'EQ_CAT_001_04' AND is_deleted = false);
SET @category_weight = (SELECT id FROM equip_category WHERE category_code = 'EQ_CAT_002_02' AND is_deleted = false);

SET @manufacturer_yonex = (SELECT id FROM equip_manufacturer WHERE manufacturer_code = 'MFR_001' AND is_deleted = false);
SET @manufacturer_victor = (SELECT id FROM equip_manufacturer WHERE manufacturer_code = 'MFR_002' AND is_deleted = false);
SET @manufacturer_lining = (SELECT id FROM equip_manufacturer WHERE manufacturer_code = 'MFR_003' AND is_deleted = false);
SET @manufacturer_kason = (SELECT id FROM equip_manufacturer WHERE manufacturer_code = 'MFR_004' AND is_deleted = false);
SET @manufacturer_rsl = (SELECT id FROM equip_manufacturer WHERE manufacturer_code = 'MFR_005' AND is_deleted = false);

INSERT INTO equip_ledger (
    equipment_code,
    equipment_name,
    category_id,
    manufacturer_id,
    equipment_model,
    serial_number,
    workshop_id,
    production_line_id,
    installation_location,
    purchase_date,
    commissioning_date,
    equipment_status,
    responsible_person,
    remark,
    status,
    create_by
) VALUES
('EQP-001', '一号编织机', @category_weaving, @manufacturer_yonex, 'YW-3000', 'SN-YW-202601001', 1, 1, '一号成型车间-A区-01工位', '2025-12-01', '2026-01-05', 'RUNNING', '张师傅', '羽毛球拍线网编织主设备', 1, 1),
('EQP-002', '二号成型机', @category_forming, @manufacturer_victor, 'VF-2200', 'SN-VF-202601002', 1, 1, '一号成型车间-B区-02工位', '2025-12-10', '2026-01-08', 'IDLE', '李师傅', '球头热压成型设备', 1, 1),
('EQP-003', '一号装配线', @category_assembly, @manufacturer_lining, 'LA-1000', 'SN-LA-202601003', 1, 2, '一号成型车间-C区-装配线', '2025-11-20', '2026-01-03', 'RUNNING', '王师傅', '羽毛球成品装配线', 1, 1),
('EQP-004', '喷涂烘干一体机', @category_painting, @manufacturer_kason, 'KP-880', 'SN-KP-202601004', 1, 2, '一号成型车间-D区-涂装间', '2025-10-15', '2026-01-10', 'STOPPED', '赵师傅', '表面喷涂与低温烘干一体设备', 1, 1),
('EQP-005', '成品重量检测仪', @category_weight, @manufacturer_rsl, 'RW-520', 'SN-RW-202601005', 2, 3, '二号包装车间-质检区', '2025-12-18', '2026-01-12', 'IDLE', '刘质检', '成品入库前重量检测设备', 1, 1);
