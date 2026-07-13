-- =====================================================================
-- 羽毛球 MES 数据库建表脚本(由 mes_table_design.csv / mes_index_design.csv 生成)
-- MySQL 8.0 / InnoDB / utf8mb4
-- 规范:《Java开发手册(黄山版)》建表规约;表间业务关系由应用层校验
-- =====================================================================
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `badminton_mes` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `badminton_mes`;


-- ============================== 系统管理 ==============================

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_no` varchar(32) NOT NULL COMMENT '工号(唯一)',
  `user_name` varchar(64) NOT NULL COMMENT '姓名',
  `password` varchar(128) NOT NULL COMMENT '密码(加密存储)',
  `mobile` varchar(20) NULL DEFAULT NULL COMMENT '手机号',
  `workshop_id` bigint unsigned NULL DEFAULT NULL COMMENT '所属车间',
  `line_id` bigint unsigned NULL DEFAULT NULL COMMENT '所属产线(操作工/班组长)',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除:1删除 0未删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_no` (`user_no`) COMMENT '工号唯一',
  KEY `idx_workshop_id` (`workshop_id`) COMMENT '按车间查人员',
  KEY `idx_line_id` (`line_id`) COMMENT '按产线查班组/操作工'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户表';

DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_code` varchar(32) NOT NULL COMMENT '角色编码(唯一)',
  `role_name` varchar(64) NOT NULL COMMENT '角色名称(管理员/PMC/车间主管/班组长/操作工/质检员等)',
  `remark` varchar(255) NULL DEFAULT NULL COMMENT '备注',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`) COMMENT '角色编码唯一'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '系统角色表';

DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `role_id` bigint unsigned NOT NULL COMMENT '角色ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`) COMMENT '防重复授权(INDEX-001)',
  KEY `idx_role_id` (`role_id`) COMMENT '按角色反查用户(安灯按角色匹配处理人)'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '用户角色关系表';


-- ============================== 基础资料 ==============================

DROP TABLE IF EXISTS `base_workshop`;
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

DROP TABLE IF EXISTS `base_production_line`;
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

DROP TABLE IF EXISTS `base_workstation`;
CREATE TABLE `base_workstation` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `station_code` varchar(32) NOT NULL COMMENT '工位编码(唯一)',
  `station_name` varchar(64) NOT NULL COMMENT '工位名称',
  `line_id` bigint unsigned NOT NULL COMMENT '所属产线',
  `seq` int unsigned NOT NULL DEFAULT 0 COMMENT '工位顺序',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_station_code` (`station_code`) COMMENT '工位编码唯一',
  KEY `idx_line_id` (`line_id`) COMMENT '产线下工位列表'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '工位表';

DROP TABLE IF EXISTS `base_shift`;
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

DROP TABLE IF EXISTS `base_factory_calendar`;
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

DROP TABLE IF EXISTS `base_unit`;
CREATE TABLE `base_unit` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `unit_code` varchar(32) NOT NULL COMMENT '单位编码(唯一;支持API写入)',
  `unit_name` varchar(32) NOT NULL COMMENT '单位名称(只/打/箱/克)',
  `unit_precision` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '小数精度',
  `source_type` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '来源:1手工 2API写入',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_unit_code` (`unit_code`) COMMENT '单位编码唯一(API写入防重)'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '计量单位表';

DROP TABLE IF EXISTS `base_customer`;
CREATE TABLE `base_customer` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `customer_code` varchar(32) NOT NULL COMMENT '客户编码(唯一)',
  `customer_name` varchar(128) NOT NULL COMMENT '客户名称',
  `contact` varchar(64) NULL DEFAULT NULL COMMENT '联系人',
  `phone` varchar(20) NULL DEFAULT NULL COMMENT '联系电话',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_customer_code` (`customer_code`) COMMENT '客户编码唯一'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '客户表';

DROP TABLE IF EXISTS `base_product`;
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

DROP TABLE IF EXISTS `base_material`;
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

DROP TABLE IF EXISTS `base_bom`;
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

DROP TABLE IF EXISTS `base_bom_detail`;
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


-- ============================== 生产订单 ==============================

DROP TABLE IF EXISTS `prod_work_order`;
CREATE TABLE `prod_work_order` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_no` varchar(32) NOT NULL COMMENT '工单号(唯一;日期+车间+流水)',
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

DROP TABLE IF EXISTS `prod_work_order_material`;
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

DROP TABLE IF EXISTS `material_stock`;
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

DROP TABLE IF EXISTS `kit_analysis`;
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

DROP TABLE IF EXISTS `dispatch_order`;
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


-- ============================== 条码应用 ==============================

DROP TABLE IF EXISTS `barcode_type`;
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

DROP TABLE IF EXISTS `barcode_rule`;
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

DROP TABLE IF EXISTS `barcode_rule_item`;
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

DROP TABLE IF EXISTS `barcode_serial`;
CREATE TABLE `barcode_serial` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rule_id` bigint unsigned NOT NULL COMMENT '条码规则',
  `serial_scope` varchar(64) NOT NULL COMMENT '流水维度值(日期+产品等组合;与规则联合唯一)',
  `current_serial` int unsigned NOT NULL DEFAULT 0 COMMENT '当前流水号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_scope` (`rule_id`, `serial_scope`(32)) COMMENT '流水维度唯一(并发取号加行锁依据)'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '条码流水号记录表';

DROP TABLE IF EXISTS `barcode_template`;
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

DROP TABLE IF EXISTS `barcode_template_field`;
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

DROP TABLE IF EXISTS `barcode_apply_rule`;
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
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_type_default` (`object_type`, `product_id`, `material_id`, `barcode_type_id`, `is_default`) COMMENT '同对象同类型仅一条默认规则',
  KEY `idx_barcode_type_id` (`barcode_type_id`) COMMENT '类型停用联查',
  KEY `idx_rule_id` (`rule_id`) COMMENT '规则停用联查',
  KEY `idx_template_id` (`template_id`) COMMENT '模板停用联查'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '条码应用规则表';

DROP TABLE IF EXISTS `barcode`;
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

DROP TABLE IF EXISTS `barcode_print_record`;
CREATE TABLE `barcode_print_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `barcode_id` bigint unsigned NOT NULL COMMENT '条码',
  `template_id` bigint unsigned NOT NULL COMMENT '打印模板',
  `print_by` bigint unsigned NOT NULL COMMENT '打印人',
  `print_count` int unsigned NOT NULL DEFAULT 1 COMMENT '累计打印次数',
  `reprint_reason` varchar(255) NULL DEFAULT NULL COMMENT '重复打印原因',
  `print_time` datetime NOT NULL COMMENT '打印时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_barcode_id` (`barcode_id`) COMMENT '按条码查打印历史'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '条码打印记录表';


-- ============================== 现场管理 ==============================

