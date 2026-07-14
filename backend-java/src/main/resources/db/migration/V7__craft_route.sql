-- ----------------------------------------------------------------------------
-- V7 工艺路线：路线主档、产品绑定、明细控制字段和变更审计
--
-- 不建立物理外键，关联存在性、状态与生命周期约束由 Service 层校验。
-- ----------------------------------------------------------------------------

CREATE TABLE `base_workstation` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `station_code` varchar(32) NOT NULL COMMENT '工位编码',
  `station_name` varchar(64) NOT NULL COMMENT '工位名称',
  `line_id` bigint unsigned NOT NULL COMMENT '所属产线ID',
  `seq` int unsigned NOT NULL DEFAULT 0 COMMENT '工位顺序',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除:1删除 0未删除',
  `active_station_code` varchar(32) GENERATED ALWAYS AS (
    CASE WHEN `is_deleted` = 0 THEN `station_code` ELSE NULL END
  ) STORED COMMENT '未删除工位编码(唯一索引辅助列)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_active_station_code` (`active_station_code`) COMMENT '未删除工位编码唯一',
  KEY `idx_line_status` (`line_id`, `status`) COMMENT '按产线查询可用工位'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '工位表';

CREATE TABLE `craft_route` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `routing_code` varchar(32) NOT NULL COMMENT '路线编码',
  `routing_name` varchar(128) NOT NULL COMMENT '路线名称',
  `routing_version` varchar(32) NOT NULL COMMENT '业务版本',
  `previous_route_id` bigint unsigned NULL DEFAULT NULL COMMENT '上一版本路线ID',
  `source_type` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '来源:1本地创建 2ERP读取确认',
  `routing_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '状态:0草稿 1生效 2停用',
  `audit_by` bigint unsigned NULL DEFAULT NULL COMMENT '审核人',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `version` int unsigned NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `update_by` bigint unsigned NOT NULL COMMENT '最后修改人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除:1删除 0未删除',
  `active_routing_code` varchar(32) GENERATED ALWAYS AS (
    CASE WHEN `is_deleted` = 0 THEN `routing_code` ELSE NULL END
  ) STORED COMMENT '未删除路线编码(唯一索引辅助列)',
  `active_routing_version` varchar(32) GENERATED ALWAYS AS (
    CASE WHEN `is_deleted` = 0 THEN `routing_version` ELSE NULL END
  ) STORED COMMENT '未删除路线版本(唯一索引辅助列)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_active_code_version` (`active_routing_code`, `active_routing_version`)
    COMMENT '未删除路线编码与版本唯一',
  KEY `idx_route_page` (`is_deleted`, `routing_code`, `id` DESC) COMMENT '默认路线分页与编码前缀查询',
  KEY `idx_route_status_source` (`is_deleted`, `routing_status`, `source_type`, `id` DESC)
    COMMENT '按状态与来源分页查询',
  KEY `idx_previous_route_id` (`previous_route_id`) COMMENT '按上一版本追溯版本链'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '工艺路线主表';

ALTER TABLE `craft_route_detail`
  ADD COLUMN `station_id` bigint unsigned NULL DEFAULT NULL COMMENT '默认工位ID' AFTER `process_id`,
  ADD COLUMN `equipment_category_id` bigint unsigned NULL DEFAULT NULL COMMENT '设备类别要求' AFTER `station_id`,
  ADD COLUMN `is_inspect` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否质检节点:1是 0否' AFTER `equipment_category_id`,
  ADD COLUMN `sop_id` bigint unsigned NULL DEFAULT NULL COMMENT '绑定工序SOP关联ID' AFTER `is_inspect`,
  ADD COLUMN `quality_plan_id` bigint unsigned NULL DEFAULT NULL COMMENT '检验方案ID' AFTER `sop_id`,
  ADD COLUMN `version` int unsigned NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER `quality_plan_id`,
  ADD COLUMN `create_by` bigint unsigned NOT NULL DEFAULT 0 COMMENT '创建人' AFTER `version`,
  ADD COLUMN `update_by` bigint unsigned NOT NULL DEFAULT 0 COMMENT '最后修改人' AFTER `create_by`,
  ADD KEY `idx_station_id` (`station_id`) COMMENT '按工位查询路线明细',
  ADD KEY `idx_equipment_category_id` (`equipment_category_id`) COMMENT '按设备类别查询路线明细',
  ADD KEY `idx_sop_id` (`sop_id`) COMMENT '按SOP检查路线引用',
  ADD KEY `idx_quality_plan_id` (`quality_plan_id`) COMMENT '按检验方案检查路线引用';

