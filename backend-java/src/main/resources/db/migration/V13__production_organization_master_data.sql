-- ----------------------------------------------------------------------------
-- V13 生产组织基础资料：车间、产线审计、乐观锁及逻辑删除唯一键
-- ----------------------------------------------------------------------------

ALTER TABLE `base_workshop`
  ADD COLUMN `create_by` bigint unsigned NULL DEFAULT NULL COMMENT '创建人' AFTER `status`,
  ADD COLUMN `update_by` bigint unsigned NULL DEFAULT NULL COMMENT '最后修改人' AFTER `create_by`,
  ADD COLUMN `version` int unsigned NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER `update_by`,
  ADD COLUMN `active_workshop_code` varchar(32) GENERATED ALWAYS AS
    (CASE WHEN `is_deleted` = 0 THEN `workshop_code` ELSE NULL END) STORED
    COMMENT '有效车间编码唯一键' AFTER `is_deleted`;

UPDATE `base_workshop`
SET `create_by` = 1, `update_by` = 1
WHERE `create_by` IS NULL OR `update_by` IS NULL;

ALTER TABLE `base_workshop`
  MODIFY COLUMN `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  MODIFY COLUMN `update_by` bigint unsigned NOT NULL COMMENT '最后修改人',
  DROP INDEX `uk_workshop_code`,
  ADD UNIQUE KEY `uk_active_workshop_code`
    (`active_workshop_code`) COMMENT '有效车间编码唯一',
  ADD KEY `idx_workshop_code_deleted_id`
    (`workshop_code`, `is_deleted`, `id`) COMMENT '车间编码查重与前缀查询',
  ADD KEY `idx_workshop_status_id`
    (`status`, `is_deleted`, `id`) COMMENT '车间状态分页';

ALTER TABLE `base_production_line`
  ADD COLUMN `create_by` bigint unsigned NULL DEFAULT NULL COMMENT '创建人' AFTER `status`,
  ADD COLUMN `update_by` bigint unsigned NULL DEFAULT NULL COMMENT '最后修改人' AFTER `create_by`,
  ADD COLUMN `version` int unsigned NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER `update_by`,
  ADD COLUMN `active_line_code` varchar(32) GENERATED ALWAYS AS
    (CASE WHEN `is_deleted` = 0 THEN `line_code` ELSE NULL END) STORED
    COMMENT '有效产线编码唯一键' AFTER `is_deleted`;

UPDATE `base_production_line`
SET `create_by` = 1, `update_by` = 1
WHERE `create_by` IS NULL OR `update_by` IS NULL;

ALTER TABLE `base_production_line`
  MODIFY COLUMN `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  MODIFY COLUMN `update_by` bigint unsigned NOT NULL COMMENT '最后修改人',
  DROP INDEX `uk_line_code`,
  DROP INDEX `idx_workshop_id`,
  ADD UNIQUE KEY `uk_active_line_code`
    (`active_line_code`) COMMENT '有效产线编码唯一',
  ADD KEY `idx_line_code_deleted_id`
    (`line_code`, `is_deleted`, `id`) COMMENT '产线编码查重与前缀查询',
  ADD KEY `idx_line_workshop_status_id`
    (`workshop_id`, `status`, `is_deleted`, `id`) COMMENT '车间产线查询与引用校验';
