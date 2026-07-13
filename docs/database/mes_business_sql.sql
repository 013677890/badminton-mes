-- =====================================================================
-- 羽毛球 MES 核心业务 SQL(配合 mes_schema.sql 使用)
-- MySQL 8.0 / InnoDB / 隔离级别 REPEATABLE READ
--
-- 通用约定:
-- 1. 幂等与防重一律由唯一索引兜底(INDEX-001),应用层校验只是第一道防线。
-- 2. 悲观锁(SELECT ... FOR UPDATE)只用于短事务的关键资源:流水号取号、
--    工单未派数量校验、任务数量累加前的状态确认;加锁顺序统一为
--    工单 -> 任务 -> 派工明细,避免交叉死锁。
-- 3. 状态机变更一律用 CAS 式 UPDATE(WHERE 带旧状态),受影响行数=0 即并发冲突,
--    由应用层重试或报错,不用先查后改。
-- 4. 数量一致性(投入>=良品+不良)在 UPDATE 的 WHERE 条件中兜底,
--    保证并发下也不会写出非法数据。
-- 5. 列表查询禁止 SELECT *(ORM-001);分页 count=0 直接返回(SQL-005);
--    深分页用游标(keyset)或延迟关联(INDEX-007)。
-- 6. 所有 UPDATE 显式携带 update_time = NOW()(ORM-007;虽有 ON UPDATE 兜底)。
-- 7. 逻辑删除:UPDATE is_deleted = 1,禁止物理 DELETE(TABLE-010)。
-- =====================================================================

-- #####################################################################
-- 一、生产订单
-- #####################################################################

-- 1.1 ERP/API 工单写入(幂等:uk_source_order 防重复生成)
-- 事务:同步记录 + 工单一起提交,失败一起回滚
START TRANSACTION;

INSERT INTO api_sync_record (sync_type, source_system, source_no, target_table, sync_status, sync_time)
VALUES (1, 'ERP', 'ERP20260706001', 'prod_work_order', 0, NOW());

-- 唯一索引冲突说明该来源单号已生成过工单,应用层捕获 1062 后改写同步记录为"重复跳过"并提交
INSERT INTO prod_work_order
    (work_order_no, source_type, source_order_no, product_id, product_name, spec, unit_id,
     batch_no, bom_id, routing_id, customer_id, workshop_id,
     plan_quantity, priority, plan_start_time, plan_end_time, order_status, create_by)
SELECT 'WO202607060001', 3, 'ERP20260706001', t1.id, t1.product_name, t1.spec, t1.unit_id,
       NULL, NULL, NULL, NULL, 1,
       10000, 5, '2026-07-08 08:00:00', '2026-07-15 18:00:00', 0, 1
FROM base_product AS t1
WHERE t1.product_code = 'P-TRAIN-01' AND t1.status = 1 AND t1.is_deleted = 0;
-- 受影响行数=0 说明产品编码不存在 → 回滚,同步记录写失败原因

UPDATE api_sync_record SET sync_status = 1, target_id = LAST_INSERT_ID(), update_time = NOW()
WHERE sync_type = 1 AND source_system = 'ERP' AND source_no = 'ERP20260706001';

COMMIT;

-- 1.2 工单下达(CAS 状态机:未维护 BOM/工艺路线不允许下达)
UPDATE prod_work_order
SET order_status = 1, update_time = NOW()
WHERE id = 1001 AND order_status = 0 AND is_deleted = 0
  AND bom_id IS NOT NULL AND routing_id IS NOT NULL;
-- 受影响行数=0:要么已被并发下达,要么缺 BOM/路线,应用层查明原因提示

-- 1.3 齐套分析(按工单一次性算出所有物料的齐套结果;先删旧结果再写入,同一事务)
-- 净可用为负(锁定+在检>可用)时截 0，与 wiki/16 及应用层实现一致
START TRANSACTION;

UPDATE kit_analysis SET is_deleted = 1, update_time = NOW()
WHERE work_order_id = 1001 AND is_deleted = 0;

INSERT INTO kit_analysis
    (work_order_id, material_id, require_quantity, available_quantity, transit_quantity,
     shortage_quantity, kit_status, analysis_time)
SELECT t1.work_order_id,
       t1.material_id,
       t1.require_quantity - t1.issued_quantity,
       GREATEST(IFNULL(t2.available_quantity - t2.locked_quantity - t2.checking_quantity, 0), 0),
       IFNULL(t2.transit_quantity, 0),
       GREATEST((t1.require_quantity - t1.issued_quantity)
                - GREATEST(IFNULL(t2.available_quantity - t2.locked_quantity - t2.checking_quantity, 0), 0), 0),
       CASE
         WHEN (t1.require_quantity - t1.issued_quantity)
              <= GREATEST(IFNULL(t2.available_quantity - t2.locked_quantity - t2.checking_quantity, 0), 0) THEN 1
         WHEN GREATEST(IFNULL(t2.available_quantity - t2.locked_quantity - t2.checking_quantity, 0), 0) > 0 THEN 2
         ELSE 3
       END,
       NOW()
FROM prod_work_order_material AS t1
LEFT JOIN material_stock AS t2 ON t2.material_id = t1.material_id AND t2.is_deleted = 0
WHERE t1.work_order_id = 1001 AND t1.is_deleted = 0;

-- 冗余回写工单齐套状态(欠料>0 不能标记齐套)
UPDATE prod_work_order AS t1
SET t1.kit_status = (SELECT MAX(t2.kit_status) FROM kit_analysis AS t2
                     WHERE t2.work_order_id = t1.id AND t2.is_deleted = 0),
    t1.update_time = NOW()
