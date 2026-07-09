-- ----------------------------------------------------------------------------
-- V2 生产工单补全：物料、BOM、工单物料需求、工单状态日志 + 联调种子数据
--
-- base_material / base_bom / base_bom_detail / prod_work_order_material
-- 照抄 wiki/database/mes_schema.sql；prod_work_order_status_log 为本迁移新增
-- (需求验收要求"工单状态变化有日志可查"，schema 缺此表，待同步回 wiki)。
-- 业务关系由 Service 校验与应用层约束处理，不建实体外键；
-- 唯一约束和查询索引仍在数据库层兜底。
-- ----------------------------------------------------------------------------

-- 物料表
CREATE TABLE `base_material` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `material_code` varchar(32) NOT NULL COMMENT '物料编码(唯一)',
  `material_name` varchar(128) NOT NULL COMMENT '物料名称(球头/羽片/胶水/线圈/包装材料)',
  `spec` varchar(128) NULL DEFAULT NULL COMMENT '规格型号',
  `material_type` tinyint unsigned NOT NULL COMMENT '物料类型:1球头 2羽片 3胶水 4线圈 5包装材料 9其他',
  `unit_id` bigint unsigned NOT NULL COMMENT '计量单位',
  `is_key_material` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否关键物料:1是 0否(关键物料需批次追溯)',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_material_code` (`material_code`) COMMENT '物料编码唯一',
  KEY `idx_material_type` (`material_type`) COMMENT '按类型筛选关键物料'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '物料表';

-- BOM 主表
CREATE TABLE `base_bom` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `bom_code` varchar(32) NOT NULL COMMENT 'BOM编码(唯一)',
  `product_id` bigint unsigned NOT NULL COMMENT '产品',
  `version` varchar(16) NOT NULL COMMENT 'BOM版本(同产品多版本)',
  `bom_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '状态:0草稿 1生效 2停用',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_version` (`product_id`, `version`) COMMENT '同产品同版本唯一',
  UNIQUE KEY `uk_bom_code` (`bom_code`) COMMENT 'BOM编码唯一'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = 'BOM主表';

-- BOM 明细表
CREATE TABLE `base_bom_detail` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `bom_id` bigint unsigned NOT NULL COMMENT 'BOM主表',
  `material_id` bigint unsigned NOT NULL COMMENT '物料',
  `quantity` decimal(12,4) NOT NULL COMMENT '标准用量(单位产品)',
  `loss_rate` decimal(5,2) NOT NULL DEFAULT 0 COMMENT '损耗率(%)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bom_material` (`bom_id`, `material_id`) COMMENT '同BOM物料不重复',
  KEY `idx_material_id` (`material_id`) COMMENT '按物料反查BOM'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = 'BOM明细表';

-- 工单物料需求表
CREATE TABLE `prod_work_order_material` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id` bigint unsigned NOT NULL COMMENT '生产工单',
  `material_id` bigint unsigned NOT NULL COMMENT '物料',
  `require_quantity` decimal(12,4) NOT NULL COMMENT '需求数量(计划数×BOM用量)',
  `issued_quantity` decimal(12,4) NOT NULL DEFAULT 0 COMMENT '已领/已发数量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_material` (`work_order_id`, `material_id`) COMMENT '同工单物料不重复',
  KEY `idx_material_id` (`material_id`) COMMENT '按物料查需求(齐套/追溯)'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '工单物料需求表';

-- 工单状态日志表(schema 新增：状态流转与下达后计划变更的留痕)
CREATE TABLE `prod_work_order_status_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id` bigint unsigned NOT NULL COMMENT '生产工单',
  `from_status` tinyint unsigned NOT NULL COMMENT '变更前状态(计划变更时与 to_status 相同)',
  `to_status` tinyint unsigned NOT NULL COMMENT '变更后状态',
  `change_type` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '变更类型:1状态流转 2计划变更',
  `change_reason` varchar(255) NULL DEFAULT NULL COMMENT '变更原因(暂停/作废/下达后改计划时必填)',
  `operate_by` bigint unsigned NOT NULL COMMENT '操作人',
  `operate_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_id` (`work_order_id`, `operate_time`) COMMENT '按工单查状态轨迹'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '工单状态日志表';

-- 联调种子数据(unit_id 暂以 1 占位，计量单位表建设后修正)
INSERT INTO `base_material` (`material_code`, `material_name`, `spec`, `material_type`, `unit_id`, `is_key_material`, `status`) VALUES
('M001', '天然软木球头', '直径25mm', 1, 1, 1, 1),
('M002', '鹅毛羽片', '一级 左翼', 2, 1, 1, 1),
('M003', '环保胶水', '水性 500ml', 3, 1, 0, 1),
('M004', '纸筒包装', '12只装', 5, 1, 0, 1);

INSERT INTO `base_bom` (`bom_code`, `product_id`, `version`, `bom_status`, `create_by`) VALUES
('BOM-P001-V1', 1, 'V1.0', 1, 1);

INSERT INTO `base_bom_detail` (`bom_id`, `material_id`, `quantity`, `loss_rate`) VALUES
(1, 1, 1.0000, 1.00),
(1, 2, 16.0000, 5.00),
(1, 3, 0.0200, 2.00),
(1, 4, 0.0834, 0.00);
