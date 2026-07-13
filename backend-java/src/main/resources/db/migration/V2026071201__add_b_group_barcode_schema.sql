-- ----------------------------------------------------------------------------
-- V2026071201 B 组 M1 条码应用：条码类型/规则/流水/模板/应用规则/条码/打印/使用记录
--
-- 表结构照抄 wiki/database/mes_schema.sql 条码区段，并落实
-- wiki/database/changes/2026-07-11-B组M0契约差异与迁移协调.md 已登记修正：
--   1. barcode_serial 唯一索引使用完整列 (rule_id, serial_scope)；
--   2. barcode_apply_rule 以生成列 active_default_object_id 建默认规则唯一约束；
--   3. barcode_print_record 逐次插入模型，新增模板版本与预览快照及 (barcode_id, print_count) 唯一键；
--   4. 新增扫码使用记录表 barcode_use_record。
-- 差异登记见 wiki/database/changes/2026-07-12-B组M1条码结构迁移.md。
-- 业务关系由 Service 校验与应用层约束处理，不建实体外键；
-- 唯一约束和查询索引仍在数据库层兜底。不写种子与联调数据。
-- ----------------------------------------------------------------------------

-- 条码类型表
CREATE TABLE `barcode_type` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type_code` varchar(32) NOT NULL COMMENT '类型编码(唯一)',
  `type_name` varchar(64) NOT NULL COMMENT '类型名称:产品码/内外箱码/中箱码/栈板码/材料码',
  `apply_object` varchar(64) NULL DEFAULT NULL COMMENT '适用对象说明',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用(停用后不可新建应用规则)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_code` (`type_code`) COMMENT '类型编码唯一'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '条码类型表';

-- 条码规则表
CREATE TABLE `barcode_rule` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rule_code` varchar(32) NOT NULL COMMENT '规则编码(唯一)',
  `rule_name` varchar(64) NOT NULL COMMENT '规则名称',
  `barcode_type_id` bigint unsigned NOT NULL COMMENT '适用条码类型',
  `serial_length` tinyint unsigned NOT NULL DEFAULT 4 COMMENT '流水号位数',
  `serial_reset_cycle` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '流水号重置周期:1按日 2按月 3不重置',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_code` (`rule_code`) COMMENT '规则编码唯一',
  KEY `idx_barcode_type_id` (`barcode_type_id`) COMMENT '按类型查规则(停用校验)'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '条码规则表';

-- 条码规则组成明细表
CREATE TABLE `barcode_rule_item` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rule_id` bigint unsigned NOT NULL COMMENT '条码规则',
  `seq` tinyint unsigned NOT NULL COMMENT '组成顺序',
  `item_type` tinyint unsigned NOT NULL COMMENT '组成类型:1常量 2日期 3变量(产品编码/产线编码) 4流水号',
  `item_value` varchar(64) NULL DEFAULT NULL COMMENT '常量值或变量名',
  `date_format` varchar(16) NULL DEFAULT NULL COMMENT '日期格式(yyyyMMdd)',
  `item_length` tinyint unsigned NULL DEFAULT NULL COMMENT '该段长度',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_seq` (`rule_id`, `seq`) COMMENT '同规则组成顺序唯一'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '条码规则组成明细表';

-- 条码流水号记录表(与基线差异:唯一索引使用完整列，不取 serial_scope 前 32 字符前缀)
CREATE TABLE `barcode_serial` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rule_id` bigint unsigned NOT NULL COMMENT '条码规则',
  `serial_scope` varchar(64) NOT NULL COMMENT '流水维度值(日期+产品等组合;与规则联合唯一)',
  `current_serial` int unsigned NOT NULL DEFAULT 0 COMMENT '当前流水号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_scope` (`rule_id`, `serial_scope`) COMMENT '流水维度唯一(并发取号加行锁依据)'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '条码流水号记录表';

-- 条码模板表
CREATE TABLE `barcode_template` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_code` varchar(32) NOT NULL COMMENT '模板编码(唯一)',
  `template_name` varchar(64) NOT NULL COMMENT '模板名称',
  `paper_width` decimal(6,2) NOT NULL COMMENT '纸张宽度(mm)',
  `paper_height` decimal(6,2) NOT NULL COMMENT '纸张高度(mm)',
  `version` varchar(16) NOT NULL DEFAULT 'V1' COMMENT '模板版本(被绑定后修改需升版本)',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code_version` (`template_code`, `version`) COMMENT '模板编码+版本唯一'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '条码模板表';

-- 模板字段表
CREATE TABLE `barcode_template_field` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_id` bigint unsigned NOT NULL COMMENT '条码模板',
  `field_name` varchar(64) NOT NULL COMMENT '字段名称(条码值/产品名称/批次号/生产日期/工单号)',
  `field_type` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '字段类型:1文本 2条码 3二维码',
  `data_source` varchar(64) NOT NULL COMMENT '数据来源字段',
  `pos_x` decimal(6,2) NOT NULL DEFAULT 0 COMMENT 'X位置(mm)',
  `pos_y` decimal(6,2) NOT NULL DEFAULT 0 COMMENT 'Y位置(mm)',
  `font_size` tinyint unsigned NULL DEFAULT NULL COMMENT '字体大小',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_template_id` (`template_id`) COMMENT '按模板查字段'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '模板字段表';