DROP TABLE IF EXISTS `prod_param`;
CREATE TABLE `prod_param` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `param_code` varchar(64) NOT NULL COMMENT '参数编码(唯一;如allow_over_produce/must_scan_report/enable_first_check/allow_skip_process/enable_andon_link)',
  `param_name` varchar(128) NOT NULL COMMENT '参数名称',
  `param_value` varchar(255) NOT NULL COMMENT '参数值',
  `value_type` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '值类型:1开关 2数量 3枚举 4文本(校验用)',
  `workshop_id` bigint unsigned NULL DEFAULT NULL COMMENT '适用车间(NULL为全局)',
  `line_id` bigint unsigned NULL DEFAULT NULL COMMENT '适用产线(NULL为全车间)',
  `remark` varchar(255) NULL DEFAULT NULL COMMENT '参数说明',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用(停用回默认规则)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code_scope` (`param_code`, `workshop_id`, `line_id`) COMMENT '同参数同范围唯一'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '生产参数配置表';

DROP TABLE IF EXISTS `prod_task`;
CREATE TABLE `prod_task` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_no` varchar(32) NOT NULL COMMENT '任务单号(唯一)',
  `source_type` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '来源:1派工单下发 2手工录入 3导入 4API写入',
  `source_task_no` varchar(64) NULL DEFAULT NULL COMMENT '外部任务号(API写入时唯一)',
  `dispatch_order_id` bigint unsigned NULL DEFAULT NULL COMMENT '来源派工单',
  `work_order_id` bigint unsigned NOT NULL COMMENT '来源生产工单',
  `product_id` bigint unsigned NOT NULL COMMENT '产品',
  `product_name` varchar(128) NOT NULL COMMENT '产品名称(冗余)',
  `batch_no` varchar(64) NOT NULL COMMENT '生产批次号(追溯主线)',
  `routing_id` bigint unsigned NOT NULL COMMENT '工艺路线版本',
  `line_id` bigint unsigned NOT NULL COMMENT '产线',
  `shift_id` bigint unsigned NOT NULL COMMENT '班次',
  `plan_date` date NOT NULL COMMENT '计划生产日期',
  `plan_quantity` int unsigned NOT NULL COMMENT '计划数量',
  `input_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '投入数量',
  `good_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '良品数量',
  `defect_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '不良数量',
  `rework_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '返修数量',
  `finish_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '完工数量',
  `plan_start_time` datetime NOT NULL COMMENT '计划开始时间',
  `plan_end_time` datetime NOT NULL COMMENT '计划结束时间',
  `actual_start_time` datetime NULL DEFAULT NULL COMMENT '实际开工时间',
  `actual_end_time` datetime NULL DEFAULT NULL COMMENT '实际结束时间',
  `task_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '任务状态:0待审核 1已审核 2已下发 3生产中 4暂停 5已完工 6已关闭 7已取消',
  `pause_reason` varchar(255) NULL DEFAULT NULL COMMENT '暂停/取消/关闭原因',
  `audit_by` bigint unsigned NULL DEFAULT NULL COMMENT '审核人',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_no` (`task_no`) COMMENT '任务单号唯一',
  UNIQUE KEY `uk_source_task` (`source_type`, `source_task_no`(32)) COMMENT '外部任务号防重(API写入)',
  KEY `idx_work_order_id` (`work_order_id`) COMMENT '按工单查任务(汇总进度)',
  KEY `idx_line_date_status` (`line_id`, `plan_date`, `task_status`) COMMENT '平板端按产线+日期筛任务(高频)',
  KEY `idx_batch_no` (`batch_no`(20)) COMMENT '批次追溯查任务',
  KEY `idx_status` (`task_status`, `plan_date`) COMMENT '看板/实时信息查在制任务'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '生产任务单主表';

DROP TABLE IF EXISTS `prod_task_operate_log`;
CREATE TABLE `prod_task_operate_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` bigint unsigned NOT NULL COMMENT '生产任务单',
  `operate_type` tinyint unsigned NOT NULL COMMENT '操作类型:1开工 2暂停 3恢复 4结束 5审核 6下发 7关闭',
  `reason` varchar(255) NULL DEFAULT NULL COMMENT '操作原因(暂停必填)',
  `terminal_type` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '操作端:1后台 2平板',
  `operator_id` bigint unsigned NOT NULL COMMENT '操作人',
  `operate_time` datetime NOT NULL COMMENT '操作时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`, `operate_time`) COMMENT '按任务查操作履历'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '任务操作记录表(平板/后台)';

DROP TABLE IF EXISTS `prod_process_dispatch`;
CREATE TABLE `prod_process_dispatch` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dispatch_no` varchar(32) NOT NULL COMMENT '工序派工单号(唯一)',
  `task_id` bigint unsigned NOT NULL COMMENT '生产任务单',
  `routing_id` bigint unsigned NOT NULL COMMENT '工艺路线版本',
  `dispatch_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '状态:0待确认 1已确认 2执行中 3已完成',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dispatch_no` (`dispatch_no`) COMMENT '工序派工单号唯一',
  KEY `idx_task_id` (`task_id`) COMMENT '按任务查工序派工'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '生产派工单主表(工序派工)';

DROP TABLE IF EXISTS `prod_process_dispatch_detail`;
CREATE TABLE `prod_process_dispatch_detail` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dispatch_id` bigint unsigned NOT NULL COMMENT '工序派工主表',
  `process_id` bigint unsigned NOT NULL COMMENT '工序',
  `seq` int unsigned NOT NULL COMMENT '工序顺序(按工艺路线)',
  `station_id` bigint unsigned NULL DEFAULT NULL COMMENT '工位',
  `user_id` bigint unsigned NULL DEFAULT NULL COMMENT '作业人员',
  `equipment_id` bigint unsigned NULL DEFAULT NULL COMMENT '设备(停用设备不可派)',
  `plan_quantity` int unsigned NOT NULL COMMENT '计划数量',
  `good_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '良品数量',
  `defect_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '不良数量',
  `detail_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '工序状态:0待作业 1作业中 2已完成 3异常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dispatch_seq` (`dispatch_id`, `seq`) COMMENT '同派工单工序顺序唯一',
  KEY `idx_station_status` (`station_id`, `detail_status`) COMMENT '工位端查可作业工序(高频)',
  KEY `idx_user_status` (`user_id`, `detail_status`) COMMENT '操作工查本人工序任务',
  KEY `idx_equipment_id` (`equipment_id`) COMMENT '按设备查派工',
  KEY `idx_process_id` (`process_id`) COMMENT '按工序统计'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '工序派工明细表';

DROP TABLE IF EXISTS `prod_batch_status`;
CREATE TABLE `prod_batch_status` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_no` varchar(64) NOT NULL COMMENT '产品批次号(唯一)',
  `task_id` bigint unsigned NOT NULL COMMENT '生产任务单',
  `product_id` bigint unsigned NOT NULL COMMENT '产品',
  `current_process_id` bigint unsigned NULL DEFAULT NULL COMMENT '当前工序',
  `batch_status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '批次状态:1在制 2待检 3返修中 4隔离 5已完工 6已报废',
  `is_abnormal` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否异常:1是 0否(看板/报表提示)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_no` (`batch_no`) COMMENT '批次号唯一(追溯主键)',
  KEY `idx_task_id` (`task_id`) COMMENT '按任务查批次',
  KEY `idx_status_abnormal` (`batch_status`, `is_abnormal`) COMMENT '看板查异常/待检批次'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '产品生产状态表';

DROP TABLE IF EXISTS `prod_report`;
CREATE TABLE `prod_report` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `report_no` varchar(32) NOT NULL COMMENT '报工单号(唯一)',
  `task_id` bigint unsigned NOT NULL COMMENT '生产任务单',
  `dispatch_detail_id` bigint unsigned NULL DEFAULT NULL COMMENT '工序派工明细',
  `process_id` bigint unsigned NOT NULL COMMENT '工序',
  `batch_no` varchar(64) NOT NULL COMMENT '产品批次号',
  `barcode_id` bigint unsigned NULL DEFAULT NULL COMMENT '扫码条码',
  `report_type` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '报工类型:1普通 2不良 3检测 4设备计数 5关键物料 6装箱',
  `user_id` bigint unsigned NOT NULL COMMENT '报工人',
  `equipment_id` bigint unsigned NULL DEFAULT NULL COMMENT '设备',
  `station_id` bigint unsigned NULL DEFAULT NULL COMMENT '工位',
  `input_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '投入数量',
  `good_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '良品数量(良品+不良不超投入)',
  `defect_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '不良数量',
  `rework_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '返修数量',
  `source_type` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '数据来源:1人工 2设备自动(不互相覆盖)',
  `report_time` datetime NOT NULL COMMENT '报工时间',
  `report_status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1有效 2已修正作废(修正保留原记录)',
  `origin_report_id` bigint unsigned NULL DEFAULT NULL COMMENT '被修正的原报工记录',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_no` (`report_no`) COMMENT '报工单号唯一',
  KEY `idx_task_process` (`task_id`, `process_id`) COMMENT '任务工序进度汇总(高频)',
  KEY `idx_batch_no` (`batch_no`(20)) COMMENT '批次追溯查报工',
  KEY `idx_user_time` (`user_id`, `report_time`) COMMENT '计件按员工+日期汇总',
  KEY `idx_equipment_time` (`equipment_id`, `report_time`) COMMENT '设备产量统计(OEE)',
  KEY `idx_report_time` (`report_time`) COMMENT '产量报表按时间范围统计'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '生产报工主表';

DROP TABLE IF EXISTS `prod_report_defect`;
CREATE TABLE `prod_report_defect` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `report_id` bigint unsigned NOT NULL COMMENT '报工主表',
  `defect_reason_id` bigint unsigned NOT NULL COMMENT '不良原因',
  `defect_quantity` int unsigned NOT NULL COMMENT '不良数量',
  `defect_position` varchar(64) NULL DEFAULT NULL COMMENT '不良点位',
  `handle_type` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '处理方式:1返修 2报废 3隔离',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_report_id` (`report_id`) COMMENT '按报工查不良明细',
  KEY `idx_defect_reason_id` (`defect_reason_id`) COMMENT '不良原因排名统计'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '不良报工明细表';

