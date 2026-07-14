-- ----------------------------------------------------------------------------
-- V9 计件工资：计件规则、审核报工快照、工资结算、明细与审计日志
-- 金额统一按万分之一元整数保存；不创建数据库外键。
-- ----------------------------------------------------------------------------

CREATE TABLE `wage_piece_rate_rule` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `process_id` bigint unsigned NOT NULL COMMENT '工序主键',
  `product_id` bigint unsigned NULL DEFAULT NULL COMMENT '产品主键;NULL表示工序通用规则',
  `unit_price_basis` bigint unsigned NOT NULL COMMENT '计件单价;单位万分之一元',
  `defect_deduction_rate` int unsigned NOT NULL DEFAULT 0 COMMENT '不良扣减率基点:10000=100.00%',
  `effective_start` date NOT NULL COMMENT '生效开始日期',
  `effective_end` date NULL DEFAULT NULL COMMENT '生效结束日期;NULL表示长期',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  `version` int unsigned NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `update_by` bigint unsigned NOT NULL COMMENT '最后修改人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `active_rule_signature` varchar(96) GENERATED ALWAYS AS
    (CASE WHEN `is_deleted` = 0 THEN CONCAT(`process_id`, ':', COALESCE(`product_id`, 0), ':', `effective_start`)
      ELSE NULL END) STORED COMMENT '有效规则起始日唯一签名',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_active_rule_signature` (`active_rule_signature`) COMMENT '同维度同生效日唯一',
  KEY `idx_rule_lookup`
    (`process_id`, `product_id`, `status`, `is_deleted`, `effective_start`, `effective_end`)
    COMMENT '按工序产品日期匹配规则'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '工序计件规则';

CREATE TABLE `wage_rule_change_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rule_id` bigint unsigned NOT NULL COMMENT '计件规则主键',
  `change_type` varchar(32) NOT NULL COMMENT '变更类型:CREATE/UPDATE/STATUS/DELETE',
  `before_snapshot` json NULL COMMENT '变更前快照',
  `after_snapshot` json NULL COMMENT '变更后快照',
  `change_reason` varchar(255) NULL DEFAULT NULL COMMENT '变更原因',
  `operate_by` bigint unsigned NOT NULL COMMENT '操作人',
  `operate_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_rule_deleted_id` (`rule_id`, `is_deleted`, `id`) COMMENT '规则变更日志分页'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '计件规则变更日志';

CREATE TABLE `wage_work_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_report_id` bigint unsigned NOT NULL COMMENT '来源生产报工主键',
  `employee_id` bigint unsigned NOT NULL COMMENT '员工用户主键',
  `work_date` date NOT NULL COMMENT '作业日期',
  `work_order_id` bigint unsigned NOT NULL COMMENT '生产工单主键',
  `process_id` bigint unsigned NOT NULL COMMENT '工序主键',
  `product_id` bigint unsigned NOT NULL COMMENT '产品主键',
  `qualified_quantity` decimal(12,4) NOT NULL COMMENT '审核合格数量',
  `defect_quantity` decimal(12,4) NOT NULL DEFAULT 0 COMMENT '审核不良数量',
  `source_audit_time` datetime NOT NULL COMMENT '来源报工审核时间',
  `create_by` bigint unsigned NOT NULL COMMENT '导入人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_source_report_id` (`source_report_id`) COMMENT '来源报工幂等',
  KEY `idx_work_date_deleted_id` (`work_date`, `is_deleted`, `id`) COMMENT '按日期选择计件来源',
  KEY `idx_employee_work_date` (`employee_id`, `work_date`, `is_deleted`, `id`) COMMENT '员工日期查询',
  KEY `idx_process_product_date`
    (`process_id`, `product_id`, `work_date`, `is_deleted`) COMMENT '工序产品日期查询'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '已审核报工计件快照';