-- V6 仅提供了没有父路线主档的占位明细表。若升级库中有人提前写入数据，
-- 无法可靠重建路线主档，统一保留记录并逻辑归档，避免孤儿明细继续阻塞工序维护。
UPDATE `craft_route_detail`
SET `is_deleted` = 1,
    `update_by` = 0
WHERE `is_deleted` = 0;

-- 旧数据回填完成后移除系统迁移默认值，后续业务写入必须提供真实操作人。
ALTER TABLE `craft_route_detail`
  MODIFY COLUMN `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  MODIFY COLUMN `update_by` bigint unsigned NOT NULL COMMENT '最后修改人';

-- 支撑删除路线前的工单引用校验，避免增长型工单表全表扫描。
ALTER TABLE `prod_work_order`
  ADD KEY `idx_routing_deleted` (`routing_id`, `is_deleted`) COMMENT '按工艺路线检查工单引用';

CREATE TABLE `craft_route_product` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `route_id` bigint unsigned NOT NULL COMMENT '工艺路线ID',
  `product_id` bigint unsigned NOT NULL COMMENT '产品ID',
  `is_default` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否产品默认生效路线:1是 0否',
  `version` int unsigned NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `update_by` bigint unsigned NOT NULL COMMENT '最后修改人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除:1删除 0未删除',
  `active_product_id` bigint unsigned GENERATED ALWAYS AS (
    CASE WHEN `is_deleted` = 0 THEN `product_id` ELSE NULL END
  ) STORED COMMENT '未删除产品ID(路线内唯一辅助列)',
  `default_product_id` bigint unsigned GENERATED ALWAYS AS (
    CASE WHEN `is_deleted` = 0 AND `is_default` = 1 THEN `product_id` ELSE NULL END
  ) STORED COMMENT '默认路线产品ID(全局唯一辅助列)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_route_active_product` (`route_id`, `active_product_id`) COMMENT '路线内未删除产品唯一',
  UNIQUE KEY `uk_default_product` (`default_product_id`) COMMENT '每个产品仅一条默认路线',
  KEY `idx_product_route` (`product_id`, `route_id`) COMMENT '按产品查询可用路线'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '工艺路线产品关系表';

CREATE TABLE `craft_route_change_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `route_id` bigint unsigned NOT NULL COMMENT '工艺路线ID',
  `change_type` tinyint unsigned NOT NULL COMMENT '变更类型:1创建 2修改 3审核生效 4停用 5删除 6创建新版本',
  `before_snapshot` text NULL COMMENT '变更前快照(JSON)',
  `after_snapshot` text NULL COMMENT '变更后快照(JSON)',
  `change_reason` varchar(255) NULL DEFAULT NULL COMMENT '变更原因',
  `operator_id` bigint unsigned NOT NULL COMMENT '操作人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除:1删除 0未删除',
  PRIMARY KEY (`id`),
  KEY `idx_route_deleted_id` (`route_id`, `is_deleted`, `id`) COMMENT '按路线分页追溯变更'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '工艺路线变更日志表';

INSERT INTO `base_workstation` (`station_code`, `station_name`, `line_id`, `seq`, `status`) VALUES
('LINE01-S01', '一号线准备工位', 1, 1, 1),
('LINE01-S02', '一号线植毛工位', 1, 2, 1),
('LINE01-S03', '一号线质检工位', 1, 3, 1);