DROP TABLE IF EXISTS `prod_report_packing`;
CREATE TABLE `prod_report_packing` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `report_id` bigint unsigned NOT NULL COMMENT '报工主表',
  `box_barcode_id` bigint unsigned NOT NULL COMMENT '箱码(内外箱/中箱/栈板)',
  `product_batch_no` varchar(64) NOT NULL COMMENT '箱内产品批次号',
  `packing_quantity` int unsigned NOT NULL COMMENT '装箱数量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_report_id` (`report_id`) COMMENT '按报工查装箱',
  KEY `idx_box_barcode_id` (`box_barcode_id`) COMMENT '按箱码查箱内批次(物流追溯)',
  KEY `idx_product_batch` (`product_batch_no`(20)) COMMENT '按产品批次查所在箱'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '装箱报工明细表';

DROP TABLE IF EXISTS `prod_report_material`;
CREATE TABLE `prod_report_material` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `report_id` bigint unsigned NOT NULL COMMENT '报工主表',
  `material_id` bigint unsigned NOT NULL COMMENT '关键物料',
  `material_batch_no` varchar(64) NOT NULL COMMENT '物料批次号(关键工序强制记录)',
  `product_batch_no` varchar(64) NOT NULL COMMENT '产品批次号(冗余;物料反查产品)',
  `use_quantity` decimal(12,4) NOT NULL COMMENT '使用数量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_report_id` (`report_id`) COMMENT '按报工查物料',
  KEY `idx_material_batch` (`material_id`, `material_batch_no`(20)) COMMENT '物料批次反查受影响产品(关键物料追溯核心)',
  KEY `idx_product_batch` (`product_batch_no`(20)) COMMENT '产品批次正向查物料批次'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '关键物料报工表';

DROP TABLE IF EXISTS `prod_finish_order`;
CREATE TABLE `prod_finish_order` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `finish_no` varchar(32) NOT NULL COMMENT '完工单号(唯一)',
  `task_id` bigint unsigned NOT NULL COMMENT '生产任务单',
  `work_order_id` bigint unsigned NOT NULL COMMENT '生产工单(冗余)',
  `product_id` bigint unsigned NOT NULL COMMENT '产品',
  `batch_no` varchar(64) NOT NULL COMMENT '产品批次号',
  `finish_quantity` int unsigned NOT NULL COMMENT '完工数量(不超可完工数量)',
  `good_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '良品数量',
  `defect_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '不良数量',
  `rework_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '返修数量',
  `finish_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '状态:0待审核 1已审核 2已作废(已审核不可删)',
  `audit_by` bigint unsigned NULL DEFAULT NULL COMMENT '审核人(车间主管)',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(255) NULL DEFAULT NULL COMMENT '审核意见',
  `sync_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '外部同步状态:0未读取 1ERP/WMS已读取',
  `create_by` bigint unsigned NOT NULL COMMENT '发起人(班组长)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_finish_no` (`finish_no`) COMMENT '完工单号唯一',
  KEY `idx_task_id` (`task_id`) COMMENT '按任务查完工单',
  KEY `idx_work_order_id` (`work_order_id`) COMMENT '按工单汇总完工',
  KEY `idx_status_sync` (`finish_status`, `sync_status`) COMMENT '完工单读取接口查已审核未同步(高频轮询)',
  KEY `idx_batch_no` (`batch_no`(20)) COMMENT '批次追溯'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '生产完工单主表';

DROP TABLE IF EXISTS `rework_order`;
CREATE TABLE `rework_order` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rework_no` varchar(32) NOT NULL COMMENT '返修单号(唯一)',
  `source_type` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '来源:1生产报工 2质检 3手工',
  `source_id` bigint unsigned NULL DEFAULT NULL COMMENT '来源单据ID(报工/检验单)',
  `task_id` bigint unsigned NOT NULL COMMENT '生产任务单',
  `product_id` bigint unsigned NOT NULL COMMENT '产品',
  `batch_no` varchar(64) NOT NULL COMMENT '不良批次号',
  `defect_reason_id` bigint unsigned NULL DEFAULT NULL COMMENT '不良原因',
  `defect_quantity` int unsigned NOT NULL COMMENT '不良数量',
  `rework_process_id` bigint unsigned NULL DEFAULT NULL COMMENT '返修工序',
  `handler_id` bigint unsigned NULL DEFAULT NULL COMMENT '返修责任人',
  `require_remark` varchar(500) NULL DEFAULT NULL COMMENT '处理要求',
  `rework_count` int unsigned NOT NULL DEFAULT 1 COMMENT '返修次数(多次返修累计)',
  `rework_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '状态:0待返修 1返修中 2待复检 3合格关闭 4已报废',
  `recheck_by` bigint unsigned NULL DEFAULT NULL COMMENT '复检质检员',
  `recheck_result` tinyint unsigned NULL DEFAULT NULL COMMENT '复检结论:1合格 2继续返修 3报废',
  `qualified_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '复检合格数量',
  `scrap_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '报废数量(需记录责任原因)',
  `recheck_time` datetime NULL DEFAULT NULL COMMENT '复检时间',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rework_no` (`rework_no`) COMMENT '返修单号唯一',
  KEY `idx_task_id` (`task_id`) COMMENT '按任务查返修',
  KEY `idx_batch_no` (`batch_no`(20)) COMMENT '批次追溯返修履历',
  KEY `idx_status` (`rework_status`) COMMENT '待返修/待复检工作台'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '返修工单表';


-- ============================== 工艺管理 ==============================

DROP TABLE IF EXISTS `craft_process`;
CREATE TABLE `craft_process` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `process_code` varchar(32) NOT NULL COMMENT '工序编码(唯一)',
  `process_name` varchar(64) NOT NULL COMMENT '工序名称(球头准备/羽片分选/植毛/注胶/定型烘干/修整/称重/外观检验/包装)',
  `process_type` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '工序类型:1加工 2检验 3包装',
  `standard_hour` decimal(8,2) NULL DEFAULT NULL COMMENT '标准工时(秒/只)',
  `is_key` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否关键工序:1是 0否(关键工序强制记录报工人员)',
  `is_inspect` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否需要质检:1是 0否',
  `is_scan` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否必须扫码:1是 0否',
  `is_piece` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否参与计件:1是 0否',
  `equipment_category_id` bigint unsigned NULL DEFAULT NULL COMMENT '设备类别要求',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_process_code` (`process_code`) COMMENT '工序编码唯一'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '工序主表';

