-- ----------------------------------------------------------------------------
-- V15 设备计数报工写入：成功记录、异常池及接口专用幂等唯一键
-- ----------------------------------------------------------------------------

CREATE TABLE `integration_device_count_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_system` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '来源系统',
  `external_key` varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '来源系统内幂等键（统一大写）',
  `equipment_code` varchar(32) NOT NULL COMMENT '设备编码',
  `dispatch_order_id` bigint unsigned NOT NULL COMMENT '匹配派工单主键',
  `dispatch_no` varchar(32) NOT NULL COMMENT '匹配派工单号',
  `process_id` bigint unsigned NOT NULL COMMENT '匹配工序主键',
  `process_code` varchar(32) NOT NULL COMMENT '匹配工序编码',
  `collect_time` datetime NOT NULL COMMENT '设备采集时间',
  `count_value` bigint unsigned NOT NULL COMMENT '设备累计计数值',
  `increment_value` bigint unsigned NOT NULL COMMENT '相对最近记录的增量值',
  `create_by` bigint unsigned NOT NULL COMMENT '调用用户',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_count_source_external`
    (`source_system`, `external_key`) COMMENT '来源系统内计数请求唯一',
  KEY `idx_device_count_context_id`
    (`source_system`, `equipment_code`, `dispatch_order_id`, `process_id`, `id`)
    COMMENT '按设备任务工序查询最近累计计数',
  KEY `idx_device_count_dispatch_process`
    (`dispatch_order_id`, `process_id`, `id`) COMMENT '按派工单工序查询计数'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '设备计数成功记录';

CREATE TABLE `integration_device_count_exception` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_system` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '来源系统',
  `external_key` varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '来源系统内幂等键（统一大写）',
  `equipment_code` varchar(32) NOT NULL COMMENT '设备编码',
  `dispatch_order_id` bigint unsigned NULL DEFAULT NULL COMMENT '匹配派工单主键',
  `dispatch_no` varchar(32) NOT NULL COMMENT '上报派工单号',
  `process_id` bigint unsigned NULL DEFAULT NULL COMMENT '匹配工序主键',
  `process_code` varchar(32) NOT NULL COMMENT '上报工序编码',
  `collect_time` datetime NOT NULL COMMENT '设备采集时间',
  `count_value` bigint NOT NULL COMMENT '原始累计计数值，可保留非法负数',
  `request_snapshot` json NOT NULL COMMENT '原始请求快照',
  `exception_type` varchar(32) NOT NULL COMMENT '异常类型',
  `error_code` varchar(5) NOT NULL COMMENT '异常错误码',
  `error_message` varchar(512) NOT NULL COMMENT '异常原因',
  `handle_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '处理状态:0待处理 1已处理 2已忽略',
  `create_by` bigint unsigned NOT NULL COMMENT '调用用户',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_exception_source_external`
    (`source_system`, `external_key`) COMMENT '来源系统内异常请求唯一',
  KEY `idx_device_exception_handle_type_id`
    (`handle_status`, `exception_type`, `id`) COMMENT '异常池稳定分页',
  KEY `idx_device_exception_equipment_id`
    (`equipment_code`, `id`) COMMENT '按设备查询异常'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '设备计数异常池';

-- 既有写入接口允许重复请求继续追加 DUPLICATE 日志，因此只对设备计数接口
-- 生成唯一值；其他接口生成 NULL，不改变其现有日志契约。
ALTER TABLE `integration_write_log`
  ADD COLUMN `device_count_idempotency_key` varchar(97)
    CHARACTER SET ascii COLLATE ascii_bin GENERATED ALWAYS AS
    (CASE WHEN `interface_type` = 'DEVICE_COUNT_WRITE'
      THEN UPPER(CONCAT(`source_system`, '#', `business_key`)) ELSE NULL END) STORED
    COMMENT '设备计数接口幂等唯一键' AFTER `business_key`,
  ADD UNIQUE KEY `uk_device_count_idempotency_key`
    (`device_count_idempotency_key`) COMMENT '设备计数只处理一次';
