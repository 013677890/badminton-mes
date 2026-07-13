-- ----------------------------------------------------------------------------
-- V17 A 组业务闭环：现场任务、工序任务、报工、设备绑定与异常处理扩展
-- ----------------------------------------------------------------------------

CREATE TABLE `scene_production_task` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_no` varchar(32) NOT NULL COMMENT '生产任务单号，复用派工单号',
  `dispatch_order_id` bigint unsigned NOT NULL COMMENT '来源派工单主键',
  `work_order_id` bigint unsigned NOT NULL COMMENT '来源生产工单主键',
  `routing_id` bigint unsigned NOT NULL COMMENT '下发时工艺路线主键快照',
  `line_id` bigint unsigned NOT NULL COMMENT '产线主键',
  `shift_id` bigint unsigned NOT NULL COMMENT '班次主键',
  `plan_quantity` int unsigned NOT NULL COMMENT '计划数量',
  `task_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '状态:0待执行 1执行中 2已完成 3已取消',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_task_dispatch` (`dispatch_order_id`) COMMENT '一张派工单只生成一个现场任务',
  UNIQUE KEY `uk_scene_task_no` (`task_no`) COMMENT '生产任务单号唯一',
  KEY `idx_scene_task_line_status_id` (`line_id`, `task_status`, `id`),
  KEY `idx_scene_task_work_order_id` (`work_order_id`, `id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '现场生产任务';

CREATE TABLE `scene_process_task` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `production_task_id` bigint unsigned NOT NULL COMMENT '现场生产任务主键',
  `route_detail_id` bigint unsigned NOT NULL COMMENT '工艺路线明细主键快照',
  `process_id` bigint unsigned NOT NULL COMMENT '工序主键',
  `sequence_no` int unsigned NOT NULL COMMENT '工序顺序',
  `station_id` bigint unsigned NULL DEFAULT NULL COMMENT '默认工位主键',
  `equipment_category_id` bigint unsigned NULL DEFAULT NULL COMMENT '设备类别要求',
  `sop_id` bigint unsigned NULL DEFAULT NULL COMMENT 'SOP 关联主键',
  `is_inspect` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否质检节点',
  `task_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '状态:0待执行 1执行中 2已完成',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_process_task_step` (`production_task_id`, `route_detail_id`),
  KEY `idx_scene_process_task_process_status` (`process_id`, `task_status`, `id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '现场工序任务';

CREATE TABLE `scene_work_report` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `report_no` varchar(32) NOT NULL COMMENT '报工单号',
  `source_type` tinyint unsigned NOT NULL COMMENT '来源:1人工 2设备自动',
  `source_record_id` bigint unsigned NULL DEFAULT NULL COMMENT '来源设备计数记录主键',
  `production_task_id` bigint unsigned NOT NULL COMMENT '现场生产任务主键',
  `dispatch_order_id` bigint unsigned NOT NULL COMMENT '派工单主键',
  `work_order_id` bigint unsigned NOT NULL COMMENT '生产工单主键',
  `product_id` bigint unsigned NOT NULL COMMENT '产品主键',
  `process_id` bigint unsigned NOT NULL COMMENT '工序主键',
  `employee_id` bigint unsigned NULL DEFAULT NULL COMMENT '操作员工主键，待确认报工可空',
  `qualified_quantity` decimal(12,4) NOT NULL DEFAULT 0 COMMENT '合格数量',
  `defect_quantity` decimal(12,4) NOT NULL DEFAULT 0 COMMENT '不良数量',
  `report_time` datetime NOT NULL COMMENT '报工时间',
  `audit_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '审核状态:0待确认 1已审核 2已驳回',
  `audit_by` bigint unsigned NULL DEFAULT NULL COMMENT '审核人',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_work_report_no` (`report_no`),
  UNIQUE KEY `uk_scene_work_report_source` (`source_type`, `source_record_id`),
  KEY `idx_scene_work_report_task_status` (`production_task_id`, `audit_status`, `id`),
  KEY `idx_scene_work_report_employee_time` (`employee_id`, `report_time`, `id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '现场生产报工';

CREATE TABLE `integration_equipment_binding` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `equipment_code` varchar(32) NOT NULL COMMENT '设备编码',
  `line_id` bigint unsigned NOT NULL COMMENT '绑定产线主键',
  `process_id` bigint unsigned NULL DEFAULT NULL COMMENT '限定工序主键，可空',
  `default_employee_id` bigint unsigned NULL DEFAULT NULL COMMENT '自动报工默认员工，可空',
  `is_auto_report` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否自动审核报工',
  `max_increment` bigint unsigned NOT NULL DEFAULT 100000 COMMENT '单次允许最大增量',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:0停用 1启用',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `update_by` bigint unsigned NOT NULL COMMENT '最后修改人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_equipment_binding_code` (`equipment_code`),
  KEY `idx_equipment_binding_line_status` (`line_id`, `status`, `id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '设备报工绑定配置';

ALTER TABLE `integration_device_count_record`
  ADD COLUMN `work_report_id` bigint unsigned NULL DEFAULT NULL COMMENT '生成的现场报工主键' AFTER `increment_value`;

ALTER TABLE `integration_device_count_exception`
  ADD COLUMN `handle_by` bigint unsigned NULL DEFAULT NULL COMMENT '处理人' AFTER `handle_status`,
  ADD COLUMN `handle_time` datetime NULL DEFAULT NULL COMMENT '处理时间' AFTER `handle_by`,
  ADD COLUMN `handle_remark` varchar(255) NULL DEFAULT NULL COMMENT '处理说明' AFTER `handle_time`;

ALTER TABLE `erp_craft_pending`
  MODIFY COLUMN `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态:0待确认 1已确认 2异常 3已驳回';

ALTER TABLE `material_stock`
  ADD COLUMN `source_system` varchar(32) NOT NULL DEFAULT 'MANUAL' COMMENT '库存来源系统' AFTER `material_id`;
