-- ----------------------------------------------------------------------------
-- V18 A 组 Review 修复：任务进度、完工约束与设备异常重试审计
-- ----------------------------------------------------------------------------

ALTER TABLE `scene_process_task`
  ADD COLUMN `qualified_quantity` decimal(12,4) NOT NULL DEFAULT 0
    COMMENT '已审核合格数量' AFTER `task_status`,
  ADD COLUMN `defect_quantity` decimal(12,4) NOT NULL DEFAULT 0
    COMMENT '已审核不良数量' AFTER `qualified_quantity`;

ALTER TABLE `scene_production_task`
  ADD COLUMN `qualified_quantity` decimal(12,4) NOT NULL DEFAULT 0
    COMMENT '末工序已审核合格数量' AFTER `task_status`,
  ADD COLUMN `defect_quantity` decimal(12,4) NOT NULL DEFAULT 0
    COMMENT '末工序已审核不良数量' AFTER `qualified_quantity`;

ALTER TABLE `prod_completion_order`
  ADD COLUMN `production_task_id` bigint unsigned NULL DEFAULT NULL
    COMMENT '现场生产任务主键，历史完工单可空' AFTER `completion_no`,
  ADD KEY `idx_completion_task_status_id`
    (`production_task_id`, `audit_status`, `id`) COMMENT '按任务汇总已审核完工数量';

ALTER TABLE `integration_device_count_exception`
  ADD COLUMN `retry_request_snapshot` json NULL DEFAULT NULL
    COMMENT '最后一次修正重试请求快照' AFTER `request_snapshot`,
  ADD COLUMN `retry_log_id` bigint unsigned NULL DEFAULT NULL
    COMMENT '修正后写入日志主键' AFTER `handle_remark`,
  ADD COLUMN `retry_record_id` bigint unsigned NULL DEFAULT NULL
    COMMENT '修正后设备计数记录主键' AFTER `retry_log_id`;
