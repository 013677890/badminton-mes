-- =============================================
-- 设备数据接入首期表
-- 作者: 角色C
-- 日期: 2026/07/12
-- =============================================

CREATE TABLE IF NOT EXISTS device_access_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    config_code VARCHAR(32) NOT NULL COMMENT '接入配置编码',
    config_name VARCHAR(128) NOT NULL COMMENT '接入配置名称',
    equipment_id BIGINT NOT NULL COMMENT '设备台账id',
    collection_point_code VARCHAR(64) NOT NULL COMMENT '采集点编码',
    process_id BIGINT DEFAULT NULL COMMENT '关联工序id',
    production_line_id BIGINT DEFAULT NULL COMMENT '关联产线id',
    data_source VARCHAR(32) NOT NULL DEFAULT 'HTTP_API' COMMENT '数据来源：HTTP_API',
    count_mode VARCHAR(32) NOT NULL DEFAULT 'CUMULATIVE' COMMENT '计数模式：CUMULATIVE累计值 INCREMENTAL增量值',
    spike_threshold BIGINT DEFAULT NULL COMMENT '单次计数异常跳变阈值',
    report_mode VARCHAR(32) NOT NULL DEFAULT 'PENDING_CONFIRMATION' COMMENT '报工模式：AUTO PENDING_CONFIRMATION NONE',
    commissioning_status VARCHAR(32) NOT NULL DEFAULT 'NOT_TESTED' COMMENT '联调状态：NOT_TESTED PASSED FAILED',
    enabled_status TINYINT NOT NULL DEFAULT 0 COMMENT '正式采集状态：1启用 0停用',
    last_communication_time DATETIME DEFAULT NULL COMMENT '最后通信时间',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注说明',
    create_by BIGINT NOT NULL COMMENT '创建人用户id',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '逻辑删除标记',
    UNIQUE KEY uk_device_access_config_code (config_code) COMMENT '接入配置编码唯一约束',
    INDEX idx_device_access_equipment (equipment_id, is_deleted) COMMENT '设备台账索引',
    INDEX idx_device_access_point (equipment_id, collection_point_code, is_deleted) COMMENT '设备采集点索引',
    INDEX idx_device_access_status (enabled_status, commissioning_status, is_deleted) COMMENT '接入状态索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备接入配置表';

CREATE TABLE IF NOT EXISTS device_commissioning_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    access_config_id BIGINT NOT NULL COMMENT '设备接入配置id',
    test_time DATETIME NOT NULL COMMENT '联调测试时间',
    tester_user_id BIGINT NOT NULL COMMENT '测试人用户id',
    communication_result VARCHAR(32) NOT NULL COMMENT '通信结果：SUCCESS FAILED',
    data_format_result VARCHAR(32) NOT NULL COMMENT '数据格式结果：SUCCESS FAILED',
    test_result VARCHAR(32) NOT NULL COMMENT '联调结果：PASSED FAILED',
    issue_description VARCHAR(500) DEFAULT NULL COMMENT '问题说明',
    sample_payload TEXT DEFAULT NULL COMMENT '联调样例报文',
    create_by BIGINT NOT NULL COMMENT '创建人用户id',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_device_commissioning_config (access_config_id, test_time) COMMENT '接入配置联调记录索引',
    INDEX idx_device_commissioning_result (test_result, test_time) COMMENT '联调结果索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备联调记录表';

CREATE TABLE IF NOT EXISTS device_count_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    access_config_id BIGINT NOT NULL COMMENT '设备接入配置id',
    equipment_id BIGINT NOT NULL COMMENT '设备台账id',
    equipment_code_snapshot VARCHAR(32) NOT NULL COMMENT '上报时设备编码快照',
    collection_point_code_snapshot VARCHAR(64) NOT NULL COMMENT '上报时采集点编码快照',
    collected_at DATETIME NOT NULL COMMENT '设备采集时间',
    serial_number VARCHAR(64) NOT NULL COMMENT '设备端上报流水号',
    raw_count BIGINT NOT NULL COMMENT '设备原始计数值',
    increment_count BIGINT NOT NULL DEFAULT 0 COMMENT '本次有效增量',
    runtime_status VARCHAR(32) DEFAULT NULL COMMENT '设备上报运行状态',
    fault_status VARCHAR(64) DEFAULT NULL COMMENT '设备上报故障状态',
    production_task_id BIGINT DEFAULT NULL COMMENT '匹配生产任务id',
    process_id BIGINT DEFAULT NULL COMMENT '匹配工序id',
    match_status VARCHAR(32) NOT NULL COMMENT '匹配状态：PENDING MATCHED EXCEPTION',
    report_status VARCHAR(32) NOT NULL DEFAULT 'NOT_CREATED' COMMENT '报工状态：NOT_CREATED AUTO_REPORTED PENDING_CONFIRMATION',
    deduplication_key CHAR(64) NOT NULL COMMENT '幂等键SHA-256',
    raw_payload TEXT DEFAULT NULL COMMENT '原始上报报文',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_device_count_deduplication (deduplication_key) COMMENT '设备计数幂等约束',
    INDEX idx_device_count_config_time (access_config_id, collected_at) COMMENT '配置采集时间索引',
    INDEX idx_device_count_equipment_time (equipment_id, collected_at) COMMENT '设备采集时间索引',
    INDEX idx_device_count_match (match_status, collected_at) COMMENT '计数匹配状态索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备计数记录表';

CREATE TABLE IF NOT EXISTS device_count_exception (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    count_record_id BIGINT NOT NULL COMMENT '设备计数记录id',
    access_config_id BIGINT NOT NULL COMMENT '设备接入配置id',
    equipment_id BIGINT NOT NULL COMMENT '设备台账id',
    exception_type VARCHAR(64) NOT NULL COMMENT '异常类型',
    exception_reason VARCHAR(500) NOT NULL COMMENT '异常原因',
    processing_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态：PENDING RESOLVED IGNORED',
    processed_by BIGINT DEFAULT NULL COMMENT '处理人用户id',
    processed_at DATETIME DEFAULT NULL COMMENT '处理时间',
    processing_result VARCHAR(500) DEFAULT NULL COMMENT '处理结果说明',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_device_count_exception_record (count_record_id) COMMENT '计数记录异常唯一约束',
    INDEX idx_device_exception_status (processing_status, create_time) COMMENT '异常处理状态索引',
    INDEX idx_device_exception_equipment (equipment_id, create_time) COMMENT '设备异常索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备计数异常表';
