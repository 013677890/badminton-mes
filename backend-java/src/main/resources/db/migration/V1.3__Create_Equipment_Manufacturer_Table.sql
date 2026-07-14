-- =============================================
-- 设备制造商表
-- 作者: 角色C
-- 日期: 2026/07/09
-- =============================================

CREATE TABLE IF NOT EXISTS equip_manufacturer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    manufacturer_code VARCHAR(32) NOT NULL COMMENT '制造商编码',
    manufacturer_name VARCHAR(128) NOT NULL COMMENT '制造商名称',
    contact_person VARCHAR(64) DEFAULT NULL COMMENT '联系人',
    contact_phone VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
    contact_email VARCHAR(64) DEFAULT NULL COMMENT '联系邮箱',
    address VARCHAR(255) DEFAULT NULL COMMENT '地址',
    website VARCHAR(128) DEFAULT NULL COMMENT '官网',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注说明',
    status TINYINT DEFAULT 1 COMMENT '状态：1启用 0停用',
    create_by BIGINT NOT NULL COMMENT '创建人用户id',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '逻辑删除标记',
    INDEX idx_status (status, is_deleted) COMMENT '状态筛选索引',
    UNIQUE KEY uk_manufacturer_code (manufacturer_code) COMMENT '制造商编码唯一约束'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备制造商表';
