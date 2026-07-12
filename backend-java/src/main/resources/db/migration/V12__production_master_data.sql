-- ----------------------------------------------------------------------------
-- V12 生产基础资料：产品、物料与 BOM 的审计、乐观锁及逻辑删除唯一键
-- ----------------------------------------------------------------------------

ALTER TABLE `base_product`
  ADD COLUMN `create_by` bigint unsigned NULL DEFAULT NULL COMMENT '创建人' AFTER `status`,
  ADD COLUMN `update_by` bigint unsigned NULL DEFAULT NULL COMMENT '最后修改人' AFTER `create_by`,
  ADD COLUMN `version` int unsigned NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER `update_by`,
  ADD COLUMN `active_product_code` varchar(32) GENERATED ALWAYS AS
    (CASE WHEN `is_deleted` = 0 THEN `product_code` ELSE NULL END) STORED
    COMMENT '有效产品编码唯一键' AFTER `is_deleted`;

UPDATE `base_product`
SET `create_by` = 1, `update_by` = 1
WHERE `create_by` IS NULL OR `update_by` IS NULL;

ALTER TABLE `base_product`
  MODIFY COLUMN `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  MODIFY COLUMN `update_by` bigint unsigned NOT NULL COMMENT '最后修改人',
  DROP INDEX `uk_product_code`,
  ADD UNIQUE KEY `uk_active_product_code` (`active_product_code`) COMMENT '有效产品编码唯一',
  ADD KEY `idx_product_code_deleted_id`
    (`product_code`, `is_deleted`, `id`) COMMENT '产品编码查重与前缀查询',
  ADD KEY `idx_product_status_id` (`status`, `is_deleted`, `id`) COMMENT '产品状态分页';

ALTER TABLE `base_material`
  ADD COLUMN `create_by` bigint unsigned NULL DEFAULT NULL COMMENT '创建人' AFTER `status`,
  ADD COLUMN `update_by` bigint unsigned NULL DEFAULT NULL COMMENT '最后修改人' AFTER `create_by`,
  ADD COLUMN `version` int unsigned NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER `update_by`,
  ADD COLUMN `active_material_code` varchar(32) GENERATED ALWAYS AS
    (CASE WHEN `is_deleted` = 0 THEN `material_code` ELSE NULL END) STORED
    COMMENT '有效物料编码唯一键' AFTER `is_deleted`;

UPDATE `base_material`
SET `create_by` = 1, `update_by` = 1
WHERE `create_by` IS NULL OR `update_by` IS NULL;

ALTER TABLE `base_material`
  MODIFY COLUMN `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  MODIFY COLUMN `update_by` bigint unsigned NOT NULL COMMENT '最后修改人',
  DROP INDEX `uk_material_code`,
  ADD UNIQUE KEY `uk_active_material_code` (`active_material_code`) COMMENT '有效物料编码唯一',
  ADD KEY `idx_material_code_deleted_id`
    (`material_code`, `is_deleted`, `id`) COMMENT '物料编码查重与前缀查询',
  ADD KEY `idx_material_status_type_id`
    (`status`, `material_type`, `is_deleted`, `id`) COMMENT '物料状态类型分页';

ALTER TABLE `base_bom`
  ADD COLUMN `update_by` bigint unsigned NULL DEFAULT NULL COMMENT '最后修改人' AFTER `create_by`,
  ADD COLUMN `lock_version` int unsigned NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER `update_by`,
  ADD COLUMN `active_bom_code` varchar(32) GENERATED ALWAYS AS
    (CASE WHEN `is_deleted` = 0 THEN `bom_code` ELSE NULL END) STORED
    COMMENT '有效BOM编码唯一键' AFTER `is_deleted`,
  ADD COLUMN `active_product_version` varchar(80) GENERATED ALWAYS AS
    (CASE WHEN `is_deleted` = 0 THEN CONCAT(`product_id`, ':', `version`) ELSE NULL END) STORED
    COMMENT '有效产品版本唯一键' AFTER `active_bom_code`;

UPDATE `base_bom`
SET `update_by` = `create_by`
WHERE `update_by` IS NULL;

ALTER TABLE `base_bom`
  MODIFY COLUMN `update_by` bigint unsigned NOT NULL COMMENT '最后修改人',
  DROP INDEX `uk_product_version`,
  DROP INDEX `uk_bom_code`,
  ADD UNIQUE KEY `uk_active_bom_code` (`active_bom_code`) COMMENT '有效BOM编码唯一',
  ADD UNIQUE KEY `uk_active_product_version`
    (`active_product_version`) COMMENT '有效产品BOM版本唯一',
  ADD KEY `idx_bom_code_deleted_id`
    (`bom_code`, `is_deleted`, `id`) COMMENT 'BOM编码查重与前缀查询',
  ADD KEY `idx_bom_product_version_deleted`
    (`product_id`, `version`, `is_deleted`) COMMENT '产品BOM版本查重',
  ADD KEY `idx_bom_product_status_id`
    (`product_id`, `bom_status`, `is_deleted`, `id`) COMMENT '产品BOM版本分页';

ALTER TABLE `base_bom_detail`
  ADD COLUMN `active_bom_material` varchar(80) GENERATED ALWAYS AS
    (CASE WHEN `is_deleted` = 0 THEN CONCAT(`bom_id`, ':', `material_id`) ELSE NULL END) STORED
    COMMENT '有效BOM物料唯一键' AFTER `is_deleted`,
  DROP INDEX `uk_bom_material`,
  ADD UNIQUE KEY `uk_active_bom_material`
    (`active_bom_material`) COMMENT '有效BOM内物料唯一',
  ADD KEY `idx_bom_deleted_id` (`bom_id`, `is_deleted`, `id`) COMMENT 'BOM明细查询';

-- BOM 草稿删除前需按主键判断工单历史引用，避免核心大表全表扫描。
ALTER TABLE `prod_work_order`
  ADD KEY `idx_bom_id` (`bom_id`, `is_deleted`) COMMENT '按BOM反查生产工单';

-- 产品停用/删除会反查产品维度计件规则，原索引以 process_id 开头无法支撑该查询。
ALTER TABLE `wage_piece_rate_rule`
  ADD KEY `idx_product_status_deleted`
    (`product_id`, `status`, `is_deleted`, `id`) COMMENT '产品引用完整性校验';
