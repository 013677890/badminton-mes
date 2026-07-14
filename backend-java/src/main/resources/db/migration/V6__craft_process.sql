-- ----------------------------------------------------------------------------
-- V6 工艺管理：工序档案、规则、子资源、路线明细基础与质量方案最小主档
--
-- 工艺路线在后续迁移中建设；本迁移提供工序规则、SOP 和不良原因关联。
-- 不建物理外键，关联一致性由 Service 层校验。
-- ----------------------------------------------------------------------------

CREATE TABLE `craft_process` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `process_code` varchar(32) NOT NULL COMMENT '工序编码(唯一)',
  `process_name` varchar(64) NOT NULL COMMENT '工序名称',
  `process_type` varchar(32) NOT NULL COMMENT '工序类型编码',
  `standard_time_seconds` int unsigned NOT NULL COMMENT '标准工时(秒)',
  `is_key_process` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否关键工序:1是 0否',
  `is_quality_required` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否需要质检:1是 0否',
  `is_scan_required` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否需要扫码:1是 0否',
  `is_piece_rate_enabled` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否参与计件:1是 0否',
  `equipment_category_id` bigint unsigned NULL DEFAULT NULL COMMENT '适用设备类别',
  `quality_plan_id` bigint unsigned NULL DEFAULT NULL COMMENT '检验方案(质量模块建设后校验)',
  `remark` varchar(255) NULL DEFAULT NULL COMMENT '备注',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  `version` int unsigned NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `update_by` bigint unsigned NOT NULL COMMENT '最后修改人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除:1删除 0未删除',
  `active_process_code` varchar(32) GENERATED ALWAYS AS (
    CASE WHEN `is_deleted` = 0 THEN `process_code` ELSE NULL END
  ) STORED COMMENT '未删除工序编码(唯一索引辅助列)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_active_process_code` (`active_process_code`) COMMENT '未删除工序编码唯一',
  KEY `idx_process_type_status` (`process_type`, `status`) COMMENT '按类型和状态查工序',
  KEY `idx_equipment_category_id` (`equipment_category_id`) COMMENT '按设备类别查工序'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '工序档案表';

CREATE TABLE `craft_process_change_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `process_id` bigint unsigned NOT NULL COMMENT '工序ID',
  `change_type` tinyint unsigned NOT NULL COMMENT '变更类型:1创建 2修改 3状态变更 4删除 5SOP变更 6不良原因变更',
  `before_snapshot` text NULL COMMENT '变更前快照(JSON)',
  `after_snapshot` text NULL COMMENT '变更后快照(JSON)',
  `change_reason` varchar(255) NULL DEFAULT NULL COMMENT '变更原因',
  `operator_id` bigint unsigned NOT NULL COMMENT '操作人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除:1删除 0未删除',
  PRIMARY KEY (`id`),
  KEY `idx_process_deleted_id` (`process_id`, `is_deleted`, `id`) COMMENT '按工序分页追溯变更'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '工序变更日志表';

CREATE TABLE `craft_process_sop` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `process_id` bigint unsigned NOT NULL COMMENT '工序ID',
  `sop_code` varchar(32) NOT NULL COMMENT 'SOP编码',
  `sop_name` varchar(64) NOT NULL COMMENT 'SOP名称',
  `sop_version` varchar(32) NOT NULL COMMENT 'SOP版本',
  `file_url` varchar(512) NOT NULL COMMENT 'SOP文件地址',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  `version` int unsigned NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `update_by` bigint unsigned NOT NULL COMMENT '最后修改人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除:1删除 0未删除',
  `active_sop_code` varchar(32) GENERATED ALWAYS AS (
    CASE WHEN `is_deleted` = 0 THEN `sop_code` ELSE NULL END
  ) STORED COMMENT '未删除SOP编码(唯一索引辅助列)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_process_active_sop_code` (`process_id`, `active_sop_code`) COMMENT '同工序未删除SOP编码唯一',
  KEY `idx_process_status` (`process_id`, `status`) COMMENT '按工序查可用SOP'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '工序SOP关联表';

CREATE TABLE `craft_process_defect_reason` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `process_id` bigint unsigned NOT NULL COMMENT '工序ID',
  `reason_code` varchar(32) NOT NULL COMMENT '不良原因编码',
  `reason_name` varchar(64) NOT NULL COMMENT '不良原因名称',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  `version` int unsigned NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `update_by` bigint unsigned NOT NULL COMMENT '最后修改人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除:1删除 0未删除',
  `active_reason_code` varchar(32) GENERATED ALWAYS AS (
    CASE WHEN `is_deleted` = 0 THEN `reason_code` ELSE NULL END
  ) STORED COMMENT '未删除不良原因编码(唯一索引辅助列)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_process_active_reason_code` (`process_id`, `active_reason_code`) COMMENT '同工序未删除原因编码唯一',
  KEY `idx_process_status` (`process_id`, `status`) COMMENT '按工序查可用不良原因'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '工序不良原因表';

-- 路线模块的基础明细表：当前先支撑工序删除引用校验，后续迁移可追加控制字段。
CREATE TABLE `craft_route_detail` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `route_id` bigint unsigned NOT NULL COMMENT '工艺路线ID',
  `process_id` bigint unsigned NOT NULL COMMENT '工序ID',
  `sequence_no` int unsigned NOT NULL COMMENT '工序顺序号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除:1删除 0未删除',
  `active_sequence_no` int unsigned GENERATED ALWAYS AS (
    CASE WHEN `is_deleted` = 0 THEN `sequence_no` ELSE NULL END
  ) STORED COMMENT '未删除路线顺序(唯一索引辅助列)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_route_active_sequence` (`route_id`, `active_sequence_no`) COMMENT '同一路线未删除顺序唯一',
  KEY `idx_process_id` (`process_id`) COMMENT '按工序检查路线引用'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '工艺路线明细基础表';

-- 质量模块最小检验方案主档：当前用于工序关联完整性校验，后续迁移可追加适用范围和审核字段。
CREATE TABLE `quality_inspection_plan` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `plan_code` varchar(32) NOT NULL COMMENT '检验方案编码',
  `plan_name` varchar(64) NOT NULL COMMENT '检验方案名称',
  `plan_version` varchar(32) NOT NULL COMMENT '方案版本',
  `status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '状态:1启用 0停用',
  `version` int unsigned NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `update_by` bigint unsigned NOT NULL COMMENT '最后修改人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除:1删除 0未删除',
  `active_plan_code` varchar(32) GENERATED ALWAYS AS (
    CASE WHEN `is_deleted` = 0 THEN `plan_code` ELSE NULL END
  ) STORED COMMENT '未删除方案编码(唯一索引辅助列)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_active_plan_code` (`active_plan_code`) COMMENT '未删除检验方案编码唯一',
  KEY `idx_status_deleted` (`status`, `is_deleted`) COMMENT '按状态查询可用检验方案'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '质量检验方案主表';

-- 工艺工程师角色：已存在、停用或逻辑删除时恢复为可用状态。
INSERT INTO `sys_role` (`role_code`, `role_name`, `remark`, `status`, `is_deleted`)
VALUES ('CRAFT_ENGINEER', '工艺工程师', '工序、工艺路线与SOP维护', 1, 0)
ON DUPLICATE KEY UPDATE
  `role_name` = VALUES(`role_name`),
  `remark` = VALUES(`remark`),
  `status` = 1,
  `is_deleted` = 0;
