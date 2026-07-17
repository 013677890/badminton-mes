-- =============================================
-- 设备故障原理表
-- 作者: 角色C
-- 日期: 2026/07/10
-- =============================================

CREATE TABLE IF NOT EXISTS equip_fault_principle (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    fault_code VARCHAR(32) NOT NULL COMMENT '故障编码',
    fault_name VARCHAR(128) NOT NULL COMMENT '故障名称',
    category_id BIGINT DEFAULT NULL COMMENT '适用设备类别id，为空表示通用故障',
    fault_level VARCHAR(32) NOT NULL DEFAULT 'LOW' COMMENT '故障等级：LOW低 MEDIUM中 HIGH高 CRITICAL严重',
    fault_description VARCHAR(500) DEFAULT NULL COMMENT '故障描述',
    suggested_solution VARCHAR(500) DEFAULT NULL COMMENT '建议处理方案',
    sort_order INT DEFAULT 0 COMMENT '排序号，数字越小越靠前',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注说明',
    status TINYINT DEFAULT 1 COMMENT '状态：1启用 0停用',
    create_by BIGINT NOT NULL COMMENT '创建人用户id',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '逻辑删除标记',
    INDEX idx_category_id (category_id, is_deleted) COMMENT '设备类别索引',
    INDEX idx_fault_level (fault_level, is_deleted) COMMENT '故障等级筛选索引',
    INDEX idx_status (status, is_deleted) COMMENT '状态筛选索引',
    UNIQUE KEY uk_fault_code (fault_code) COMMENT '故障编码唯一约束'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备故障原理表';
