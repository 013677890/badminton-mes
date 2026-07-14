-- V4: 齐套分析与欠料看板
-- material_stock、kit_analysis 照抄 wiki/database/mes_schema.sql；
-- prod_kit_shortage_handle 为本模块新增(欠料处理记录，schema 缺此表，待同步回 wiki)。
-- 索引必建，不加物理外键，关联一致性由应用层保证(库内既有风格)。

CREATE TABLE `material_stock` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `material_id` bigint unsigned NOT NULL COMMENT '物料(唯一)',
  `available_quantity` decimal(12,4) NOT NULL DEFAULT 0 COMMENT '可用数量',
  `locked_quantity` decimal(12,4) NOT NULL DEFAULT 0 COMMENT '已锁定数量',
  `checking_quantity` decimal(12,4) NOT NULL DEFAULT 0 COMMENT '在检数量',
  `transit_quantity` decimal(12,4) NOT NULL DEFAULT 0 COMMENT '在途数量',
  `sync_time` datetime NOT NULL COMMENT '库存同步时间(来自WMS/ERP)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_material_id` (`material_id`) COMMENT '一物料一条库存快照'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '物料库存可用量表';

CREATE TABLE `kit_analysis` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id` bigint unsigned NOT NULL COMMENT '生产工单',
  `material_id` bigint unsigned NOT NULL COMMENT '物料',
  `require_quantity` decimal(12,4) NOT NULL COMMENT '需求数量',
  `available_quantity` decimal(12,4) NOT NULL DEFAULT 0 COMMENT '可用数量(已扣锁定与在检)',
  `transit_quantity` decimal(12,4) NOT NULL DEFAULT 0 COMMENT '在途数量',
  `shortage_quantity` decimal(12,4) NOT NULL DEFAULT 0 COMMENT '欠料数量',
  `kit_status` tinyint unsigned NOT NULL COMMENT '齐套状态:1齐套 2部分齐套 3欠料',
  `analysis_time` datetime NOT NULL COMMENT '分析时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_order_status` (`work_order_id`, `kit_status`) COMMENT '按工单查齐套结果',
  KEY `idx_material_id` (`material_id`) COMMENT '欠料看板按物料汇总'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '齐套分析结果表';

CREATE TABLE `prod_kit_shortage_handle` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id` bigint unsigned NOT NULL COMMENT '生产工单',
  `material_id` bigint unsigned NOT NULL COMMENT '欠料物料',
  `handle_type` tinyint unsigned NOT NULL COMMENT '处理方式:1催采购 2调拨 3代用料 4调整排产',
  `handler_id` bigint unsigned NOT NULL COMMENT '责任人(sys_user.id)',
  `expected_arrival_date` date NULL DEFAULT NULL COMMENT '预计到料日期',
  `handle_remark` varchar(255) NULL DEFAULT NULL COMMENT '处理说明',
  `handle_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '处理状态:0处理中 1已解决',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_order_material` (`work_order_id`, `material_id`) COMMENT '按工单物料查处理记录',
  KEY `idx_material_status` (`material_id`, `handle_status`) COMMENT '看板取物料未解决处理的预计到料'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '欠料处理记录表';

-- 种子数据：对应 V2 已建的 4 条 base_material(M001~M004)。
-- M001 充足；M002 可用扣除锁定在检后不足且有在途(欠料看板演示)；M003 可用为 0；M004 充足。
INSERT INTO `material_stock`
    (`material_id`, `available_quantity`, `locked_quantity`, `checking_quantity`, `transit_quantity`, `sync_time`) VALUES
(1, 50000.0000, 2000.0000, 1000.0000, 0.0000, NOW()),
(2, 30000.0000, 20000.0000, 5000.0000, 80000.0000, NOW()),
(3, 0.0000, 0.0000, 0.0000, 200.0000, NOW()),
(4, 900.0000, 0.0000, 0.0000, 0.0000, NOW());