DROP TABLE IF EXISTS `craft_process_defect`;
CREATE TABLE `craft_process_defect` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `process_id` bigint unsigned NOT NULL COMMENT '工序',
  `defect_code` varchar(32) NOT NULL COMMENT '不良原因编码(工序内唯一)',
  `defect_name` varchar(64) NOT NULL COMMENT '不良原因名称(羽片歪斜/胶量不足/球头开裂等)',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_process_defect_code` (`process_id`, `defect_code`) COMMENT '工序内不良编码唯一'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '工序不良原因表';

DROP TABLE IF EXISTS `craft_sop`;
CREATE TABLE `craft_sop` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sop_code` varchar(32) NOT NULL COMMENT 'SOP编码',
  `sop_name` varchar(128) NOT NULL COMMENT 'SOP名称',
  `version` varchar(16) NOT NULL DEFAULT 'V1' COMMENT '版本(编码+版本唯一)',
  `effect_date` date NULL DEFAULT NULL COMMENT '生效日期',
  `sop_status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1生效 0停用(停用版本不作为新任务默认)',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人(工艺工程师)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code_version` (`sop_code`, `version`) COMMENT 'SOP编码+版本唯一'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = 'SOP主表';

DROP TABLE IF EXISTS `craft_sop_file`;
CREATE TABLE `craft_sop_file` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sop_id` bigint unsigned NOT NULL COMMENT 'SOP主表',
  `file_type` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '文件类型:1图片 2视频 3文档',
  `file_name` varchar(128) NOT NULL COMMENT '文件名称',
  `file_path` varchar(255) NOT NULL COMMENT '文件路径',
  `seq` int unsigned NOT NULL DEFAULT 0 COMMENT '轮播顺序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_sop_id` (`sop_id`) COMMENT '按SOP查文件'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = 'SOP文件表';

DROP TABLE IF EXISTS `craft_routing`;
CREATE TABLE `craft_routing` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `routing_code` varchar(32) NOT NULL COMMENT '路线编码',
  `routing_name` varchar(128) NOT NULL COMMENT '路线名称',
  `version` varchar(16) NOT NULL DEFAULT 'V1' COMMENT '路线版本(编码+版本唯一;已用于任务的版本不可覆盖)',
  `source_type` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '来源:1本地创建 2ERP读取确认',
  `routing_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '状态:0草稿 1生效 2停用(停用不可用于新任务)',
  `audit_by` bigint unsigned NULL DEFAULT NULL COMMENT '审核人',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code_version` (`routing_code`, `version`) COMMENT '路线编码+版本唯一'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '工艺路线主表';

DROP TABLE IF EXISTS `craft_routing_detail`;
CREATE TABLE `craft_routing_detail` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `routing_id` bigint unsigned NOT NULL COMMENT '工艺路线',
  `seq` int unsigned NOT NULL COMMENT '工序顺序(路线内不可重复不可断裂)',
  `process_id` bigint unsigned NOT NULL COMMENT '工序',
  `station_id` bigint unsigned NULL DEFAULT NULL COMMENT '默认工位',
  `is_inspect` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否质检节点:1是 0否',
  `sop_id` bigint unsigned NULL DEFAULT NULL COMMENT '绑定SOP',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_routing_seq` (`routing_id`, `seq`) COMMENT '路线内工序顺序不重复',
  KEY `idx_process_id` (`process_id`) COMMENT '工序被引用校验(删除拦截)'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '工艺路线明细表';

DROP TABLE IF EXISTS `craft_routing_product`;
CREATE TABLE `craft_routing_product` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `routing_id` bigint unsigned NOT NULL COMMENT '工艺路线',
  `product_id` bigint unsigned NOT NULL COMMENT '产品(一条路线可绑多产品)',
  `is_default` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '是否产品默认路线:1是 0否',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_routing_product` (`routing_id`, `product_id`) COMMENT '绑定关系唯一',
  KEY `idx_product_default` (`product_id`, `is_default`) COMMENT '按产品取默认路线(高频)'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '路线产品关系表';


-- ============================== 设备管理 ==============================

DROP TABLE IF EXISTS `eqp_category`;
CREATE TABLE `eqp_category` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_code` varchar(32) NOT NULL COMMENT '类别编码(唯一)',
  `category_name` varchar(64) NOT NULL COMMENT '类别名称(植毛机/注胶机/烘干设备/分拣设备/包装设备)',
  `parent_id` bigint unsigned NULL DEFAULT NULL COMMENT '上级类别(树形)',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用(停用后不可新增该类设备)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_code` (`category_code`) COMMENT '类别编码唯一',
  KEY `idx_parent_id` (`parent_id`) COMMENT '类别树查询'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '设备类别表';

DROP TABLE IF EXISTS `eqp_manufacturer`;
CREATE TABLE `eqp_manufacturer` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `manufacturer_name` varchar(128) NOT NULL COMMENT '制造商名称',
  `contact` varchar(64) NULL DEFAULT NULL COMMENT '联系人',
  `phone` varchar(20) NULL DEFAULT NULL COMMENT '联系电话',
  `address` varchar(255) NULL DEFAULT NULL COMMENT '地址',
  `after_sale_remark` varchar(500) NULL DEFAULT NULL COMMENT '售后说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_manufacturer_name` (`manufacturer_name`(20)) COMMENT '按名称查(不建唯一;业务仅建议不重复)'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '设备制造商表';

DROP TABLE IF EXISTS `eqp_fault_reason`;
CREATE TABLE `eqp_fault_reason` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `reason_code` varchar(32) NOT NULL COMMENT '故障编码(唯一)',
  `category_id` bigint unsigned NOT NULL COMMENT '设备类别(按类别维护)',
  `reason_name` varchar(64) NOT NULL COMMENT '故障名称',
  `fault_desc` varchar(500) NULL DEFAULT NULL COMMENT '故障现象描述',
  `handle_suggest` varchar(500) NULL DEFAULT NULL COMMENT '处理建议',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用(停用不用于新报修)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reason_code` (`reason_code`) COMMENT '故障编码唯一',
  KEY `idx_category_id` (`category_id`) COMMENT '报修按设备类别选原因'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '故障原因表';

DROP TABLE IF EXISTS `eqp_equipment`;
CREATE TABLE `eqp_equipment` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `equipment_code` varchar(32) NOT NULL COMMENT '设备编码(唯一)',
  `equipment_name` varchar(128) NOT NULL COMMENT '设备名称',
  `category_id` bigint unsigned NOT NULL COMMENT '设备类别',
  `model` varchar(64) NULL DEFAULT NULL COMMENT '型号',
  `manufacturer_id` bigint unsigned NULL DEFAULT NULL COMMENT '制造商',
  `workshop_id` bigint unsigned NOT NULL COMMENT '所属车间',
  `line_id` bigint unsigned NULL DEFAULT NULL COMMENT '所属产线(变更保留状态日志)',
  `station_id` bigint unsigned NULL DEFAULT NULL COMMENT '所在工位',
  `manage_dept` varchar(64) NULL DEFAULT NULL COMMENT '管理部门',
  `manager_id` bigint unsigned NULL DEFAULT NULL COMMENT '责任人',
  `theory_beat` decimal(8,2) NULL DEFAULT NULL COMMENT '理论节拍(秒/只;OEE性能效率计算)',
  `run_status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '运行状态:1运行 2停机 3维修 4停用 5报废(停用/维修不可新派工)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_equipment_code` (`equipment_code`) COMMENT '设备编码唯一',
  KEY `idx_category_id` (`category_id`) COMMENT '按类别统计设备',
  KEY `idx_workshop_line` (`workshop_id`, `line_id`) COMMENT '按车间产线筛设备(移动端)',
  KEY `idx_run_status` (`run_status`) COMMENT '异常/维修设备优先展示'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '设备台账表';

