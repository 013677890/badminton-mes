ALTER TABLE prod_task
    ADD COLUMN dispatch_order_id bigint unsigned NULL COMMENT 'A组派工单主键' AFTER source_type,
    ADD UNIQUE KEY uk_task_dispatch_order (dispatch_order_id);
