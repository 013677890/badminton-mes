-- ----------------------------------------------------------------------------
-- V1 生产工单样例切片：车间、产品、生产工单三张表 + 联调种子数据
--
-- 完整库表结构见 wiki/database/mes_schema.sql。本迁移只建样例接口涉及的表；
-- 业务关系由 Service 校验与应用层约束处理；
-- 唯一约束和查询索引仍在数据库层兜底。
-- ----------------------------------------------------------------------------

-- 车间表
CREATE TABLE `base_workshop` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `workshop_code` varchar(32) NOT NULL COMMENT '车间编码(唯一)',
  `workshop_name` varchar(64) NOT NULL COMMENT '车间名称',
  `manager_id` bigint unsigned NULL DEFAULT NULL COMMENT '车间主管',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workshop_code` (`workshop_code`) COMMENT '车间编码唯一'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '车间表';

-- 产品表
CREATE TABLE `base_product` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_code` varchar(32) NOT NULL COMMENT '产品编码(唯一)',
  `product_name` varchar(128) NOT NULL COMMENT '产品名称(训练球/比赛球等)',
  `spec` varchar(128) NULL DEFAULT NULL COMMENT '规格型号',
  `product_type` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '产品类型:1成品 2半成品',
  `grade` varchar(32) NULL DEFAULT NULL COMMENT '产品等级',
  `unit_id` bigint unsigned NOT NULL COMMENT '计量单位',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_code` (`product_code`) COMMENT '产品编码唯一'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '产品表';

-- 生产工单主表
CREATE TABLE `prod_work_order` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_no` varchar(32) NOT NULL COMMENT '工单号(唯一;日期+流水)',
  `source_type` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '来源:1手工 2导入 3ERP同步 4API写入',
  `source_order_no` varchar(64) NULL DEFAULT NULL COMMENT '外部来源单号(ERP/API;来源内唯一)',
  `product_id` bigint unsigned NOT NULL COMMENT '产品',
  `product_name` varchar(128) NOT NULL COMMENT '产品名称(冗余;避免联查)',
  `spec` varchar(128) NULL DEFAULT NULL COMMENT '规格型号(冗余)',
  `unit_id` bigint unsigned NOT NULL COMMENT '计量单位',
  `batch_no` varchar(64) NULL DEFAULT NULL COMMENT '生产批次号',
  `bom_id` bigint unsigned NULL DEFAULT NULL COMMENT 'BOM版本(下达前必须维护)',
  `routing_id` bigint unsigned NULL DEFAULT NULL COMMENT '工艺路线(下达前必须维护)',
  `customer_id` bigint unsigned NULL DEFAULT NULL COMMENT '客户',
  `workshop_id` bigint unsigned NOT NULL COMMENT '目标车间',
  `plan_quantity` int unsigned NOT NULL COMMENT '计划数量',
  `dispatched_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '已派工数量',
  `input_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '投入数量(汇总冗余)',
  `finish_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '完工数量(汇总冗余)',
  `defect_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '不良数量(汇总冗余)',
  `rework_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '返修数量(汇总冗余)',
  `over_ratio` decimal(5,2) NOT NULL DEFAULT 0 COMMENT '允许超产比例(%)',
  `priority` tinyint unsigned NOT NULL DEFAULT 5 COMMENT '优先级:1最高-9最低',
  `plan_start_time` datetime NOT NULL COMMENT '计划开始时间',
  `plan_end_time` datetime NOT NULL COMMENT '计划完成时间(交期)',
  `order_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '工单状态:0已创建 1已下达 2生产中 3暂停 4已完工 5已关闭 6已作废',
  `kit_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '齐套状态:0未分析 1齐套 2部分齐套 3欠料(冗余)',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_work_order_no` (`work_order_no`) COMMENT '工单号唯一(业务强制)',
  UNIQUE KEY `uk_source_order` (`source_type`, `source_order_no`(32)) COMMENT '外部单号防重复生成(ERP/API)',
  KEY `idx_product_id` (`product_id`) COMMENT '按产品查工单',
  KEY `idx_workshop_status` (`workshop_id`, `order_status`, `plan_end_time`) COMMENT '车间+状态+交期(等值在前排序在后INDEX-005)',
  KEY `idx_batch_no` (`batch_no`(20)) COMMENT '批次追溯反查工单'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '生产工单主表';

-- 联调种子数据(unit_id 暂以 1 占位，计量单位表建设后修正)
INSERT INTO `base_workshop` (`workshop_code`, `workshop_name`, `status`) VALUES
('WS001', '一号成型车间', 1),
('WS002', '二号包装车间', 1);

INSERT INTO `base_product` (`product_code`, `product_name`, `spec`, `product_type`, `grade`, `unit_id`, `status`) VALUES
('P001', '比赛级羽毛球', '77速 鹅毛', 1, 'A', 1, 1),
('P002', '训练级羽毛球', '76速 鸭毛', 1, 'B', 1, 1),
('P003', '尼龙羽毛球', '中速 尼龙', 1, 'C', 1, 0);