DROP TABLE IF EXISTS `eqp_maintain_plan`;
CREATE TABLE `eqp_maintain_plan` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `plan_no` varchar(32) NOT NULL COMMENT '计划编号(唯一)',
  `equipment_id` bigint unsigned NULL DEFAULT NULL COMMENT '设备(与类别二选一)',
  `category_id` bigint unsigned NULL DEFAULT NULL COMMENT '设备类别(按类别配置)',
  `maintain_item` varchar(255) NOT NULL COMMENT '保养项目',
  `maintain_standard` varchar(500) NULL DEFAULT NULL COMMENT '保养标准',
  `cycle_type` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '周期类型:1日 2周 3月 4季 5年',
  `cycle_value` int unsigned NOT NULL DEFAULT 1 COMMENT '周期值(每N个周期)',
  `handler_id` bigint unsigned NOT NULL COMMENT '默认责任人',
  `next_time` datetime NULL DEFAULT NULL COMMENT '下次生成任务时间',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0暂停(维修/停用设备可暂停)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_no` (`plan_no`) COMMENT '计划编号唯一',
  KEY `idx_equipment_id` (`equipment_id`) COMMENT '按设备查计划',
  KEY `idx_status_next` (`status`, `next_time`) COMMENT '定时任务扫描到期计划'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '保养计划表';

DROP TABLE IF EXISTS `eqp_maintain_task`;
CREATE TABLE `eqp_maintain_task` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_no` varchar(32) NOT NULL COMMENT '任务编号(唯一)',
  `plan_id` bigint unsigned NOT NULL COMMENT '保养计划',
  `equipment_id` bigint unsigned NOT NULL COMMENT '设备',
  `plan_time` datetime NOT NULL COMMENT '计划保养时间',
  `handler_id` bigint unsigned NOT NULL COMMENT '执行人',
  `finish_time` datetime NULL DEFAULT NULL COMMENT '实际完成时间',
  `result_type` tinyint unsigned NULL DEFAULT NULL COMMENT '保养结果:1正常 2异常(异常可触发报修)',
  `result_remark` varchar(500) NULL DEFAULT NULL COMMENT '保养结果说明',
  `image_path` varchar(500) NULL DEFAULT NULL COMMENT '照片路径',
  `is_overdue` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否逾期:1是 0否',
  `task_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '状态:0待执行 1已执行 2已审核',
  `audit_by` bigint unsigned NULL DEFAULT NULL COMMENT '审核人(设备管理员)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_no` (`task_no`) COMMENT '任务编号唯一',
  KEY `idx_equipment_id` (`equipment_id`) COMMENT '按设备查保养历史',
  KEY `idx_handler_status` (`handler_id`, `task_status`) COMMENT '维修人待办',
  KEY `idx_status_plan_time` (`task_status`, `plan_time`) COMMENT '逾期扫描与完成率统计'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '保养任务表';

DROP TABLE IF EXISTS `eqp_spot_check_item`;
CREATE TABLE `eqp_spot_check_item` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_id` bigint unsigned NOT NULL COMMENT '设备类别',
  `item_name` varchar(128) NOT NULL COMMENT '点检项目名称',
  `check_standard` varchar(500) NULL DEFAULT NULL COMMENT '点检标准',
  `check_cycle` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '点检频率:1每班 2每日 3每周',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`) COMMENT '按类别查点检项目'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '点检标准表';

DROP TABLE IF EXISTS `eqp_spot_check_record`;
CREATE TABLE `eqp_spot_check_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `equipment_id` bigint unsigned NOT NULL COMMENT '点检设备',
  `item_id` bigint unsigned NOT NULL COMMENT '点检项目',
  `check_result` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '点检结果:1正常 2异常',
  `abnormal_remark` varchar(500) NULL DEFAULT NULL COMMENT '异常说明(结果异常必填)',
  `handle_type` tinyint unsigned NULL DEFAULT NULL COMMENT '异常处理:1生成报修 2生成安灯 3现场处理',
  `handle_result` varchar(255) NULL DEFAULT NULL COMMENT '处理结果',
  `checker_id` bigint unsigned NOT NULL COMMENT '点检人',
  `check_time` datetime NOT NULL COMMENT '点检时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_equipment_time` (`equipment_id`, `check_time`) COMMENT '按设备查点检记录(关键设备未点检拦截开工)',
  KEY `idx_check_result` (`check_result`, `check_time`) COMMENT '异常点检频率统计'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '点检记录表';

DROP TABLE IF EXISTS `eqp_repair_order`;
CREATE TABLE `eqp_repair_order` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `repair_no` varchar(32) NOT NULL COMMENT '报修单号(唯一)',
  `equipment_id` bigint unsigned NOT NULL COMMENT '故障设备',
  `line_id` bigint unsigned NULL DEFAULT NULL COMMENT '影响产线',
  `fault_reason_id` bigint unsigned NULL DEFAULT NULL COMMENT '故障现象(报修人选择)',
  `real_fault_reason_id` bigint unsigned NULL DEFAULT NULL COMMENT '真实故障原因(维修人补充)',
  `urgent_level` tinyint unsigned NOT NULL DEFAULT 2 COMMENT '紧急程度:1紧急 2一般 3低',
  `repair_desc` varchar(500) NULL DEFAULT NULL COMMENT '报修说明',
  `image_path` varchar(500) NULL DEFAULT NULL COMMENT '故障照片路径',
  `reporter_id` bigint unsigned NOT NULL COMMENT '报修人',
  `assign_to` bigint unsigned NULL DEFAULT NULL COMMENT '维修人(设备管理员分派)',
  `handle_process` varchar(1000) NULL DEFAULT NULL COMMENT '维修处理过程',
  `handle_result` varchar(500) NULL DEFAULT NULL COMMENT '维修结果',
  `stop_start_time` datetime NULL DEFAULT NULL COMMENT '停机开始时间',
  `stop_end_time` datetime NULL DEFAULT NULL COMMENT '停机结束时间',
  `stop_minutes` int unsigned NULL DEFAULT NULL COMMENT '停机时长(分钟)',
  `relate_repair_id` bigint unsigned NULL DEFAULT NULL COMMENT '关联重复报修单',
  `accept_by` bigint unsigned NULL DEFAULT NULL COMMENT '验收人',
  `repair_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '状态:0待分派 1维修中 2待验收 3已关闭(未分派不可关闭)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报修时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_repair_no` (`repair_no`) COMMENT '报修单号唯一',
  KEY `idx_equipment_time` (`equipment_id`, `create_time`) COMMENT '设备维修历史(重复报修关联)',
  KEY `idx_assign_status` (`assign_to`, `repair_status`) COMMENT '维修人待办',
  KEY `idx_status` (`repair_status`) COMMENT '待分派/待验收工作台'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '报修任务表';

DROP TABLE IF EXISTS `eqp_run_record`;
CREATE TABLE `eqp_run_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `equipment_id` bigint unsigned NOT NULL COMMENT '设备',
  `line_id` bigint unsigned NULL DEFAULT NULL COMMENT '产线(冗余)',
  `run_status` tinyint unsigned NOT NULL COMMENT '状态:1运行 2停机 3故障',
  `stop_reason_type` tinyint unsigned NULL DEFAULT NULL COMMENT '停机原因分类:1计划停机 2故障 3换型 4缺料 9其他',
  `start_time` datetime NOT NULL COMMENT '状态开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '状态结束时间',
  `duration_minutes` int unsigned NULL DEFAULT NULL COMMENT '持续时长(分钟)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_equipment_start` (`equipment_id`, `start_time`) COMMENT 'OEE按设备+时段汇总(高频)',
  KEY `idx_line_start` (`line_id`, `start_time`) COMMENT '产线维度OEE汇总'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '设备运行记录表';

DROP TABLE IF EXISTS `eqp_oee_statistic`;
CREATE TABLE `eqp_oee_statistic` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `equipment_id` bigint unsigned NULL DEFAULT NULL COMMENT '设备(设备维度)',
  `line_id` bigint unsigned NULL DEFAULT NULL COMMENT '产线(产线维度)',
  `shift_id` bigint unsigned NULL DEFAULT NULL COMMENT '班次',
  `plan_minutes` int unsigned NOT NULL DEFAULT 0 COMMENT '计划生产时间(分钟)',
  `run_minutes` int unsigned NOT NULL DEFAULT 0 COMMENT '实际运行时间(分钟)',
  `stop_minutes` int unsigned NOT NULL DEFAULT 0 COMMENT '停机时间(分钟)',
  `output_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '产出数量',
  `defect_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '不良数量',
  `time_rate` decimal(5,2) NULL DEFAULT NULL COMMENT '时间稼动率(%)',
  `perf_rate` decimal(5,2) NULL DEFAULT NULL COMMENT '性能稼动率(%;缺理论节拍时为空)',
  `quality_rate` decimal(5,2) NULL DEFAULT NULL COMMENT '良品率(%)',
  `oee` decimal(5,2) NULL DEFAULT NULL COMMENT 'OEE(%)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date_eqp_line_shift` (`stat_date`, `equipment_id`, `line_id`, `shift_id`) COMMENT '统计维度唯一防重复计算',
  KEY `idx_line_date` (`line_id`, `stat_date`) COMMENT '产线OEE趋势'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = 'OEE统计表';

