-- ----------------------------------------------------------------------------
-- V14 ERP 工艺待确认数据表
-- ----------------------------------------------------------------------------

CREATE TABLE `erp_craft_pending` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_system` varchar(32) NOT NULL COMMENT '来源系统',
  `erp_routing_code` varchar(32) NOT NULL COMMENT 'ERP 工艺路线编码',
  `erp_routing_name` varchar(128) NOT NULL COMMENT 'ERP 工艺路线名称',
  `erp_routing_version` varchar(32) NOT NULL COMMENT 'ERP 工艺路线版本',
  `product_code` varchar(32) NOT NULL COMMENT '产品编码',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态:0待确认 1已确认 2异常',
  `process_steps` json NOT NULL COMMENT '工序步骤JSON',
  `confirmed_route_id` bigint unsigned NULL DEFAULT NULL COMMENT '确认后生成的工艺路线ID',
  `confirmed_by` bigint unsigned NULL DEFAULT NULL COMMENT '确认人',
  `confirm_time` datetime NULL DEFAULT NULL COMMENT '确认时间',
  `error_code` varchar(5) NULL DEFAULT NULL COMMENT '异常错误码',
  `error_message` varchar(512) NULL DEFAULT NULL COMMENT '异常原因',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status_id` (`status`, `id`) COMMENT '按状态分页查询',
  UNIQUE KEY `uk_source_code_version`
    (`source_system`, `erp_routing_code`, `erp_routing_version`) COMMENT '来源工艺版本唯一'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'ERP 工艺待确认数据';

-- ----------------------------------------------------------------------------
-- 工序种子数据：供 ERP 工艺同步 Mock 数据引用，使端到端确认流程可验证
-- ----------------------------------------------------------------------------
INSERT INTO `craft_process`
  (`process_code`, `process_name`, `process_type`, `standard_time_seconds`,
   `is_key_process`, `is_quality_required`, `is_scan_required`, `is_piece_rate_enabled`,
   `status`, `create_by`, `update_by`)
SELECT 'PR001', '羽毛分拣', 'PREP', 30, 0, 1, 1, 1, 1, 1, 1
WHERE NOT EXISTS (
  SELECT 1 FROM `craft_process` WHERE `process_code` = 'PR001'
);

INSERT INTO `craft_process`
  (`process_code`, `process_name`, `process_type`, `standard_time_seconds`,
   `is_key_process`, `is_quality_required`, `is_scan_required`, `is_piece_rate_enabled`,
   `status`, `create_by`, `update_by`)
SELECT 'PR002', '插毛成型', 'FORM', 60, 1, 1, 1, 1, 1, 1, 1
WHERE NOT EXISTS (
  SELECT 1 FROM `craft_process` WHERE `process_code` = 'PR002'
);

INSERT INTO `craft_process`
  (`process_code`, `process_name`, `process_type`, `standard_time_seconds`,
   `is_key_process`, `is_quality_required`, `is_scan_required`, `is_piece_rate_enabled`,
   `status`, `create_by`, `update_by`)
SELECT 'PR003', '注胶固定', 'BOND', 45, 0, 0, 1, 1, 1, 1, 1
WHERE NOT EXISTS (
  SELECT 1 FROM `craft_process` WHERE `process_code` = 'PR003'
);

INSERT INTO `craft_process`
  (`process_code`, `process_name`, `process_type`, `standard_time_seconds`,
   `is_key_process`, `is_quality_required`, `is_scan_required`, `is_piece_rate_enabled`,
   `status`, `create_by`, `update_by`)
SELECT 'PR004', '质量检验', 'QC', 20, 1, 1, 0, 0, 1, 1, 1
WHERE NOT EXISTS (
  SELECT 1 FROM `craft_process` WHERE `process_code` = 'PR004'
);

INSERT INTO `craft_process`
  (`process_code`, `process_name`, `process_type`, `standard_time_seconds`,
   `is_key_process`, `is_quality_required`, `is_scan_required`, `is_piece_rate_enabled`,
   `status`, `create_by`, `update_by`)
SELECT 'PR005', '包装入库', 'PACK', 15, 0, 0, 0, 1, 1, 1, 1
WHERE NOT EXISTS (
  SELECT 1 FROM `craft_process` WHERE `process_code` = 'PR005'
);
