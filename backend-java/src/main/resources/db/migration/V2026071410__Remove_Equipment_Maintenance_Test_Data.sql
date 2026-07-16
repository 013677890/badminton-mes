-- 正式迁移不保留保养模块测试数据；开发环境由 db/dev-migration 重新加载。
DELETE FROM equip_maintenance_record
WHERE record_no IN ('MNT-REC-202607-001', 'MNT-REC-202607-002');

DELETE FROM equip_maintenance_plan
WHERE plan_code IN ('MNT-PLAN-001', 'MNT-PLAN-002', 'MNT-PLAN-003');