DROP TABLE IF EXISTS `energy_record`;
CREATE TABLE `energy_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `equipment_id` bigint unsigned NOT NULL COMMENT '设备',
  `energy_type` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '能耗类型:1电 2水 3气',
  `energy_value` decimal(12,2) NOT NULL COMMENT '能耗值(非负)',
  `collect_time` datetime NOT NULL COMMENT '采集时间',
  `source_type` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '来源:1采集 2手工录入',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_equipment_collect` (`equipment_id`, `collect_time`) COMMENT '按设备+时段汇总能耗'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '能耗采集记录表';


-- ============================== 设备对接 ==============================

DROP TABLE IF EXISTS `eqp_access_config`;
CREATE TABLE `eqp_access_config` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `equipment_id` bigint unsigned NOT NULL COMMENT '设备',
  `collect_point` varchar(64) NOT NULL COMMENT '采集点编码',
  `line_id` bigint unsigned NOT NULL COMMENT '关联产线',
  `process_id` bigint unsigned NOT NULL COMMENT '关联工序',
  `count_rule` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '计数规则:1累计值 2增量值',
  `data_source` varchar(64) NULL DEFAULT NULL COMMENT '数据来源说明(PLC/传感器/API)',
  `auto_report` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否自动报工:1自动 0待确认',
  `debug_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '联调状态:0未联调 1联调中 2联调通过(未通过不可启用)',
  `is_enabled` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否启用采集:1是 0否',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_equipment_point` (`equipment_id`, `collect_point`) COMMENT '同设备采集点唯一',
  KEY `idx_line_process` (`line_id`, `process_id`) COMMENT '计数匹配任务时定位工序'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '设备接入配置表';

DROP TABLE IF EXISTS `eqp_count_record`;
CREATE TABLE `eqp_count_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `equipment_id` bigint unsigned NOT NULL COMMENT '设备',
  `collect_time` datetime NOT NULL COMMENT '采集时间',
  `serial_no` varchar(64) NULL DEFAULT NULL COMMENT '上报流水号(设备+时间+流水去重)',
  `count_value` int unsigned NOT NULL COMMENT '计数值(累计)',
  `increment_value` int unsigned NOT NULL DEFAULT 0 COMMENT '增量值',
  `run_status` tinyint unsigned NULL DEFAULT NULL COMMENT '设备运行状态:1运行 2停机 3故障',
  `task_id` bigint unsigned NULL DEFAULT NULL COMMENT '匹配生产任务',
  `process_id` bigint unsigned NULL DEFAULT NULL COMMENT '匹配工序',
  `match_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '匹配状态:0待匹配 1匹配成功 2匹配失败入异常池',
  `report_id` bigint unsigned NULL DEFAULT NULL COMMENT '生成的报工记录',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eqp_time_serial` (`equipment_id`, `collect_time`, `serial_no`(20)) COMMENT '重复上报去重(业务强制)',
  KEY `idx_task_process` (`task_id`, `process_id`) COMMENT '按任务查设备计数',
  KEY `idx_match_status` (`match_status`, `collect_time`) COMMENT '待匹配/异常数据扫描'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '设备计数记录表';

DROP TABLE IF EXISTS `eqp_count_error`;
CREATE TABLE `eqp_count_error` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `equipment_id` bigint unsigned NULL DEFAULT NULL COMMENT '设备',
  `collect_time` datetime NULL DEFAULT NULL COMMENT '采集时间',
  `raw_data` varchar(1000) NOT NULL COMMENT '原始上报数据',
  `error_type` tinyint unsigned NOT NULL COMMENT '异常类型:1无法匹配任务 2格式异常 3重复上报 4计数倒退跳变',
  `handle_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '处理状态:0待处理 1已处理 2已忽略',
  `handler_id` bigint unsigned NULL DEFAULT NULL COMMENT '处理人',
  `handle_remark` varchar(500) NULL DEFAULT NULL COMMENT '处理说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_handle_status` (`handle_status`, `create_time`) COMMENT '异常池待处理列表',
  KEY `idx_equipment_id` (`equipment_id`) COMMENT '按设备查异常数据'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '设备计数异常表';


-- ============================== 计件工资 ==============================

DROP TABLE IF EXISTS `wage_piece_price`;
CREATE TABLE `wage_piece_price` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `process_id` bigint unsigned NOT NULL COMMENT '工序',
  `product_id` bigint unsigned NOT NULL COMMENT '产品',
  `unit_price` decimal(10,4) NOT NULL COMMENT '计件单价(元/只)',
  `defect_deduct_type` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '不良处理:1不计件 2半价 3全计件',
  `effect_date` date NOT NULL COMMENT '生效日期(变更从生效日起影响新数据)',
  `expire_date` date NULL DEFAULT NULL COMMENT '失效日期',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_process_product_effect` (`process_id`, `product_id`, `effect_date`) COMMENT '同工序同产品同生效日唯一',
  KEY `idx_product_id` (`product_id`) COMMENT '按产品查单价'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '工序计件单价表';

DROP TABLE IF EXISTS `wage_piece_detail`;
CREATE TABLE `wage_piece_detail` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stat_date` date NOT NULL COMMENT '计件日期',
  `user_id` bigint unsigned NOT NULL COMMENT '员工',
  `task_id` bigint unsigned NOT NULL COMMENT '生产任务单',
  `process_id` bigint unsigned NOT NULL COMMENT '工序',
  `product_id` bigint unsigned NOT NULL COMMENT '产品',
  `report_id` bigint unsigned NOT NULL COMMENT '来源报工记录(同报工不重复计件)',
  `qualified_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '合格数量(审核后有效报工)',
  `defect_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '不良扣减数量',
  `unit_price` decimal(10,4) NOT NULL COMMENT '单价快照(计算时点)',
  `amount` decimal(12,2) NOT NULL COMMENT '计件金额',
  `audit_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '审核状态:0待审核 1已审核(审核后不可改)',
  `audit_by` bigint unsigned NULL DEFAULT NULL COMMENT '审核人',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `adjust_reason` varchar(255) NULL DEFAULT NULL COMMENT '调整原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_id` (`report_id`) COMMENT '同一报工不重复计件(业务强制)',
  KEY `idx_user_date` (`user_id`, `stat_date`) COMMENT '员工按日期查本人计件(高频)',
  KEY `idx_date_process` (`stat_date`, `process_id`) COMMENT '按日期工序汇总金额',
  KEY `idx_audit_status` (`audit_status`) COMMENT '待审核工作台'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '计件工资明细表';


-- ============================== 质量管理 ==============================

DROP TABLE IF EXISTS `qc_item_category`;
CREATE TABLE `qc_item_category` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_code` varchar(32) NOT NULL COMMENT '分类编码(唯一)',
  `category_name` varchar(64) NOT NULL COMMENT '分类名称(外观/重量/羽片排列/球头牢固度/飞行稳定性)',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_code` (`category_code`) COMMENT '分类编码唯一'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '检验项目分类表';

DROP TABLE IF EXISTS `qc_item`;
CREATE TABLE `qc_item` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `item_code` varchar(32) NOT NULL COMMENT '项目编码(唯一)',
  `category_id` bigint unsigned NOT NULL COMMENT '项目分类',
  `item_name` varchar(128) NOT NULL COMMENT '项目名称',
  `unit_id` bigint unsigned NULL DEFAULT NULL COMMENT '计量单位(数值型必填)',
  `judge_type` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '判定方式:1数值范围 2目测判定',
  `standard_value` decimal(12,4) NULL DEFAULT NULL COMMENT '标准值',
  `upper_limit` decimal(12,4) NULL DEFAULT NULL COMMENT '上限(数值型必填)',
  `lower_limit` decimal(12,4) NULL DEFAULT NULL COMMENT '下限(数值型必填)',
  `check_method` varchar(255) NULL DEFAULT NULL COMMENT '检验方法说明',
  `is_required` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否必检:1是 0否',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_code` (`item_code`) COMMENT '项目编码唯一',
  KEY `idx_category_id` (`category_id`) COMMENT '分类下项目列表'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '检验项目表';