WHERE t1.id = 1001;

COMMIT;

-- 1.4 创建派工单(悲观锁校验"派工数量不能超过工单未派数量")
START TRANSACTION;

-- 锁住工单行,防止两个计划员并发派工超量
SELECT id, plan_quantity, dispatched_quantity, order_status
FROM prod_work_order
WHERE id = 1001 AND is_deleted = 0
FOR UPDATE;
-- 应用层校验:order_status IN (1,2) 且 plan_quantity*(1+over_ratio) - dispatched_quantity >= 本次派工数

INSERT INTO dispatch_order
    (dispatch_no, work_order_id, line_id, shift_id, plan_date, plan_quantity,
     plan_start_time, plan_end_time, is_suggest, dispatch_status, create_by)
VALUES ('DP202607060001', 1001, 3, 1, '2026-07-08', 3000,
        '2026-07-08 08:00:00', '2026-07-08 17:00:00', 0, 0, 2);

UPDATE prod_work_order
SET dispatched_quantity = dispatched_quantity + 3000, update_time = NOW()
WHERE id = 1001
  AND dispatched_quantity + 3000 <= FLOOR(plan_quantity * (1 + over_ratio / 100));
-- WHERE 条件兜底超派;受影响行数=0 → 回滚

COMMIT;

-- 1.5 工单列表(游标分页/keyset:按 id 倒序,避免深分页 offset 扫描)
-- 第一页
SELECT id, work_order_no, product_name, plan_quantity, finish_quantity,
       order_status, kit_status, plan_end_time
FROM prod_work_order
WHERE workshop_id = 1 AND order_status = 2 AND is_deleted = 0
ORDER BY id DESC
LIMIT 20;
-- 下一页:携带上一页最后一条的 id 作为游标
SELECT id, work_order_no, product_name, plan_quantity, finish_quantity,
       order_status, kit_status, plan_end_time
FROM prod_work_order
WHERE workshop_id = 1 AND order_status = 2 AND is_deleted = 0
  AND id < 100230            -- 游标
ORDER BY id DESC
LIMIT 20;

-- 1.6 传统页码深分页兜底:延迟关联(INDEX-007),先索引定位 id 再回表
SELECT t1.id, t1.work_order_no, t1.product_name, t1.plan_quantity, t1.order_status
FROM prod_work_order AS t1,
     (SELECT id FROM prod_work_order
      WHERE workshop_id = 1 AND is_deleted = 0
      ORDER BY id DESC LIMIT 100000, 20) AS t2
WHERE t1.id = t2.id;

-- #####################################################################
-- 二、条码应用
-- #####################################################################

-- 2.1 流水号取号(核心并发点:唯一索引行 + FOR UPDATE 行锁,事务内取号并生成条码)
START TRANSACTION;

-- 不存在则初始化(唯一索引防并发重复初始化,冲突则忽略)
INSERT IGNORE INTO barcode_serial (rule_id, serial_scope, current_serial)
VALUES (1, 'P-TRAIN-01-20260708', 0);

-- 行锁串行化取号:同一规则同一维度同时只有一个事务能推进流水号
SELECT current_serial FROM barcode_serial
WHERE rule_id = 1 AND serial_scope = 'P-TRAIN-01-20260708'
FOR UPDATE;

-- 批量生成 N 个条码:一次性把流水号推进 N,区间内的号由应用层拼码
UPDATE barcode_serial
SET current_serial = current_serial + 100, update_time = NOW()
WHERE rule_id = 1 AND serial_scope = 'P-TRAIN-01-20260708';

-- 批量插入条码(条码值唯一索引兜底,任何一条重复整批回滚)
INSERT INTO barcode (barcode_value, barcode_type_id, barcode_mode, apply_rule_id,
                     product_id, batch_no, task_id, source_type, barcode_status, create_by)
VALUES ('P-TRAIN-01-20260708-0001', 1, 2, 5, 10, 'B20260708001', 2001, 1, 0, 2),
       ('P-TRAIN-01-20260708-0002', 1, 2, 5, 10, 'B20260708001', 2001, 1, 0, 2);
       -- ... 应用层按区间批量拼装,单批建议 <= 1000 行

COMMIT;

-- 2.2 扫码识别(uk_barcode_value 唯一索引,const 级查询)
SELECT t1.id, t1.barcode_value, t1.barcode_type_id, t1.barcode_mode, t1.product_id,
       t1.batch_no, t1.task_id, t1.barcode_status
FROM barcode AS t1
WHERE t1.barcode_value = 'P-TRAIN-01-20260708-0001' AND t1.is_deleted = 0;

-- 2.3 条码作废(CAS:已使用条码不能作废)
UPDATE barcode SET barcode_status = 2, update_time = NOW()
WHERE id = 30001 AND barcode_status = 0 AND is_deleted = 0;

-- 2.4 打印记录(重复打印累计次数,单行 upsert 免查询)
INSERT INTO barcode_print_record (barcode_id, template_id, print_by, print_count, print_time)
VALUES (30001, 2, 5, 1, NOW())
ON DUPLICATE KEY UPDATE print_count = print_count + 1,
                        reprint_reason = VALUES(reprint_reason),
                        print_time = NOW(), update_time = NOW();

-- #####################################################################
-- 三、现场管理(核心执行链路)
-- #####################################################################

-- 3.1 任务审核下发 → 平板开工(全部 CAS 状态机)
UPDATE prod_task SET task_status = 1, audit_by = 3, audit_time = NOW(), update_time = NOW()
WHERE id = 2001 AND task_status = 0 AND is_deleted = 0;

