-- 质量管理基础数据：检验分类、检验项目和检验方案

CREATE TABLE IF NOT EXISTS quality_inspection_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    category_code VARCHAR(32) NOT NULL COMMENT '分类编码',
    category_name VARCHAR(128) NOT NULL COMMENT '分类名称',
    enabled_status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT NOT NULL COMMENT '创建人用户id',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '逻辑删除标记',
    UNIQUE KEY uk_quality_category_code (category_code),
    INDEX idx_quality_category_status (enabled_status, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质量检验项目分类表';

CREATE TABLE IF NOT EXISTS quality_inspection_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    item_code VARCHAR(32) NOT NULL COMMENT '检验项目编码',
    item_name VARCHAR(128) NOT NULL COMMENT '检验项目名称',
    category_id BIGINT NOT NULL COMMENT '检验分类id',
    value_type VARCHAR(16) NOT NULL COMMENT '值类型：NUMERIC TEXT BOOLEAN',
    unit VARCHAR(32) DEFAULT NULL COMMENT '计量单位',
    standard_value VARCHAR(128) DEFAULT NULL COMMENT '标准值',
    lower_limit DECIMAL(18,6) DEFAULT NULL COMMENT '数值下限',
    upper_limit DECIMAL(18,6) DEFAULT NULL COMMENT '数值上限',
    judgment_method VARCHAR(32) NOT NULL COMMENT '判定方式：RANGE STANDARD_VALUE MANUAL',
    inspection_method VARCHAR(255) DEFAULT NULL COMMENT '检验方法',
    required_flag BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否必检',
    enabled_status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT NOT NULL COMMENT '创建人用户id',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '逻辑删除标记',
    UNIQUE KEY uk_quality_item_code (item_code),
    INDEX idx_quality_item_category (category_id, enabled_status, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质量检验项目表';

CREATE TABLE IF NOT EXISTS quality_inspection_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    plan_code VARCHAR(32) NOT NULL COMMENT '方案编码',
    plan_name VARCHAR(128) NOT NULL COMMENT '方案名称',
    product_id BIGINT DEFAULT NULL COMMENT '适用产品id',
    customer_id BIGINT DEFAULT NULL COMMENT '适用客户id',
    inspection_type VARCHAR(32) NOT NULL COMMENT '检验类型',
    version_no INT NOT NULL DEFAULT 1 COMMENT '版本号',
    plan_status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT EFFECTIVE DISABLED',
    effective_date DATE DEFAULT NULL COMMENT '生效日期',
    default_flag BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否默认方案',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT NOT NULL COMMENT '创建人用户id',
    audit_by BIGINT DEFAULT NULL COMMENT '审核人用户id',
    audit_time DATETIME DEFAULT NULL COMMENT '审核时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '逻辑删除标记',
    UNIQUE KEY uk_quality_plan_code_version (plan_code, version_no),
    INDEX idx_quality_plan_scope (product_id, customer_id, inspection_type, plan_status, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质量检验标准方案表';

CREATE TABLE IF NOT EXISTS quality_inspection_plan_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    plan_id BIGINT NOT NULL COMMENT '检验方案id',
    inspection_item_id BIGINT NOT NULL COMMENT '检验项目id',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    sample_quantity INT NOT NULL DEFAULT 1 COMMENT '抽样数量',
    required_flag BOOLEAN NOT NULL DEFAULT FALSE COMMENT '方案内是否必检',
    standard_value VARCHAR(128) DEFAULT NULL COMMENT '方案标准值快照',
    lower_limit DECIMAL(18,6) DEFAULT NULL COMMENT '方案数值下限快照',
    upper_limit DECIMAL(18,6) DEFAULT NULL COMMENT '方案数值上限快照',
    judgment_method VARCHAR(32) NOT NULL COMMENT '判定方式快照',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_quality_plan_item (plan_id, inspection_item_id),
    INDEX idx_quality_plan_item_reference (inspection_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质量检验标准方案明细表';