CREATE TABLE `wage_settlement` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `settlement_no` varchar(32) NOT NULL COMMENT '结算批次号',
  `period_start` date NOT NULL COMMENT '结算开始日期',
  `period_end` date NOT NULL COMMENT '结算结束日期',
  `employee_scope` json NULL COMMENT '参与计算的员工主键范围;NULL表示全部员工',
  `settlement_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '状态:0草稿 1待审核 2已审核 3已驳回',
  `total_qualified_quantity` decimal(14,4) NOT NULL DEFAULT 0 COMMENT '合格数量合计',
  `total_defect_quantity` decimal(14,4) NOT NULL DEFAULT 0 COMMENT '不良数量合计',
  `total_amount_basis` bigint unsigned NOT NULL DEFAULT 0 COMMENT '最终金额合计;万分之一元',
  `version` int unsigned NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `submit_by` bigint unsigned NULL DEFAULT NULL COMMENT '提交人',
  `submit_time` datetime NULL DEFAULT NULL COMMENT '提交时间',
  `audit_by` bigint unsigned NULL DEFAULT NULL COMMENT '审核人',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `audit_reason` varchar(255) NULL DEFAULT NULL COMMENT '审核意见',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `update_by` bigint unsigned NOT NULL COMMENT '最后修改人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_settlement_no` (`settlement_no`) COMMENT '结算批次号唯一',
  KEY `idx_status_period_id`
    (`settlement_status`, `is_deleted`, `period_start`, `period_end`, `id`) COMMENT '状态日期分页'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '计件工资结算批次';

CREATE TABLE `wage_settlement_detail` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `settlement_id` bigint unsigned NOT NULL COMMENT '结算批次主键',
  `work_record_id` bigint unsigned NOT NULL COMMENT '报工快照主键',
  `rule_id` bigint unsigned NOT NULL COMMENT '计件规则主键',
  `employee_id` bigint unsigned NOT NULL COMMENT '员工用户主键',
  `work_date` date NOT NULL COMMENT '作业日期',
  `work_order_id` bigint unsigned NOT NULL COMMENT '生产工单主键',
  `process_id` bigint unsigned NOT NULL COMMENT '工序主键',
  `product_id` bigint unsigned NOT NULL COMMENT '产品主键',
  `qualified_quantity` decimal(12,4) NOT NULL COMMENT '合格数量快照',
  `defect_quantity` decimal(12,4) NOT NULL COMMENT '不良数量快照',
  `unit_price_basis` bigint unsigned NOT NULL COMMENT '单价快照;万分之一元',
  `defect_deduction_rate` int unsigned NOT NULL COMMENT '不良扣减率快照',
  `calculated_amount_basis` bigint unsigned NOT NULL COMMENT '系统计算金额;万分之一元',
  `adjusted_amount_basis` bigint unsigned NULL DEFAULT NULL COMMENT '人工调整金额;万分之一元',
  `final_amount_basis` bigint unsigned NOT NULL COMMENT '最终金额;万分之一元',
  `is_active` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '是否占用来源记录:1是 0否',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `active_work_record_id` bigint unsigned GENERATED ALWAYS AS
    (CASE WHEN `is_active` = 1 AND `is_deleted` = 0 THEN `work_record_id` ELSE NULL END) STORED
    COMMENT '有效来源记录唯一键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_active_work_record_id` (`active_work_record_id`) COMMENT '来源记录不得重复计件',
  KEY `idx_settlement_deleted_id` (`settlement_id`, `is_deleted`, `id`) COMMENT '结算明细分页',
  KEY `idx_employee_date_settlement`
    (`employee_id`, `work_date`, `is_deleted`, `settlement_id`) COMMENT '员工绩效汇总',
  KEY `idx_process_date_settlement`
    (`process_id`, `work_date`, `is_deleted`, `settlement_id`) COMMENT '工序金额汇总'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '计件工资结算明细';

CREATE TABLE `wage_settlement_audit_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `settlement_id` bigint unsigned NOT NULL COMMENT '结算批次主键',
  `detail_id` bigint unsigned NULL DEFAULT NULL COMMENT '调整明细主键',
  `action_type` varchar(32) NOT NULL COMMENT '动作:CALCULATE/RECALCULATE/SUBMIT/APPROVE/REJECT/ADJUST',
  `from_status` tinyint unsigned NULL DEFAULT NULL COMMENT '变更前状态',
  `to_status` tinyint unsigned NULL DEFAULT NULL COMMENT '变更后状态',
  `before_amount_basis` bigint unsigned NULL DEFAULT NULL COMMENT '调整前金额',
  `after_amount_basis` bigint unsigned NULL DEFAULT NULL COMMENT '调整后金额',
  `action_reason` varchar(255) NULL DEFAULT NULL COMMENT '操作原因',
  `operate_by` bigint unsigned NOT NULL COMMENT '操作人',
  `operate_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_settlement_deleted_id` (`settlement_id`, `is_deleted`, `id`) COMMENT '结算审计日志分页'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '计件工资结算审计日志';
