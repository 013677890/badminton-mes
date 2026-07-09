-- =============================================
-- 设备管理模块 - 数据库表结构
-- 作者: 角色C
-- 日期: 2026/07/09
-- =============================================

-- 设备类别表
CREATE TABLE IF NOT EXISTS equip_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    category_code VARCHAR(32) NOT NULL COMMENT '类别编码',
    category_name VARCHAR(64) NOT NULL COMMENT '类别名称',
    parent_id BIGINT DEFAULT NULL COMMENT '父级类别id，顶级为null',
    sort_order INT DEFAULT 0 COMMENT '排序号，数字越小越靠前',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注说明',
    status TINYINT DEFAULT 1 COMMENT '状态：1启用 0停用',
    create_by BIGINT NOT NULL COMMENT '创建人用户id',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '逻辑删除标记',
    INDEX idx_parent_id (parent_id, is_deleted) COMMENT '父级类别索引',
    INDEX idx_status (status, is_deleted) COMMENT '状态筛选索引',
    UNIQUE KEY uk_category_code (category_code) COMMENT '类别编码唯一约束'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备类别表';