UPDATE prod_task SET task_status = 2, update_time = NOW()
WHERE id = 2001 AND task_status = 1 AND is_deleted = 0;

-- 平板开工:未审核任务不可开工由旧状态条件保证;记录实际开工时间(仅首次)
START TRANSACTION;

UPDATE prod_task
SET task_status = 3,
    actual_start_time = IFNULL(actual_start_time, NOW()),
    update_time = NOW()
WHERE id = 2001 AND task_status IN (2, 4) AND is_deleted = 0;
-- 受影响行数=0 → 状态不允许开工,回滚并提示

INSERT INTO prod_task_operate_log (task_id, operate_type, terminal_type, operator_id, operate_time)
VALUES (2001, 1, 2, 8, NOW());

COMMIT;

-- 3.2 按工艺路线生成工序派工(INSERT...SELECT,一条 SQL 展开整条路线)
START TRANSACTION;

INSERT INTO prod_process_dispatch (dispatch_no, task_id, routing_id, dispatch_status, create_by)
VALUES ('PD202607080001', 2001, 7, 0, 3);

SET @dispatch_id = LAST_INSERT_ID();

INSERT INTO prod_process_dispatch_detail
    (dispatch_id, process_id, seq, station_id, plan_quantity, detail_status)
SELECT @dispatch_id, t1.process_id, t1.seq, t1.station_id, t2.plan_quantity, 0
FROM craft_routing_detail AS t1
JOIN prod_task AS t2 ON t2.id = 2001
WHERE t1.routing_id = t2.routing_id AND t1.is_deleted = 0
ORDER BY t1.seq;

COMMIT;

-- 3.3 生产报工(整个系统最核心的事务:一次报工原子完成 5 件事)
-- 前置拦截:首件检验未通过不允许批量报工(qc_inspection idx_task_type 命中)
SELECT COUNT(*) AS first_check_pass
FROM qc_inspection AS t1
WHERE t1.task_id = 2001 AND t1.inspect_type = 1 AND t1.conclusion = 1
  AND t1.inspection_status = 1 AND t1.is_deleted = 0;
-- 启用首检参数且 first_check_pass = 0 → 拒绝报工,不开事务

START TRANSACTION;

-- (1) 锁定工序派工明细行(数量累加的锚点;锁粒度=单工位单工序,冲突面小)
SELECT id, plan_quantity, good_quantity, defect_quantity, detail_status
FROM prod_process_dispatch_detail
WHERE id = 40001 AND is_deleted = 0
FOR UPDATE;

-- (2) 写报工主记录
INSERT INTO prod_report
    (report_no, task_id, dispatch_detail_id, process_id, batch_no, barcode_id, report_type,
     user_id, equipment_id, station_id, input_quantity, good_quantity, defect_quantity,
     source_type, report_time)
VALUES ('RP202607080001', 2001, 40001, 12, 'B20260708001', 30001, 1,
        8, 15, 6, 100, 96, 4, 1, NOW());

SET @report_id = LAST_INSERT_ID();

-- (3) 不良明细(有不良才写)
INSERT INTO prod_report_defect (report_id, defect_reason_id, defect_quantity, defect_position, handle_type)
VALUES (@report_id, 9, 4, '羽片区', 1);

-- (4) 累加工序派工明细进度(WHERE 兜底:累计良品+不良不得超过计划量的合法上限)
UPDATE prod_process_dispatch_detail
SET good_quantity = good_quantity + 96,
    defect_quantity = defect_quantity + 4,
    detail_status = IF(good_quantity + 96 >= plan_quantity, 2, 1),
    update_time = NOW()
WHERE id = 40001
  AND good_quantity + defect_quantity + 100 <= plan_quantity;
-- 受影响行数=0 → 数量超限,整个事务回滚(数量异常被系统拦截)

-- (5) 累加任务进度(原子自增,无须先查后写)
UPDATE prod_task
SET input_quantity  = input_quantity + 100,
    good_quantity   = good_quantity + 96,
    defect_quantity = defect_quantity + 4,
    update_time = NOW()
WHERE id = 2001 AND task_status = 3;

-- (6) 更新批次当前工序与状态(报工推动状态流转)
UPDATE prod_batch_status
SET current_process_id = 12, batch_status = 1, update_time = NOW()
WHERE batch_no = 'B20260708001' AND is_deleted = 0;

-- (7) 同事务生成计件明细(uk_report_id 保证同一报工绝不重复计件)
INSERT INTO wage_piece_detail
    (stat_date, user_id, task_id, process_id, product_id, report_id,
     qualified_quantity, defect_quantity, unit_price, amount, audit_status)
SELECT CURDATE(), 8, 2001, 12, t2.product_id, @report_id,
       96, 4, t1.unit_price,
       ROUND(96 * t1.unit_price + CASE t1.defect_deduct_type
                                    WHEN 2 THEN 4 * t1.unit_price * 0.5
                                    WHEN 3 THEN 4 * t1.unit_price
                                    ELSE 0 END, 2),
       0
FROM wage_piece_price AS t1
JOIN prod_task AS t2 ON t2.id = 2001
WHERE t1.process_id = 12 AND t1.product_id = t2.product_id
  AND t1.effect_date <= CURDATE()
  AND (t1.expire_date IS NULL OR t1.expire_date >= CURDATE())
  AND t1.status = 1 AND t1.is_deleted = 0
ORDER BY t1.effect_date DESC
LIMIT 1;   -- 取生效日期最近的一条单价;工序不计件时该语句插入 0 行,属正常

COMMIT;

-- 3.4 报工修正(不改原记录:作废旧的 + 插入新的,保留完整痕迹)
START TRANSACTION;

