-- =============================================
-- 设备台账表
-- 作者: 角色C
-- 日期: 2026/07/09
-- =============================================

CREATE TABLE IF NOT EXISTS equip_ledger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    equipment_code VARCHAR(32) NOT NULL COMMENT '设备编码',
    equipment_name VARCHAR(128) NOT NULL COMMENT '设备名称',
    category_id BIGINT NOT NULL COMMENT '设备类别id',
    manufacturer_id BIGINT DEFAULT NULL COMMENT '设备制造商id',
    equipment_model VARCHAR(64) DEFAULT NULL COMMENT '规格型号',
    serial_number VARCHAR(64) DEFAULT NULL COMMENT '出厂编号',
    workshop_id BIGINT DEFAULT NULL COMMENT '所属车间id',
    production_line_id BIGINT DEFAULT NULL COMMENT '所属产线id',
    installation_location VARCHAR(128) DEFAULT NULL COMMENT '安装位置',
    purchase_date DATE DEFAULT NULL COMMENT '采购日期',
    commissioning_date DATE DEFAULT NULL COMMENT '启用日期',
    equipment_status VARCHAR(32) NOT NULL DEFAULT 'IDLE' COMMENT '设备状态：IDLE空闲 RUNNING运行中 STOPPED停机 REPAIRING维修中 MAINTAINING保养中 SCRAPPED已报废',
    responsible_person VARCHAR(64) DEFAULT NULL COMMENT '负责人',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注说明',
    status TINYINT DEFAULT 1 COMMENT '状态：1启用 0停用',
    create_by BIGINT NOT NULL COMMENT '创建人用户id',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '逻辑删除标记',
    INDEX idx_category_id (category_id, is_deleted) COMMENT '设备类别索引',
    INDEX idx_manufacturer_id (manufacturer_id, is_deleted) COMMENT '设备制造商索引',
    INDEX idx_equipment_status (equipment_status, is_deleted) COMMENT '设备状态筛选索引',
    INDEX idx_status (status, is_deleted) COMMENT '启停状态筛选索引',
    UNIQUE KEY uk_equipment_code (equipment_code) COMMENT '设备编码唯一约束'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备台账表';
