-- ----------------------------------------------------------------------------
-- V8 外部写入接口：计量单位主档、来源级工单幂等键、接口写入日志
-- ----------------------------------------------------------------------------

CREATE TABLE `base_unit` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `unit_code` varchar(32) NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) NOT NULL COMMENT '单位名称',
  `decimal_precision` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '数量小数精度:0-6',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  `version` int unsigned NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `update_by` bigint unsigned NOT NULL COMMENT '最后修改人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `active_unit_code` varchar(32) GENERATED ALWAYS AS
    (CASE WHEN `is_deleted` = 0 THEN `unit_code` ELSE NULL END) STORED COMMENT '有效单位编码唯一键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_active_unit_code` (`active_unit_code`) COMMENT '有效单位编码唯一'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '计量单位表';

-- V1 产品种子数据使用 unit_id=1，占位值在单位主档落地后由该种子承接。
INSERT INTO `base_unit`
  (`unit_code`, `unit_name`, `decimal_precision`, `status`, `create_by`, `update_by`)
VALUES
  ('PCS', '个', 0, 1, 1, 1);

-- 原唯一键只按 source_type + 外部单号前 32 字符判重，既无法区分来源系统，
-- 也会让长单号前缀相同的合法请求产生碰撞。改为完整来源级幂等键。
ALTER TABLE `prod_work_order`
  ADD COLUMN `source_system` varchar(32) NULL DEFAULT NULL COMMENT '外部来源系统' AFTER `source_type`,
  ADD COLUMN `source_system_key` varchar(32) GENERATED ALWAYS AS
    (COALESCE(`source_system`, '')) STORED COMMENT '来源系统唯一键(NULL按空串参与唯一约束)'
    AFTER `source_system`,
  DROP INDEX `uk_source_order`,
  ADD UNIQUE KEY `uk_external_source_order`
    (`source_type`, `source_system_key`, `source_order_no`) COMMENT '来源系统内外部工单号唯一';

CREATE TABLE `integration_write_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `interface_type` varchar(32) NOT NULL COMMENT '接口类型:UNIT_WRITE/WORK_ORDER_WRITE',
  `source_system` varchar(32) NOT NULL COMMENT '来源系统',
  `business_key` varchar(64) NOT NULL COMMENT '来源侧业务键',
  `request_snapshot` json NOT NULL COMMENT '请求快照',
  `write_status` tinyint unsigned NOT NULL COMMENT '处理状态:1成功 2失败 3重复',
  `result_id` bigint unsigned NULL DEFAULT NULL COMMENT 'MES 业务主键',
  `result_no` varchar(64) NULL DEFAULT NULL COMMENT 'MES 业务编号',
  `error_code` varchar(5) NULL DEFAULT NULL COMMENT '失败错误码',
  `error_message` varchar(512) NULL DEFAULT NULL COMMENT '失败原因',
  `create_by` bigint unsigned NOT NULL COMMENT '调用用户',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_interface_source_key_id`
    (`interface_type`, `source_system`, `business_key`, `id`) COMMENT '按来源业务键查询写入结果',
  KEY `idx_status_id` (`write_status`, `id`) COMMENT '按处理状态分页查询'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '外部接口写入日志';