UPDATE prod_report SET report_status = 2, update_time = NOW()
WHERE id = 50001 AND report_status = 1;
-- 同时按原记录数量对 prod_task / prod_process_dispatch_detail 做反向冲减(同 3.3 的累加逻辑取负)
-- 再插入一条新报工,origin_report_id = 50001

COMMIT;

-- 3.5 生成完工单(汇总校验在前,插入在后;完工数量不超可完工数量)
START TRANSACTION;

-- 锁任务防止完工期间继续报工/重复完工
SELECT id, task_status, plan_quantity, good_quantity, defect_quantity, rework_quantity
FROM prod_task WHERE id = 2001 AND is_deleted = 0
FOR UPDATE;

-- 校验必经工序全部完成(存在未完成工序则拒绝)
SELECT COUNT(*) AS unfinished
FROM prod_process_dispatch AS t1
JOIN prod_process_dispatch_detail AS t2 ON t2.dispatch_id = t1.id AND t2.is_deleted = 0
WHERE t1.task_id = 2001 AND t1.is_deleted = 0 AND t2.detail_status <> 2;
-- unfinished > 0 → 回滚

INSERT INTO prod_finish_order
    (finish_no, task_id, work_order_id, product_id, batch_no,
     finish_quantity, good_quantity, defect_quantity, rework_quantity, finish_status, create_by)
SELECT 'FN202607080001', t1.id, t1.work_order_id, t1.product_id, t1.batch_no,
       t1.good_quantity, t1.good_quantity, t1.defect_quantity, t1.rework_quantity, 0, 8
FROM prod_task AS t1 WHERE t1.id = 2001;

UPDATE prod_task SET task_status = 5, finish_quantity = good_quantity,
                     actual_end_time = NOW(), update_time = NOW()
WHERE id = 2001 AND task_status = 3;

-- 冗余回写工单汇总(避免报表联查)
UPDATE prod_work_order AS t1
JOIN prod_task AS t2 ON t2.id = 2001
SET t1.finish_quantity = t1.finish_quantity + t2.finish_quantity,
    t1.input_quantity  = t1.input_quantity + t2.input_quantity,
    t1.defect_quantity = t1.defect_quantity + t2.defect_quantity,
    t1.update_time = NOW()
WHERE t1.id = t2.work_order_id;

UPDATE prod_batch_status SET batch_status = 5, update_time = NOW()
WHERE task_id = 2001 AND batch_status = 1 AND is_deleted = 0;

COMMIT;

-- 完工单审核(CAS;已审核完工单不允许删除,由状态+逻辑删除约束保证)
UPDATE prod_finish_order SET finish_status = 1, audit_by = 3, audit_time = NOW(), update_time = NOW()
WHERE id = 60001 AND finish_status = 0 AND is_deleted = 0;

-- 3.6 返修闭环
-- 从不良报工生成返修单
INSERT INTO rework_order
    (rework_no, source_type, source_id, task_id, product_id, batch_no,
     defect_reason_id, defect_quantity, rework_process_id, handler_id, rework_status, create_by)
SELECT 'RW202607080001', 1, t1.report_id, t2.task_id, t3.product_id, t2.batch_no,
       t1.defect_reason_id, t1.defect_quantity, 12, 9, 0, 3
FROM prod_report_defect AS t1
JOIN prod_report AS t2 ON t2.id = t1.report_id
JOIN prod_task  AS t3 ON t3.id = t2.task_id
WHERE t1.id = 70001 AND t1.handle_type = 1;

-- 复检合格关闭(事务:返修单结论 + 任务返修数冲减 + 批次状态恢复)
START TRANSACTION;

UPDATE rework_order
SET rework_status = 3, recheck_by = 6, recheck_result = 1,
    qualified_quantity = 4, scrap_quantity = 0, recheck_time = NOW(), update_time = NOW()
WHERE id = 80001 AND rework_status = 2 AND is_deleted = 0
  AND 4 <= defect_quantity;    -- 返修数量不能超过来源不良数量

UPDATE prod_task SET good_quantity = good_quantity + 4, rework_quantity = rework_quantity + 4,
                     update_time = NOW()
WHERE id = 2001;

UPDATE prod_batch_status SET batch_status = 1, update_time = NOW()
WHERE batch_no = 'B20260708001' AND batch_status = 3 AND is_deleted = 0;

COMMIT;

-- #####################################################################
-- 四、设备管理 / 设备对接
-- #####################################################################

-- 4.1 报修状态机(发起→分派→维修→验收关闭;设备状态随之联动,同一事务)
START TRANSACTION;

INSERT INTO eqp_repair_order
    (repair_no, equipment_id, line_id, fault_reason_id, urgent_level, repair_desc,
     reporter_id, stop_start_time, repair_status)
VALUES ('RE202607080001', 15, 3, 4, 1, '注胶机出胶异常', 8, NOW(), 0);

UPDATE eqp_equipment SET run_status = 3, update_time = NOW()
WHERE id = 15 AND run_status IN (1, 2);   -- 报修中的设备标记为维修状态

COMMIT;

-- 分派 / 完成维修 / 验收关闭(CAS 链;未分派不能关闭由状态条件保证)
UPDATE eqp_repair_order SET assign_to = 9, repair_status = 1, update_time = NOW()
WHERE id = 90001 AND repair_status = 0;

UPDATE eqp_repair_order
SET repair_status = 2, real_fault_reason_id = 4, handle_process = '更换胶阀',
    handle_result = '修复', stop_end_time = NOW(),
    stop_minutes = TIMESTAMPDIFF(MINUTE, stop_start_time, NOW()), update_time = NOW()
