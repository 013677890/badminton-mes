CREATE TABLE scene_repair_work_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    repair_no VARCHAR(64) NOT NULL,
    source_report_id BIGINT NOT NULL,
    task_id BIGINT NULL,
    batch_no VARCHAR(128) NOT NULL,
    defect_quantity INT NOT NULL,
    repair_quantity INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    assignee_id BIGINT NULL,
    recheck_result VARCHAR(32) NULL,
    recheck_quantity INT NULL,
    created_by BIGINT NOT NULL,
    created_time DATETIME NOT NULL,
    updated_time DATETIME NOT NULL,
    is_deleted BIT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_scene_repair_no (repair_no),
    UNIQUE KEY uk_scene_repair_source_report (source_report_id),
    KEY idx_scene_repair_batch_status (batch_no, status),
    KEY idx_scene_repair_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE scene_repair_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    repair_work_order_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    description VARCHAR(500) NOT NULL,
    operator_id BIGINT NOT NULL,
    created_time DATETIME NOT NULL,
    KEY idx_scene_repair_record_order (repair_work_order_id, created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE scene_repair_recheck_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    repair_work_order_id BIGINT NOT NULL,
    result VARCHAR(32) NOT NULL,
    quantity INT NOT NULL,
    inspector_id BIGINT NOT NULL,
    created_time DATETIME NOT NULL,
    KEY idx_scene_repair_recheck_order (repair_work_order_id, created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
