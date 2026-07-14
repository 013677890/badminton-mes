-- V5: 派工单(排产)
-- base_production_line、base_shift、base_factory_calendar、dispatch_order 照抄 wiki/database/mes_schema.sql；
-- prod_dispatch_adjust_log 为本模块新增(排产调整日志，schema 缺此表，待同步回 wiki)。
-- 索引必建，不加物理外键，关联一致性由应用层保证(库内既有风格)。

CREATE TABLE `base_production_line` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `line_code` varchar(32) NOT NULL COMMENT '产线编码(唯一)',
  `line_name` varchar(64) NOT NULL COMMENT '产线名称',
  `workshop_id` bigint unsigned NOT NULL COMMENT '所属车间',
  `standard_capacity` int unsigned NULL DEFAULT NULL COMMENT '标准日产能(只)',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_line_code` (`line_code`) COMMENT '产线编码唯一',
  KEY `idx_workshop_id` (`workshop_id`) COMMENT '车间下产线列表'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '产线表';

CREATE TABLE `base_shift` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shift_code` varchar(32) NOT NULL COMMENT '班次编码(唯一)',
  `shift_name` varchar(64) NOT NULL COMMENT '班次名称(白班/夜班)',
  `start_time` time NOT NULL COMMENT '班次开始时间',
  `end_time` time NOT NULL COMMENT '班次结束时间',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shift_code` (`shift_code`) COMMENT '班次编码唯一'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '班次表';

CREATE TABLE `base_factory_calendar` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `calendar_date` date NOT NULL COMMENT '日历日期',
  `workshop_id` bigint unsigned NOT NULL COMMENT '适用车间',
  `is_workday` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '是否工作日:1是 0否',
  `remark` varchar(255) NULL DEFAULT NULL COMMENT '备注(节假日/调休说明)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workshop_date` (`workshop_id`, `calendar_date`) COMMENT '同车间同日期仅一条'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '工厂日历表';

CREATE TABLE `dispatch_order` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dispatch_no` varchar(32) NOT NULL COMMENT '派工单号(唯一)',
  `work_order_id` bigint unsigned NOT NULL COMMENT '来源生产工单',
  `line_id` bigint unsigned NOT NULL COMMENT '产线',
  `shift_id` bigint unsigned NOT NULL COMMENT '班次',
  `plan_date` date NOT NULL COMMENT '排产日期',
  `plan_quantity` int unsigned NOT NULL COMMENT '计划数量(不超工单未派数量)',
  `plan_start_time` datetime NOT NULL COMMENT '计划开始时间',
  `plan_end_time` datetime NOT NULL COMMENT '计划结束时间',
  `is_suggest` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否系统建议排产:1是 0人工',
  `dispatch_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '状态:0待审核 1已审核 2已下发 3执行中 4已完成 5已取消',
  `audit_by` bigint unsigned NULL DEFAULT NULL COMMENT '审核人',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `adjust_reason` varchar(255) NULL DEFAULT NULL COMMENT '下发后调整原因',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dispatch_no` (`dispatch_no`) COMMENT '派工单号唯一',
  KEY `idx_work_order_id` (`work_order_id`) COMMENT '按工单查派工',
  KEY `idx_line_date_shift` (`line_id`, `plan_date`, `shift_id`) COMMENT '产线排程视图(同线同时段防超产能校验)'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '派工单主表(排产结果)';

CREATE TABLE `prod_dispatch_adjust_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dispatch_order_id` bigint unsigned NOT NULL COMMENT '派工单',
  `adjust_type` tinyint unsigned NOT NULL COMMENT '记录类型:1系统建议 2人工创建 3调整 4审核 5下发 6取消',
  `before_snapshot` varchar(512) NULL DEFAULT NULL COMMENT '调整前快照(产线/班次/日期/数量 JSON)',
  `after_snapshot` varchar(512) NULL DEFAULT NULL COMMENT '调整后快照(JSON)',
  `adjust_reason` varchar(255) NULL DEFAULT NULL COMMENT '调整原因(下发后调整必填)',
  `operator_id` bigint unsigned NOT NULL COMMENT '操作人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_dispatch_order_id` (`dispatch_order_id`) COMMENT '按派工单查调整轨迹'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '派工单排产调整日志表';

-- 种子数据：产线挂 V1 已建车间(id=1)，白/夜班，2026 下半年工厂日历(周末非工作日)。
INSERT INTO `base_production_line` (`line_code`, `line_name`, `workshop_id`, `standard_capacity`, `status`) VALUES
('LINE-01', '一号成型线', 1, 5000, 1),
('LINE-02', '二号成型线', 1, 4000, 1);

INSERT INTO `base_shift` (`shift_code`, `shift_name`, `start_time`, `end_time`, `status`) VALUES
('DAY',   '白班', '08:00:00', '20:00:00', 1),
('NIGHT', '夜班', '20:00:00', '08:00:00', 1);

-- 递归 CTE 生成 2026-07-01 ~ 2026-12-31 的日历，周六日记非工作日(MySQL 8)
INSERT INTO `base_factory_calendar` (`calendar_date`, `workshop_id`, `is_workday`, `remark`)
WITH RECURSIVE calendar_dates AS (
  SELECT DATE '2026-07-01' AS calendar_date
  UNION ALL
  SELECT calendar_date + INTERVAL 1 DAY FROM calendar_dates WHERE calendar_date < DATE '2026-12-31'
)
SELECT calendar_date,
       1,
       IF(WEEKDAY(calendar_date) < 5, 1, 0),
       IF(WEEKDAY(calendar_date) < 5, NULL, '周末')
FROM calendar_dates;
