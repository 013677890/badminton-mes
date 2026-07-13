-- A/B/C 共享工艺依赖迁移。
-- 结构来源：badminton-mes.wiki/database/mes_schema.sql 的已冻结工艺契约。
-- 本迁移仅落地共享表，不写入演示数据，不修改历史迁移。

CREATE TABLE `craft_process` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `process_code` varchar(32) NOT NULL COMMENT '工序编码(唯一)',
    `process_name` varchar(64) NOT NULL COMMENT '工序名称',
    `process_type` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '工序类型:1加工 2检验 3包装',
    `standard_hour` decimal(8,2) NULL DEFAULT NULL COMMENT '标准工时(秒/只)',
    `is_key` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否关键工序:1是 0否',
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
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工序主表';

CREATE TABLE `craft_process_defect` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `process_id` bigint unsigned NOT NULL COMMENT '工序',
    `defect_code` varchar(32) NOT NULL COMMENT '不良原因编码(工序内唯一)',
    `defect_name` varchar(64) NOT NULL COMMENT '不良原因名称',
    `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_process_defect_code` (`process_id`, `defect_code`) COMMENT '工序内不良编码唯一'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工序不良原因表';

CREATE TABLE `craft_sop` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `sop_code` varchar(32) NOT NULL COMMENT 'SOP编码',
    `sop_name` varchar(128) NOT NULL COMMENT 'SOP名称',
    `version` varchar(16) NOT NULL DEFAULT 'V1' COMMENT '版本(编码+版本唯一)',
    `effect_date` date NULL DEFAULT NULL COMMENT '生效日期',
    `sop_status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1生效 0停用',
    `create_by` bigint unsigned NOT NULL COMMENT '创建人',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code_version` (`sop_code`, `version`) COMMENT 'SOP编码+版本唯一'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'SOP主表';

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
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'SOP文件表';

CREATE TABLE `craft_routing` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `routing_code` varchar(32) NOT NULL COMMENT '路线编码',
    `routing_name` varchar(128) NOT NULL COMMENT '路线名称',
    `version` varchar(16) NOT NULL DEFAULT 'V1' COMMENT '路线版本(编码+版本唯一)',
    `source_type` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '来源:1本地创建 2ERP读取确认',
    `routing_status` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '状态:0草稿 1生效 2停用',
    `audit_by` bigint unsigned NULL DEFAULT NULL COMMENT '审核人',
    `audit_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
    `create_by` bigint unsigned NOT NULL COMMENT '创建人',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code_version` (`routing_code`, `version`) COMMENT '路线编码+版本唯一'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工艺路线主表';

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
    KEY `idx_process_id` (`process_id`) COMMENT '工序被引用校验'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工艺路线明细表';

CREATE TABLE `craft_routing_product` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `routing_id` bigint unsigned NOT NULL COMMENT '工艺路线',
    `product_id` bigint unsigned NOT NULL COMMENT '产品',
    `is_default` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '是否产品默认路线:1是 0否',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_routing_product` (`routing_id`, `product_id`) COMMENT '绑定关系唯一',
    KEY `idx_product_default` (`product_id`, `is_default`) COMMENT '按产品取默认路线'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '路线产品关系表';