WHERE id = 90001 AND repair_status = 1;

START TRANSACTION;
UPDATE eqp_repair_order SET repair_status = 3, accept_by = 3, update_time = NOW()
WHERE id = 90001 AND repair_status = 2;
UPDATE eqp_equipment SET run_status = 1, update_time = NOW()
WHERE id = 15 AND run_status = 3;
COMMIT;

-- 4.2 保养任务定时生成(扫描到期计划;NOW() 推进 next_time 防重复生成)
START TRANSACTION;

SELECT id, equipment_id, maintain_item, cycle_type, cycle_value, handler_id
FROM eqp_maintain_plan
WHERE status = 1 AND next_time <= NOW() AND is_deleted = 0
FOR UPDATE;    -- 防止两个调度实例同时生成同一批任务

INSERT INTO eqp_maintain_task (task_no, plan_id, equipment_id, plan_time, handler_id, task_status)
SELECT CONCAT('MT', DATE_FORMAT(NOW(), '%Y%m%d'), LPAD(t1.id, 6, '0')),
       t1.id, t1.equipment_id, t1.next_time, t1.handler_id, 0
FROM eqp_maintain_plan AS t1
WHERE t1.status = 1 AND t1.next_time <= NOW() AND t1.is_deleted = 0;

UPDATE eqp_maintain_plan
SET next_time = CASE cycle_type
                  WHEN 1 THEN DATE_ADD(next_time, INTERVAL cycle_value DAY)
                  WHEN 2 THEN DATE_ADD(next_time, INTERVAL cycle_value WEEK)
                  WHEN 3 THEN DATE_ADD(next_time, INTERVAL cycle_value MONTH)
                  WHEN 4 THEN DATE_ADD(next_time, INTERVAL cycle_value * 3 MONTH)
                  ELSE DATE_ADD(next_time, INTERVAL cycle_value YEAR)
                END,
    update_time = NOW()
WHERE status = 1 AND next_time <= NOW() AND is_deleted = 0;

COMMIT;

-- 4.3 设备计数写入(接口高频入口:INSERT IGNORE 幂等去重,不加锁不阻塞)
INSERT IGNORE INTO eqp_count_record
    (equipment_id, collect_time, serial_no, count_value, increment_value, run_status, match_status)
VALUES (15, '2026-07-08 10:30:00', 'SN0001', 5230, 30, 1, 0);
-- 受影响行数=0 即 uk_eqp_time_serial 命中重复上报,直接丢弃,天然幂等

-- 计数匹配当前任务(定时批处理:匹配成功挂任务,失败进异常池;避免逐条事务)
START TRANSACTION;

UPDATE eqp_count_record AS t1
JOIN eqp_access_config AS t2
  ON t2.equipment_id = t1.equipment_id AND t2.is_enabled = 1 AND t2.debug_status = 2 AND t2.is_deleted = 0
JOIN prod_task AS t3
  ON t3.line_id = t2.line_id AND t3.task_status = 3 AND t3.is_deleted = 0
SET t1.task_id = t3.id, t1.process_id = t2.process_id, t1.match_status = 1, t1.update_time = NOW()
WHERE t1.match_status = 0;

INSERT INTO eqp_count_error (equipment_id, collect_time, raw_data, error_type, handle_status)
SELECT t1.equipment_id, t1.collect_time,
       CONCAT('count=', t1.count_value, ',serial=', IFNULL(t1.serial_no, '')), 1, 0
FROM eqp_count_record AS t1
WHERE t1.match_status = 0 AND t1.create_time < DATE_SUB(NOW(), INTERVAL 5 MINUTE);

UPDATE eqp_count_record SET match_status = 2, update_time = NOW()
WHERE match_status = 0 AND create_time < DATE_SUB(NOW(), INTERVAL 5 MINUTE);

COMMIT;

-- 4.4 OEE 日结(INSERT...SELECT + ON DUPLICATE:重跑不重复,统计维度唯一索引兜底)
INSERT INTO eqp_oee_statistic
    (stat_date, equipment_id, line_id, shift_id, plan_minutes, run_minutes, stop_minutes,
     output_quantity, defect_quantity, time_rate, quality_rate, oee)
SELECT '2026-07-08', t1.equipment_id, t1.line_id, NULL,
       480,
       SUM(IF(t1.run_status = 1, t1.duration_minutes, 0)),
       SUM(IF(t1.run_status <> 1, t1.duration_minutes, 0)),
       IFNULL(t2.output_qty, 0), IFNULL(t2.defect_qty, 0),
       ROUND(SUM(IF(t1.run_status = 1, t1.duration_minutes, 0)) / 480 * 100, 2),
       IF(IFNULL(t2.output_qty, 0) = 0, NULL,
          ROUND((t2.output_qty - t2.defect_qty) / t2.output_qty * 100, 2)),
       NULL   -- oee 由应用层依据三率与理论节拍计算后回填,缺节拍时保持 NULL
FROM eqp_run_record AS t1
LEFT JOIN (SELECT equipment_id,
                  SUM(good_quantity + defect_quantity) AS output_qty,
                  SUM(defect_quantity) AS defect_qty
           FROM prod_report
           WHERE report_time >= '2026-07-08' AND report_time < '2026-07-09'
             AND report_status = 1 AND is_deleted = 0
           GROUP BY equipment_id) AS t2 ON t2.equipment_id = t1.equipment_id
