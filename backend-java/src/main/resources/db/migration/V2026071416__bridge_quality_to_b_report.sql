-- C 组质量检验与 B 组不良报表联调字段

ALTER TABLE quality_inspection_record
    ADD COLUMN production_task_id BIGINT NULL COMMENT 'B组生产任务id' AFTER work_order_id,
    ADD COLUMN process_id BIGINT NULL COMMENT '关联工序id' AFTER production_line_id,
    ADD COLUMN defect_group_no VARCHAR(64) NULL COMMENT '跨报工/质检不良归并号' AFTER release_status,
    ADD COLUMN defect_quantity INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '本次质检确认的不良数量' AFTER defect_group_no,
    ADD INDEX idx_quality_record_task_status (production_task_id, record_status, is_deleted),
    ADD INDEX idx_quality_record_line_time (production_line_id, inspected_at, is_deleted),
    ADD INDEX idx_quality_record_defect_group (defect_group_no, is_deleted);
