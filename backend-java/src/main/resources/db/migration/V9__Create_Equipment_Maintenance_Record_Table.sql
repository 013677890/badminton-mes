-- =============================================
-- 设备保养记录表
-- 作者: 角色C
-- 日期: 2026/07/11
-- =============================================

CREATE TABLE IF NOT EXISTS equip_maintenance_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    record_no VARCHAR(32) NOT NULL COMMENT '保养任务编号',
    plan_id BIGINT NOT NULL COMMENT '保养计划id',
    equipment_id BIGINT NOT NULL COMMENT '设备台账id',
    scheduled_time DATETIME NOT NULL COMMENT '计划执行时间',
    start_time DATETIME DEFAULT NULL COMMENT '实际开始时间',
    finish_time DATETIME DEFAULT NULL COMMENT '实际完成时间',
    executor_user_id BIGINT DEFAULT NULL COMMENT '执行人用户id',
    maintenance_content VARCHAR(500) NOT NULL COMMENT '实际保养内容',
    maintenance_result VARCHAR(32) DEFAULT NULL COMMENT '保养结果：NORMAL正常 ABNORMAL异常',
    record_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING待执行 IN_PROGRESS保养中 COMPLETED已完成 CANCELLED已取消',
    abnormal_description VARCHAR(500) DEFAULT NULL COMMENT '异常说明',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注说明',
    create_by BIGINT NOT NULL COMMENT '创建人用户id',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '逻辑删除标记',
    INDEX idx_plan_id (plan_id, is_deleted) COMMENT '保养计划索引',
    INDEX idx_equipment_id (equipment_id, is_deleted) COMMENT '设备索引',
    INDEX idx_record_status (record_status, is_deleted) COMMENT '任务状态索引',
    INDEX idx_scheduled_time (scheduled_time, is_deleted) COMMENT '计划执行时间索引',
    UNIQUE KEY uk_maintenance_record_no (record_no) COMMENT '保养任务编号唯一约束'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备保养记录表';