WHERE t1.start_time >= '2026-07-08' AND t1.start_time < '2026-07-09' AND t1.is_deleted = 0
GROUP BY t1.equipment_id, t1.line_id, t2.output_qty, t2.defect_qty
ON DUPLICATE KEY UPDATE run_minutes = VALUES(run_minutes), stop_minutes = VALUES(stop_minutes),
                        output_quantity = VALUES(output_quantity), defect_quantity = VALUES(defect_quantity),
                        time_rate = VALUES(time_rate), quality_rate = VALUES(quality_rate),
                        update_time = NOW();

-- #####################################################################
-- 五、计件工资
-- #####################################################################

-- 5.1 员工查询本人计件(idx_user_date 覆盖高频入口;先 count 后分页 SQL-005)
SELECT COUNT(*) FROM wage_piece_detail
WHERE user_id = 8 AND stat_date BETWEEN '2026-07-01' AND '2026-07-31' AND is_deleted = 0;
-- count = 0 直接返回空页,不再执行下面的查询

SELECT id, stat_date, process_id, qualified_quantity, defect_quantity, unit_price, amount, audit_status
FROM wage_piece_detail
WHERE user_id = 8 AND stat_date BETWEEN '2026-07-01' AND '2026-07-31' AND is_deleted = 0
ORDER BY stat_date DESC, id DESC
LIMIT 20;

-- 5.2 批量审核(审核后不可修改:后续 UPDATE 一律带 audit_status = 0 条件)
UPDATE wage_piece_detail
SET audit_status = 1, audit_by = 3, audit_time = NOW(), update_time = NOW()
WHERE stat_date = '2026-07-08' AND audit_status = 0 AND is_deleted = 0
  AND id IN (1, 2, 3);   -- in 集合由前端勾选传入,控制在 1000 以内(SQL-011)

-- 5.3 工序金额汇总(idx_date_process)
SELECT t1.process_id, t2.process_name,
       SUM(t1.qualified_quantity) AS total_qty, SUM(t1.amount) AS total_amount
FROM wage_piece_detail AS t1
JOIN craft_process AS t2 ON t2.id = t1.process_id
WHERE t1.stat_date BETWEEN '2026-07-01' AND '2026-07-31'
  AND t1.audit_status = 1 AND t1.is_deleted = 0
GROUP BY t1.process_id, t2.process_name;

-- #####################################################################
-- 六、质量管理
-- #####################################################################

-- 6.1 生成首件检验单(自动带出适用方案:客户方案优先,其次默认方案)
INSERT INTO qc_inspection
    (inspection_no, inspect_type, task_id, work_order_id, product_id, batch_no,
     scheme_id, sample_quantity, inspector_id, inspect_time, release_status, inspection_status)
SELECT 'QC202607080001', 1, t1.id, t1.work_order_id, t1.product_id, t1.batch_no,
       t2.id, 1, 6, NOW(), 0, 0
FROM prod_task AS t1
JOIN qc_scheme AS t2
  ON t2.product_id = t1.product_id AND t2.inspect_type = 1
 AND t2.scheme_status = 1 AND t2.is_deleted = 0
LEFT JOIN prod_work_order AS t3 ON t3.id = t1.work_order_id
WHERE t1.id = 2001
ORDER BY (t2.customer_id = t3.customer_id) DESC, t2.is_default DESC
LIMIT 1;

-- 6.2 提交检验结果(事务:项目结果 + 单据结论 + 联动放行/停线)
START TRANSACTION;

INSERT INTO qc_inspection_item (inspection_id, item_id, measured_value, judge_result)
VALUES (100001, 1, '5.05', 1), (100001, 2, '外观正常', 1);

-- 结论 = 所有必检项目都合格才合格(一条 SQL 汇总判定)
UPDATE qc_inspection AS t1
SET t1.conclusion = IF(EXISTS (SELECT 1 FROM qc_inspection_item AS t2
                               WHERE t2.inspection_id = t1.id AND t2.judge_result = 2
                                 AND t2.is_deleted = 0), 2, 1),
    t1.release_status = IF(EXISTS (SELECT 1 FROM qc_inspection_item AS t2
                                   WHERE t2.inspection_id = t1.id AND t2.judge_result = 2
                                     AND t2.is_deleted = 0), 2, 1),
    t1.inspection_status = 1,
    t1.update_time = NOW()
WHERE t1.id = 100001 AND t1.inspection_status = 0;

COMMIT;

-- 6.3 入库检验放行校验(仓库办理入库前调用:未放行批次拦截)
SELECT t1.id, t1.conclusion, t1.release_status
FROM qc_inspection AS t1
WHERE t1.finish_order_id = 60001 AND t1.inspect_type = 4
  AND t1.inspection_status = 1 AND t1.is_deleted = 0
ORDER BY t1.id DESC
LIMIT 1;
-- release_status <> 1 → 不能作为合格品入库

-- 6.4 让步接收(需要权限与原因,CAS 防重复处理)
UPDATE qc_inspection
SET conclusion = 3, release_status = 1, concession_by = 7,
    concession_reason = '轻微色差,客户书面同意', update_time = NOW()
WHERE id = 100002 AND conclusion = 2 AND is_deleted = 0;

-- #####################################################################
-- 七、安灯管理
-- #####################################################################

-- 7.1 发起异常(事务:匹配处理人 + 写异常单 + 灯控状态;产线级配置优先于全局配置)
START TRANSACTION;

INSERT INTO andon_exception
    (exception_no, andon_type_id, reason_id, workshop_id, line_id, task_id, process_id,
     equipment_id, batch_no, description, source_terminal, reporter_id,
     handler_id, light_status, exception_status)
