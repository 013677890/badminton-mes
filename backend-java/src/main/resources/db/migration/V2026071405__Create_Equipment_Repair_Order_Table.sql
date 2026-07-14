-- =============================================
-- 设备报修任务表
-- 作者: 角色C
-- 日期: 2026/07/10
-- =============================================

CREATE TABLE IF NOT EXISTS equip_repair_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    repair_no VARCHAR(32) NOT NULL COMMENT '报修单号',
    equipment_id BIGINT NOT NULL COMMENT '设备台账id',
    fault_principle_id BIGINT DEFAULT NULL COMMENT '故障原理id',
    fault_description VARCHAR(500) NOT NULL COMMENT '故障描述',
    report_time DATETIME NOT NULL COMMENT '报修时间',
    report_user_id BIGINT NOT NULL COMMENT '报修人用户id',
    repair_user_id BIGINT DEFAULT NULL COMMENT '维修人用户id',
    repair_start_time DATETIME DEFAULT NULL COMMENT '维修开始时间',
    repair_end_time DATETIME DEFAULT NULL COMMENT '维修结束时间',
    repair_result VARCHAR(500) DEFAULT NULL COMMENT '维修结果',
    repair_status VARCHAR(32) NOT NULL DEFAULT 'REPORTED' COMMENT '报修状态：REPORTED已报修 ASSIGNED已派工 REPAIRING维修中 FINISHED已完成 CANCELLED已取消',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注说明',
    create_by BIGINT NOT NULL COMMENT '创建人用户id',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '逻辑删除标记',
    INDEX idx_repair_no (repair_no, is_deleted) COMMENT '报修单号索引',
    INDEX idx_equipment_id (equipment_id, is_deleted) COMMENT '设备索引',
    INDEX idx_fault_principle_id (fault_principle_id, is_deleted) COMMENT '故障原理索引',
    INDEX idx_repair_status (repair_status, is_deleted) COMMENT '报修状态筛选索引',
    INDEX idx_report_time (report_time, is_deleted) COMMENT '报修时间筛选索引',
    UNIQUE KEY uk_repair_no (repair_no) COMMENT '报修单号唯一约束'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备报修任务表';
