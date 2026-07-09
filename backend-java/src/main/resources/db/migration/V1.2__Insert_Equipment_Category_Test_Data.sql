-- =============================================
-- 设备类别表 - 测试数据
-- 作者: 角色C
-- 日期: 2026/07/09
-- =============================================

-- 插入顶级设备类别
INSERT INTO equip_category (category_code, category_name, parent_id, sort_order, remark, status, create_by) VALUES
('EQ_CAT_001', '生产设备', NULL, 1, '用于生产制造的设备', 1, 1),
('EQ_CAT_002', '检测设备', NULL, 2, '用于质量检测的设备', 1, 1),
('EQ_CAT_003', '辅助设备', NULL, 3, '生产辅助设备', 1, 1);

-- 插入生产设备子类别（使用变量存储父级ID）
SET @parent_id_001 = (SELECT id FROM equip_category WHERE category_code = 'EQ_CAT_001' AND is_deleted = false);
INSERT INTO equip_category (category_code, category_name, parent_id, sort_order, remark, status, create_by) VALUES
('EQ_CAT_001_01', '编织设备', @parent_id_001, 1, '羽毛球拍编织机', 1, 1),
('EQ_CAT_001_02', '成型设备', @parent_id_001, 2, '羽毛球拍成型机', 1, 1),
('EQ_CAT_001_03', '装配设备', @parent_id_001, 3, '装配线设备', 1, 1),
('EQ_CAT_001_04', '涂装设备', @parent_id_001, 4, '喷涂、烘干设备', 1, 1);

-- 插入检测设备子类别
SET @parent_id_002 = (SELECT id FROM equip_category WHERE category_code = 'EQ_CAT_002' AND is_deleted = false);
INSERT INTO equip_category (category_code, category_name, parent_id, sort_order, remark, status, create_by) VALUES
('EQ_CAT_002_01', '硬度检测', @parent_id_002, 1, '硬度检测仪', 1, 1),
('EQ_CAT_002_02', '重量检测', @parent_id_002, 2, '电子秤', 1, 1),
('EQ_CAT_002_03', '尺寸检测', @parent_id_002, 3, '卡尺、测距仪', 1, 1);

-- 插入辅助设备子类别
SET @parent_id_003 = (SELECT id FROM equip_category WHERE category_code = 'EQ_CAT_003' AND is_deleted = false);
INSERT INTO equip_category (category_code, category_name, parent_id, sort_order, remark, status, create_by) VALUES
('EQ_CAT_003_01', '搬运设备', @parent_id_003, 1, '叉车、AGV', 1, 1),
('EQ_CAT_003_02', '包装设备', @parent_id_003, 2, '封箱机、打包机', 1, 1);