-- 条码应用规则表(与基线差异:以生成列唯一索引保证"同对象同类型仅一条启用默认规则"，
-- 基线 uk_product_type_default 每行必含 NULL 列、在 MySQL 下从不生效，故不再创建)
CREATE TABLE `barcode_apply_rule` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `object_type` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '对象类型:1产品 2物料',
  `product_id` bigint unsigned NULL DEFAULT NULL COMMENT '适用产品(对象类型=1)',
  `material_id` bigint unsigned NULL DEFAULT NULL COMMENT '适用物料(对象类型=2)',
  `barcode_type_id` bigint unsigned NOT NULL COMMENT '条码类型',
  `barcode_mode` tinyint unsigned NOT NULL DEFAULT 2 COMMENT '条码模式:1唯一码 2批次码',
  `rule_id` bigint unsigned NULL DEFAULT NULL COMMENT '条码规则(来源=规则生成时必填)',
  `template_id` bigint unsigned NOT NULL COMMENT '标签模板',
  `source_type` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '条码来源:1规则生成 2传入值生成 3外部导入',
  `is_default` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '是否默认规则(同产品同类型仅一条默认启用)',
  `version` varchar(16) NOT NULL DEFAULT 'V1' COMMENT '规则版本',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  `active_default_object_id` bigint unsigned GENERATED ALWAYS AS (
    CASE WHEN `status` = 1 AND `is_default` = 1 AND `is_deleted` = 0
         THEN COALESCE(`product_id`, `material_id`) END
  ) STORED COMMENT '启用默认规则的对象id(非启用默认为NULL，唯一索引依据)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_active_default` (`object_type`, `active_default_object_id`, `barcode_type_id`) COMMENT '同对象同类型仅一条启用默认规则',
  KEY `idx_barcode_type_id` (`barcode_type_id`) COMMENT '类型停用联查',
  KEY `idx_rule_id` (`rule_id`) COMMENT '规则停用联查',
  KEY `idx_template_id` (`template_id`) COMMENT '模板停用联查'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '条码应用规则表';

-- 条码主表
CREATE TABLE `barcode` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `barcode_value` varchar(64) NOT NULL COMMENT '条码值(全局唯一)',
  `barcode_type_id` bigint unsigned NOT NULL COMMENT '条码类型',
  `barcode_mode` tinyint unsigned NOT NULL DEFAULT 2 COMMENT '条码模式:1唯一码 2批次码',
  `apply_rule_id` bigint unsigned NULL DEFAULT NULL COMMENT '来源应用规则',
  `product_id` bigint unsigned NULL DEFAULT NULL COMMENT '产品',
  `material_id` bigint unsigned NULL DEFAULT NULL COMMENT '物料(材料码)',
  `batch_no` varchar(64) NULL DEFAULT NULL COMMENT '批次号',
  `work_order_id` bigint unsigned NULL DEFAULT NULL COMMENT '关联生产工单',
  `task_id` bigint unsigned NULL DEFAULT NULL COMMENT '关联生产任务单',
  `source_type` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '来源:1规则生成 2传入值 3外部导入',
  `barcode_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '状态:0未使用 1已使用 2已作废(已使用不可作废)',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_barcode_value` (`barcode_value`) COMMENT '条码值全局唯一(扫码入口:const级查询INDEX-008)',
  KEY `idx_batch_no` (`batch_no`(20)) COMMENT '按批次查条码',
  KEY `idx_task_id` (`task_id`) COMMENT '按任务查已生成条码',
  KEY `idx_type_status` (`barcode_type_id`, `barcode_status`) COMMENT '作废/未使用条码管理'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '条码主表';

-- 条码打印记录表(与基线差异:逐次插入模型，print_count 为同一条码打印序号；
-- 新增模板版本与预览快照；(barcode_id, print_count) 唯一键最左前缀覆盖按条码查询)
CREATE TABLE `barcode_print_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `barcode_id` bigint unsigned NOT NULL COMMENT '条码',
  `template_id` bigint unsigned NOT NULL COMMENT '打印模板',
  `template_version` varchar(16) NULL DEFAULT NULL COMMENT '打印时模板版本快照',
  `preview_content` json NULL DEFAULT NULL COMMENT '打印时预览内容快照',
  `print_by` bigint unsigned NOT NULL COMMENT '打印人',
  `print_count` int unsigned NOT NULL DEFAULT 1 COMMENT '同一条码的打印序号(从1递增)',
  `reprint_reason` varchar(255) NULL DEFAULT NULL COMMENT '重复打印原因',
  `print_time` datetime NOT NULL COMMENT '打印时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_barcode_print_seq` (`barcode_id`, `print_count`) COMMENT '同条码打印序号唯一(并发重打兜底)'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '条码打印记录表';

-- 条码使用记录表(新增，最小契约见 2026-07-11 §3.4；task_id/process_id 为逻辑引用，
-- 任务表由 M2 迁移、工艺表由 A/公共迁移落地)
CREATE TABLE `barcode_use_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `barcode_id` bigint unsigned NOT NULL COMMENT '条码',
  `task_id` bigint unsigned NOT NULL COMMENT '生产任务单',
  `process_id` bigint unsigned NOT NULL COMMENT '工序',
  `user_id` bigint unsigned NOT NULL COMMENT '扫码人员',
  `equipment_id` bigint unsigned NULL DEFAULT NULL COMMENT '设备(可空)',
  `use_type` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '使用类型:1工序开工扫码 2工序完工扫码 3报工扫码 4其他',
  `business_time` datetime NOT NULL COMMENT '业务发生时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_barcode_business_time` (`barcode_id`, `business_time`) COMMENT '按条码查使用轨迹',
  KEY `idx_task_process` (`task_id`, `process_id`) COMMENT '按任务工序查扫码',
  KEY `idx_user_business_time` (`user_id`, `business_time`) COMMENT '按人员查扫码'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '条码使用记录表';