SELECT 'AD202607080001', 2, 5, 1, 3, 2001, 12,
       15, 'B20260708001', '注胶工位缺胶水', 2, 8,
       t1.handler_id,
       IF(t2.is_light_control = 1, 1, 3),
       0
FROM andon_type AS t2
LEFT JOIN andon_config AS t1
  ON t1.andon_type_id = t2.id AND t1.status = 1 AND t1.is_deleted = 0
 AND (t1.line_id = 3 OR t1.line_id IS NULL)
WHERE t2.id = 2 AND t2.status = 1 AND t2.is_deleted = 0
ORDER BY t1.line_id IS NULL   -- 产线专属配置排前
LIMIT 1;

INSERT INTO andon_handle_record (exception_id, action_type, handler_id, action_remark, action_time)
VALUES (LAST_INSERT_ID(), 1, 8, '现场发起', NOW());

COMMIT;

-- 7.2 处理人确认 / 关闭(CAS;关闭必填处理结果,联动关灯与停线时长)
UPDATE andon_exception SET exception_status = 1, confirm_time = NOW(), update_time = NOW()
WHERE id = 110001 AND exception_status = 0 AND handler_id = 9;

START TRANSACTION;

UPDATE andon_exception
SET exception_status = 2, close_time = NOW(),
    handle_result = '补料完成恢复生产',
    stop_minutes = TIMESTAMPDIFF(MINUTE, create_time, NOW()),
    light_status = IF(light_status = 1, 2, light_status),
    update_time = NOW()
WHERE id = 110001 AND exception_status = 1;

INSERT INTO andon_handle_record (exception_id, action_type, handler_id, action_remark, cost_minutes, action_time)
VALUES (110001, 5, 9, '补料完成恢复生产', 25, NOW());

COMMIT;

-- 7.3 超时升级扫描(定时任务:响应超时未确认的异常,标记并升级通知)
UPDATE andon_exception AS t1
JOIN andon_config AS t2
  ON t2.andon_type_id = t1.andon_type_id AND t2.status = 1 AND t2.is_deleted = 0
 AND (t2.line_id = t1.line_id OR t2.line_id IS NULL)
SET t1.is_timeout = 1, t1.handler_id = IFNULL(t2.upgrade_to, t1.handler_id), t1.update_time = NOW()
WHERE t1.exception_status = 0 AND t1.is_timeout = 0 AND t1.is_deleted = 0
  AND t1.create_time < DATE_SUB(NOW(), INTERVAL t2.response_minutes MINUTE);

-- 7.4 处理人待办列表(idx_handler_status,移动端高频)
SELECT id, exception_no, andon_type_id, line_id, description, create_time, is_timeout
FROM andon_exception
WHERE handler_id = 9 AND exception_status IN (0, 1) AND is_deleted = 0
ORDER BY is_timeout DESC, create_time
LIMIT 50;

-- #####################################################################
-- 八、追溯与报表(只读查询,索引全命中)
-- #####################################################################

-- 8.1 产品追溯:批次码一码到底(功能19/49/51 共用同一组查询)
-- (a) 批次主档 + 任务 + 工单
SELECT t1.batch_no, t1.batch_status, t1.current_process_id,
       t2.task_no, t2.line_id, t2.plan_date,
       t3.work_order_no, t3.product_name
FROM prod_batch_status AS t1
JOIN prod_task AS t2 ON t2.id = t1.task_id
JOIN prod_work_order AS t3 ON t3.id = t2.work_order_id
WHERE t1.batch_no = 'B20260708001' AND t1.is_deleted = 0;

-- (b) 工序履历(idx_batch_no,按时间正序展示)
SELECT t1.report_no, t1.process_id, t2.process_name, t1.user_id, t1.equipment_id,
       t1.input_quantity, t1.good_quantity, t1.defect_quantity, t1.report_time
FROM prod_report AS t1
JOIN craft_process AS t2 ON t2.id = t1.process_id
WHERE t1.batch_no = 'B20260708001' AND t1.report_status = 1 AND t1.is_deleted = 0
ORDER BY t1.report_time;

-- (c) 质检 / 返修 / 异常(三段查询同构,均命中各自 idx_batch_no)
SELECT id, inspection_no, inspect_type, conclusion, inspect_time
FROM qc_inspection WHERE batch_no = 'B20260708001' AND is_deleted = 0 ORDER BY inspect_time;

SELECT id, rework_no, defect_quantity, rework_status, recheck_result, create_time
FROM rework_order WHERE batch_no = 'B20260708001' AND is_deleted = 0 ORDER BY create_time;

SELECT id, exception_no, andon_type_id, exception_status, stop_minutes, create_time
FROM andon_exception WHERE batch_no = 'B20260708001' AND is_deleted = 0 ORDER BY create_time;

-- 8.2 关键物料追溯(功能50)
-- 正向:产品批次 → 用了哪些物料批次(idx_product_batch)
SELECT t1.material_id, t2.material_name, t1.material_batch_no, SUM(t1.use_quantity) AS use_qty
FROM prod_report_material AS t1
JOIN base_material AS t2 ON t2.id = t1.material_id
WHERE t1.product_batch_no = 'B20260708001' AND t1.is_deleted = 0
GROUP BY t1.material_id, t2.material_name, t1.material_batch_no;

-- 反向:物料批次 → 影响了哪些产品批次(idx_material_batch,质量事故圈定范围)
SELECT DISTINCT t1.product_batch_no, t2.task_no, t3.work_order_no
FROM prod_report_material AS t1
JOIN prod_report AS t4 ON t4.id = t1.report_id
JOIN prod_task  AS t2 ON t2.id = t4.task_id
JOIN prod_work_order AS t3 ON t3.id = t2.work_order_id
WHERE t1.material_id = 21 AND t1.material_batch_no = 'GLUE-20260701-03' AND t1.is_deleted = 0;

