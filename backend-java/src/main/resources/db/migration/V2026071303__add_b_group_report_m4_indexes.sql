ALTER TABLE prod_report
    ADD KEY idx_report_time_task_process (report_time, task_id, process_id);

ALTER TABLE prod_task
    ADD KEY idx_workshop_line_status_product (workshop_id, line_id, task_status, product_id);
