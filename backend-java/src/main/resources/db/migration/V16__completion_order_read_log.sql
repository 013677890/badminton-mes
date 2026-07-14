-- ----------------------------------------------------------------------------
-- V16 生产完工单读取：B 组写入的完工单主表及 A 组逐条读取日志
-- ----------------------------------------------------------------------------

CREATE TABLE `prod_completion_order` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `completion_no` varchar(32) NOT NULL COMMENT '完工单号',
  `work_order_id` bigint unsigned NOT NULL COMMENT '生产工单主键',
  `work_order_no` varchar(32) NOT NULL COMMENT '生产工单号',
  `product_id` bigint unsigned NOT NULL COMMENT '产品主键',
  `product_code` varchar(32) NOT NULL COMMENT '产品编码',
  `product_name` varchar(128) NOT NULL COMMENT '产品名称',
  `batch_no` varchar(64) NOT NULL COMMENT '产品批次号',
  `completion_quantity` int unsigned NOT NULL COMMENT '完工数量',
  `good_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '良品数量',
  `defect_quantity` int unsigned NOT NULL DEFAULT 0 COMMENT '不良数量',
  `audit_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '审核状态:0待审核 1已审核 2已作废',
  `audit_by` bigint unsigned NULL DEFAULT NULL COMMENT '审核人',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(255) NULL DEFAULT NULL COMMENT '审核意见',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `update_by` bigint unsigned NOT NULL COMMENT '最后修改人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  CONSTRAINT `chk_completion_approved_audit_time`
    CHECK (`audit_status` <> 1 OR `audit_time` IS NOT NULL),
  UNIQUE KEY `uk_completion_no` (`completion_no`) COMMENT '完工单号唯一',
  KEY `idx_completion_audit_time_id`
    (`audit_status`, `audit_time`, `id`) COMMENT '按审核状态与时间读取完工单',
  KEY `idx_completion_work_order_status_id`
    (`work_order_no`, `audit_status`, `id`) COMMENT '按工单号读取已审核完工单',
  KEY `idx_completion_batch_no` (`batch_no`(20)) COMMENT '按产品批次追溯'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '生产完工单';

CREATE TABLE `integration_completion_read_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `completion_order_id` bigint unsigned NOT NULL COMMENT '被读取完工单主键',
  `completion_no` varchar(32) NOT NULL COMMENT '被读取完工单号',
  `work_order_no` varchar(32) NOT NULL COMMENT '生产工单号',
  `source_system` varchar(32) NOT NULL COMMENT '读取来源系统',
  `read_by` bigint unsigned NOT NULL COMMENT '调用用户',
  `read_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '读取时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_completion_read_source_time_id`
    (`source_system`, `read_time`, `id`) COMMENT '按来源与读取时间查询日志',
  KEY `idx_completion_read_no_id`
    (`completion_no`, `id`) COMMENT '按完工单号查询读取日志',
  KEY `idx_completion_read_order_id`
    (`completion_order_id`, `id`) COMMENT '按完工单主键追踪读取事件'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '生产完工单读取日志';