-- 8.3 产量报表(按日汇总,区分报工口径;count(*) 而非 count(列) SQL-001)
SELECT DATE(t1.report_time) AS stat_date, t2.line_id,
       SUM(t1.good_quantity) AS good_qty, SUM(t1.defect_quantity) AS defect_qty,
       COUNT(*) AS report_count
FROM prod_report AS t1
JOIN prod_task AS t2 ON t2.id = t1.task_id
WHERE t1.report_time >= '2026-07-01' AND t1.report_time < '2026-08-01'
  AND t1.report_status = 1 AND t1.is_deleted = 0
GROUP BY DATE(t1.report_time), t2.line_id;

-- 8.4 车间时段报表日结(唯一索引防重复统计,重跑幂等)
INSERT INTO rpt_workshop_period
    (stat_type, stat_date, workshop_id, plan_quantity, output_quantity,
     achieve_rate, defect_rate, exception_count, stop_minutes)
SELECT 1, '2026-07-08', t1.workshop_id,
       SUM(t2.plan_quantity), SUM(t2.good_quantity),
       IF(SUM(t2.plan_quantity) = 0, NULL, ROUND(SUM(t2.good_quantity) / SUM(t2.plan_quantity) * 100, 2)),
       IF(SUM(t2.input_quantity) = 0, NULL, ROUND(SUM(t2.defect_quantity) / SUM(t2.input_quantity) * 100, 2)),
       IFNULL(t3.exception_count, 0), IFNULL(t3.stop_minutes, 0)
FROM base_production_line AS t1
JOIN prod_task AS t2 ON t2.line_id = t1.id AND t2.plan_date = '2026-07-08' AND t2.is_deleted = 0
LEFT JOIN (SELECT t4.workshop_id, COUNT(*) AS exception_count,
                  IFNULL(SUM(t4.stop_minutes), 0) AS stop_minutes
           FROM andon_exception AS t4
           WHERE t4.create_time >= '2026-07-08' AND t4.create_time < '2026-07-09' AND t4.is_deleted = 0
           GROUP BY t4.workshop_id) AS t3 ON t3.workshop_id = t1.workshop_id
GROUP BY t1.workshop_id, t3.exception_count, t3.stop_minutes
ON DUPLICATE KEY UPDATE output_quantity = VALUES(output_quantity),
                        achieve_rate = VALUES(achieve_rate), defect_rate = VALUES(defect_rate),
                        exception_count = VALUES(exception_count), stop_minutes = VALUES(stop_minutes),
                        update_time = NOW();

-- 8.5 看板实时查询(产线看板,只读、限定当前在制,毫秒级返回)
SELECT t1.id, t1.task_no, t1.product_name, t1.plan_quantity, t1.good_quantity,
       t1.defect_quantity,
       IF(t1.plan_quantity = 0, 0, ROUND(t1.good_quantity / t1.plan_quantity * 100, 1)) AS achieve_rate
FROM prod_task AS t1
WHERE t1.line_id = 3 AND t1.task_status = 3 AND t1.is_deleted = 0
ORDER BY t1.actual_start_time DESC
LIMIT 1;

SELECT COUNT(*) AS open_exception
FROM andon_exception
WHERE line_id = 3 AND exception_status IN (0, 1) AND is_deleted = 0;

-- 8.6 不良查询(汇总→明细两段式;不良原因为空单列为未分类)
SELECT IFNULL(t3.defect_name, '未分类') AS defect_name,
       SUM(t1.defect_quantity) AS defect_qty
FROM prod_report_defect AS t1
JOIN prod_report AS t2 ON t2.id = t1.report_id
  AND t2.report_time >= '2026-07-01' AND t2.report_time < '2026-08-01'
  AND t2.report_status = 1 AND t2.is_deleted = 0
LEFT JOIN craft_process_defect AS t3 ON t3.id = t1.defect_reason_id
WHERE t1.is_deleted = 0
GROUP BY t3.defect_name
ORDER BY defect_qty DESC;

-- #####################################################################
-- 九、接口读取(对外提供数据)
-- #####################################################################

-- 9.1 完工单读取(ERP/WMS 轮询:只给已审核,idx_status_sync 命中;游标增量拉取)
SELECT t1.id, t1.finish_no, t2.work_order_no, t1.product_id, t1.batch_no,
       t1.finish_quantity, t1.good_quantity, t1.defect_quantity, t1.finish_status
FROM prod_finish_order AS t1
JOIN prod_work_order AS t2 ON t2.id = t1.work_order_id
WHERE t1.finish_status = 1 AND t1.sync_status = 0 AND t1.is_deleted = 0
  AND t1.id > 60000        -- 外部系统记录上次游标,增量拉取
ORDER BY t1.id
LIMIT 200;

-- 读取确认回执(标记已读取;重复读取返回相同数据、不改业务内容)
UPDATE prod_finish_order SET sync_status = 1, update_time = NOW()
WHERE id IN (60001, 60002) AND finish_status = 1 AND sync_status = 0;

-- 9.2 计量单位写入(upsert:已存在按规则更新,不污染基础资料)
INSERT INTO base_unit (unit_code, unit_name, unit_precision, source_type, status)
VALUES ('DOZEN', '打', 0, 2, 1)
ON DUPLICATE KEY UPDATE unit_name = VALUES(unit_name), update_time = NOW();
-- 注意:已被业务单据使用的单位不改精度 —— 精度变更走人工审核,不在接口 upsert 中处理