DROP TABLE IF EXISTS `qc_scheme`;
CREATE TABLE `qc_scheme` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scheme_code` varchar(32) NOT NULL COMMENT '方案编码',
  `scheme_name` varchar(128) NOT NULL COMMENT '方案名称',
  `product_id` bigint unsigned NOT NULL COMMENT '适用产品',
  `customer_id` bigint unsigned NULL DEFAULT NULL COMMENT '适用客户(客户方案优先)',
  `inspect_type` tinyint unsigned NOT NULL COMMENT '检验类型:1首件 2末件 3巡检 4成品入库 5成品发货',
  `version` varchar(16) NOT NULL DEFAULT 'V1' COMMENT '方案版本(变更形成新版本)',
  `is_default` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '是否默认方案(同产品同类型仅一个默认生效)',
  `effect_date` date NULL DEFAULT NULL COMMENT '生效日期',
  `scheme_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '状态:0草稿 1生效 2停用(未审核不可用于检验)',
  `audit_by` bigint unsigned NULL DEFAULT NULL COMMENT '审核人(质量主管)',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code_version` (`scheme_code`, `version`) COMMENT '方案编码+版本唯一',
  KEY `idx_product_type_default` (`product_id`, `inspect_type`, `is_default`, `scheme_status`) COMMENT '检验单自动带出适用方案(高频)',
  KEY `idx_customer_id` (`customer_id`) COMMENT '客户专用方案优先匹配'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '检验标准方案主表';

DROP TABLE IF EXISTS `qc_scheme_item`;
CREATE TABLE `qc_scheme_item` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scheme_id` bigint unsigned NOT NULL COMMENT '检验方案',
  `item_id` bigint unsigned NOT NULL COMMENT '检验项目',
  `sample_quantity` int unsigned NOT NULL DEFAULT 1 COMMENT '抽样数量',
  `judge_standard` varchar(255) NULL DEFAULT NULL COMMENT '判定标准说明',
  `is_required` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '是否必检:1是 0否(必检项检验单不可为空)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scheme_item` (`scheme_id`, `item_id`) COMMENT '同方案项目不重复',
  KEY `idx_item_id` (`item_id`) COMMENT '项目被引用校验(删除拦截)'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '检验方案明细表';

DROP TABLE IF EXISTS `qc_inspection`;
CREATE TABLE `qc_inspection` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `inspection_no` varchar(32) NOT NULL COMMENT '检验单号(唯一)',
  `inspect_type` tinyint unsigned NOT NULL COMMENT '检验类型:1首件 2末件 3巡检 4成品入库 5成品发货',
  `task_id` bigint unsigned NULL DEFAULT NULL COMMENT '生产任务单(首末件/巡检)',
  `finish_order_id` bigint unsigned NULL DEFAULT NULL COMMENT '生产完工单(入库检验)',
  `work_order_id` bigint unsigned NULL DEFAULT NULL COMMENT '生产工单(冗余)',
  `delivery_no` varchar(64) NULL DEFAULT NULL COMMENT '发货通知单号(发货检验)',
  `product_id` bigint unsigned NOT NULL COMMENT '产品',
  `batch_no` varchar(64) NOT NULL COMMENT '产品批次号',
  `customer_id` bigint unsigned NULL DEFAULT NULL COMMENT '客户(发货检验)',
  `scheme_id` bigint unsigned NOT NULL COMMENT '检验方案版本',
  `sample_quantity` int unsigned NOT NULL DEFAULT 1 COMMENT '抽样数量',
  `inspector_id` bigint unsigned NOT NULL COMMENT '质检员',
  `inspect_time` datetime NOT NULL COMMENT '检验时间',
  `conclusion` tinyint unsigned NULL DEFAULT NULL COMMENT '检验结论:1合格 2不合格 3让步接收 4返修 5报废',
  `release_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '放行状态:0待检 1放行 2冻结/停线',
  `concession_by` bigint unsigned NULL DEFAULT NULL COMMENT '让步接收审批人',
  `concession_reason` varchar(255) NULL DEFAULT NULL COMMENT '让步接收原因',
  `handle_remark` varchar(500) NULL DEFAULT NULL COMMENT '不合格处理意见',
  `inspection_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '单据状态:0待检验 1已完成 2已作废',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inspection_no` (`inspection_no`) COMMENT '检验单号唯一',
  KEY `idx_task_type` (`task_id`, `inspect_type`) COMMENT '首检未过拦截批量报工(高频校验)',
  KEY `idx_batch_no` (`batch_no`(20)) COMMENT '批次追溯查检验',
  KEY `idx_finish_order_id` (`finish_order_id`) COMMENT '完工单查入库检验',
  KEY `idx_type_status_time` (`inspect_type`, `inspection_status`, `inspect_time`) COMMENT '各类待检任务列表与质量统计'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '检验单主表(首末件/巡检/入库/发货统一)';

DROP TABLE IF EXISTS `qc_inspection_item`;
CREATE TABLE `qc_inspection_item` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `inspection_id` bigint unsigned NOT NULL COMMENT '检验单',
  `item_id` bigint unsigned NOT NULL COMMENT '检验项目',
  `measured_value` varchar(64) NULL DEFAULT NULL COMMENT '实测值(数值或目测描述;必检项不可为空)',
  `judge_result` tinyint unsigned NOT NULL COMMENT '判定结果:1合格 2不合格',
  `defect_remark` varchar(255) NULL DEFAULT NULL COMMENT '不良现象说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_inspection_id` (`inspection_id`) COMMENT '按检验单查项目结果',
  KEY `idx_item_result` (`item_id`, `judge_result`) COMMENT '按项目统计不合格率'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '检验项目结果表';


-- ============================== 安灯管理 ==============================

DROP TABLE IF EXISTS `andon_type`;
CREATE TABLE `andon_type` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type_code` varchar(32) NOT NULL COMMENT '类型编码(唯一)',
  `type_name` varchar(64) NOT NULL COMMENT '类型名称',
  `exception_category` tinyint unsigned NOT NULL COMMENT '异常类别:1生产 2设备 3质量 4物料 5非生产',
  `handle_mode` tinyint unsigned NOT NULL DEFAULT 2 COMMENT '处理方式:1不处理 2自行处理 3协助处理',
  `response_minutes` int unsigned NULL DEFAULT NULL COMMENT '响应时限(分钟)',
  `is_light_control` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否联动设备灯控:1是 0否(不支持时降级记录状态)',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用(停用不可用于新异常)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_code` (`type_code`) COMMENT '类型编码唯一'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '安灯类型表';

DROP TABLE IF EXISTS `andon_config`;
CREATE TABLE `andon_config` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `andon_type_id` bigint unsigned NOT NULL COMMENT '安灯类型',
  `line_id` bigint unsigned NULL DEFAULT NULL COMMENT '适用产线(NULL为全部)',
  `handle_role_id` bigint unsigned NULL DEFAULT NULL COMMENT '处理角色(协助处理必配其一)',
  `handler_id` bigint unsigned NULL DEFAULT NULL COMMENT '处理人',
  `notify_way` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '通知方式:1系统消息 2看板 3消息+看板',
  `response_minutes` int unsigned NOT NULL DEFAULT 30 COMMENT '响应时限(分钟;超时升级)',
  `upgrade_to` bigint unsigned NULL DEFAULT NULL COMMENT '升级通知对象',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_line` (`andon_type_id`, `line_id`) COMMENT '同类型同产线一条配置'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '异常配置表';

DROP TABLE IF EXISTS `andon_reason`;
CREATE TABLE `andon_reason` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `reason_code` varchar(32) NOT NULL COMMENT '原因编码(唯一)',
  `andon_type_id` bigint unsigned NOT NULL COMMENT '归属安灯类型',
  `reason_name` varchar(64) NOT NULL COMMENT '原因名称',
  `reason_desc` varchar(255) NULL DEFAULT NULL COMMENT '原因描述',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用(停用不用于新异常)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reason_code` (`reason_code`) COMMENT '原因编码唯一',
  KEY `idx_andon_type_id` (`andon_type_id`) COMMENT '按类型选原因'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '异常原因表';

