-- =============================================
-- 设备保养计划表
-- 作者: 角色C
-- 日期: 2026/07/11
-- =============================================

CREATE TABLE IF NOT EXISTS equip_maintenance_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    plan_code VARCHAR(32) NOT NULL COMMENT '保养计划编码',
    plan_name VARCHAR(128) NOT NULL COMMENT '保养计划名称',
    equipment_id BIGINT NOT NULL COMMENT '设备台账id',
    maintenance_type VARCHAR(32) NOT NULL DEFAULT 'ROUTINE' COMMENT '保养类型：ROUTINE例行 PREVENTIVE预防 SPECIAL专项',
    cycle_days INT NOT NULL COMMENT '保养周期天数',
    maintenance_content VARCHAR(500) NOT NULL COMMENT '保养内容',
    responsible_user_id BIGINT DEFAULT NULL COMMENT '负责人用户id',
    last_maintenance_time DATETIME DEFAULT NULL COMMENT '上次保养时间',
    next_maintenance_time DATETIME NOT NULL COMMENT '下次保养时间',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注说明',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    create_by BIGINT NOT NULL COMMENT '创建人用户id',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '逻辑删除标记',
    INDEX idx_equipment_id (equipment_id, is_deleted) COMMENT '设备索引',
    INDEX idx_maintenance_type (maintenance_type, is_deleted) COMMENT '保养类型索引',
    INDEX idx_next_maintenance_time (next_maintenance_time, status, is_deleted) COMMENT '下次保养时间索引',
    INDEX idx_status (status, is_deleted) COMMENT '状态索引',
    UNIQUE KEY uk_maintenance_plan_code (plan_code) COMMENT '保养计划编码唯一约束'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备保养计划表';