DROP TABLE IF EXISTS `andon_exception`;
CREATE TABLE `andon_exception` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `exception_no` varchar(32) NOT NULL COMMENT '异常单号(唯一)',
  `andon_type_id` bigint unsigned NOT NULL COMMENT '安灯类型',
  `reason_id` bigint unsigned NULL DEFAULT NULL COMMENT '发起时选择原因',
  `real_reason_id` bigint unsigned NULL DEFAULT NULL COMMENT '处理人确认的实际原因',
  `workshop_id` bigint unsigned NULL DEFAULT NULL COMMENT '车间',
  `line_id` bigint unsigned NULL DEFAULT NULL COMMENT '产线(非生产异常可空)',
  `task_id` bigint unsigned NULL DEFAULT NULL COMMENT '影响生产任务',
  `process_id` bigint unsigned NULL DEFAULT NULL COMMENT '工序',
  `equipment_id` bigint unsigned NULL DEFAULT NULL COMMENT '设备(设备异常可联动报修)',
  `repair_order_id` bigint unsigned NULL DEFAULT NULL COMMENT '联动生成的报修单',
  `batch_no` varchar(64) NULL DEFAULT NULL COMMENT '产品批次号',
  `description` varchar(1000) NULL DEFAULT NULL COMMENT '异常描述',
  `image_path` varchar(500) NULL DEFAULT NULL COMMENT '照片路径(上传失败可仅文字)',
  `source_terminal` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '发起端:1后台 2平板 3移动端',
  `reporter_id` bigint unsigned NOT NULL COMMENT '发起人',
  `handler_id` bigint unsigned NULL DEFAULT NULL COMMENT '当前处理人',
  `confirm_time` datetime NULL DEFAULT NULL COMMENT '确认时间',
  `close_time` datetime NULL DEFAULT NULL COMMENT '关闭时间',
  `handle_result` varchar(500) NULL DEFAULT NULL COMMENT '处理结果(关闭必填)',
  `stop_minutes` int unsigned NULL DEFAULT NULL COMMENT '停线时长(分钟)',
  `affect_quantity` int unsigned NULL DEFAULT NULL COMMENT '影响数量',
  `is_timeout` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否超时:1是 0否',
  `light_status` tinyint unsigned NULL DEFAULT NULL COMMENT '安灯灯控状态:1已亮灯 2已关灯 3不支持',
  `exception_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '状态:0待确认 1处理中 2已关闭',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exception_no` (`exception_no`) COMMENT '异常单号唯一',
  KEY `idx_line_status` (`line_id`, `exception_status`) COMMENT '看板查产线未关闭异常(高频)',
  KEY `idx_task_id` (`task_id`) COMMENT '按任务查异常',
  KEY `idx_equipment_id` (`equipment_id`) COMMENT '按设备查异常',
  KEY `idx_handler_status` (`handler_id`, `exception_status`) COMMENT '处理人待办(移动端)',
  KEY `idx_reporter_id` (`reporter_id`) COMMENT '发起人查本人异常',
  KEY `idx_type_time` (`andon_type_id`, `create_time`) COMMENT '按类型统计异常趋势',
  KEY `idx_batch_no` (`batch_no`(20)) COMMENT '批次追溯查异常'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '现场生产异常表';

DROP TABLE IF EXISTS `andon_handle_record`;
CREATE TABLE `andon_handle_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `exception_id` bigint unsigned NOT NULL COMMENT '异常单',
  `action_type` tinyint unsigned NOT NULL COMMENT '动作:1确认 2处理 3转派 4升级 5关闭',
  `handler_id` bigint unsigned NOT NULL COMMENT '操作人',
  `transfer_to` bigint unsigned NULL DEFAULT NULL COMMENT '转派对象(转派必填原因)',
  `action_remark` varchar(500) NULL DEFAULT NULL COMMENT '处理措施/转派原因/关闭说明',
  `cost_minutes` int unsigned NULL DEFAULT NULL COMMENT '处理耗时(分钟)',
  `action_time` datetime NOT NULL COMMENT '操作时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_exception_id` (`exception_id`, `action_time`) COMMENT '异常处理全过程履历'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '异常处理记录表';


-- ============================== 接口管理 ==============================

DROP TABLE IF EXISTS `api_sync_record`;
CREATE TABLE `api_sync_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sync_type` tinyint unsigned NOT NULL COMMENT '同步类型:1工单读取 2工艺读取 3单位写入 4工单写入 5任务写入 6设备计数写入 7完工单读取',
  `source_system` varchar(32) NOT NULL COMMENT '来源/目标系统(ERP/WMS/设备系统)',
  `source_no` varchar(64) NOT NULL COMMENT '外部来源单号(同类型+来源唯一防重复生成)',
  `target_table` varchar(64) NULL DEFAULT NULL COMMENT '目标业务表',
  `target_id` bigint unsigned NULL DEFAULT NULL COMMENT '生成的MES单据ID',
  `sync_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '状态:0待处理 1成功 2失败 3重复跳过',
  `error_msg` varchar(500) NULL DEFAULT NULL COMMENT '失败原因(缺字段/重复/校验失败)',
  `sync_time` datetime NOT NULL COMMENT '同步时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_source_no` (`sync_type`, `source_system`, `source_no`(32)) COMMENT '外部单号防重复同步(业务强制)',
  KEY `idx_status_time` (`sync_status`, `sync_time`) COMMENT '失败数据重处理扫描'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '外部系统同步记录表';

DROP TABLE IF EXISTS `api_log`;
CREATE TABLE `api_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `api_code` varchar(64) NOT NULL COMMENT '接口编码',
  `source_system` varchar(32) NOT NULL COMMENT '调用方系统',
  `direction` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '方向:1外部写入 2外部读取',
  `biz_no` varchar(64) NULL DEFAULT NULL COMMENT '业务单号',
  `result_status` tinyint unsigned NOT NULL COMMENT '结果:1成功 2失败',
  `error_msg` varchar(500) NULL DEFAULT NULL COMMENT '错误信息',
  `cost_ms` int unsigned NULL DEFAULT NULL COMMENT '耗时(毫秒)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调用时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_api_time` (`api_code`(20), `create_time`) COMMENT '按接口+时间查日志',
  KEY `idx_biz_no` (`biz_no`(20)) COMMENT '按业务单号查调用记录'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '接口调用日志表';


-- ============================== 报表分析 ==============================

DROP TABLE IF EXISTS `rpt_workshop_period`;
CREATE TABLE `rpt_workshop_period` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stat_type` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '统计类型:1日 2周 3月(口径一致)',
  `stat_date` date NOT NULL COMMENT '统计起始日期',
  `workshop_id` bigint unsigned NOT NULL COMMENT '车间',
  `plan_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '计划数量',
  `output_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '实际产量',
  `achieve_rate` decimal(5,2) NULL DEFAULT NULL COMMENT '计划达成率(%)',
  `defect_rate` decimal(5,2) NULL DEFAULT NULL COMMENT '不良率(%)',
  `first_pass_rate` decimal(5,2) NULL DEFAULT NULL COMMENT '直通率(%)',
  `exception_count` int unsigned NOT NULL DEFAULT 0 COMMENT '异常次数',
  `stop_minutes` int unsigned NOT NULL DEFAULT 0 COMMENT '停线时长(分钟;来自异常/设备状态)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_date_workshop` (`stat_type`, `stat_date`, `workshop_id`) COMMENT '统计维度唯一'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '车间生产时段统计表';

